package repositories;

import config.Application;
import entities.FunctionEntity;
import entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
public class FunctionRepositoryTest {

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private CompositeFunctionRepository compositeFunctionRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        pointRepository.deleteAll(); // Удаляем точки перед функциями (избегаем проблем с внешними ключами)
        compositeFunctionRepository.deleteAll(); // Удаляем компонентные функции перед пользователями
        functionRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестового пользователя
        testUser = userRepository.save(new UserEntity("function_test_user", "password_hash"));
    }

    @Test
    void testSaveAndFindFunction() {
        // Given
        FunctionEntity function = new FunctionEntity("test_function", "x^2 + 2x + 1", testUser);

        // When
        FunctionEntity savedFunction = functionRepository.save(function);

        // Then
        assertThat(savedFunction).isNotNull();
        assertThat(savedFunction.getId()).isNotNull();
        assertThat(savedFunction.getName()).isEqualTo("test_function");
        assertThat(savedFunction.getExpression()).isEqualTo("x^2 + 2x + 1");
        assertThat(savedFunction.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void testFindByUserId() {
        // Given
        FunctionEntity f1 = functionRepository.save(new FunctionEntity("func1", "sin(x)", testUser));
        FunctionEntity f2 = functionRepository.save(new FunctionEntity("func2", "cos(x)", testUser));

        // Create another user with function
        UserEntity anotherUser = userRepository.save(new UserEntity("another_user", "hash"));
        functionRepository.save(new FunctionEntity("other_func", "x^3", anotherUser));

        // When
        List<FunctionEntity> userFunctions = functionRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userFunctions).hasSize(2);

        // Check function names
        List<String> functionNames = userFunctions.stream()
                .map(FunctionEntity::getName)
                .toList();
        assertThat(functionNames).containsExactlyInAnyOrder("func1", "func2");

        // Functions should belong to testUser
        userFunctions.forEach(func ->
                assertThat(func.getUser().getId()).isEqualTo(testUser.getId())
        );
    }

    @Test
    void testFindByUserIdEmptyResult() {
        // When - поиск функций для пользователя без функций
        UserEntity userWithoutFunctions = userRepository.save(new UserEntity("empty_user", "hash"));
        List<FunctionEntity> functions = functionRepository.findByUserId(userWithoutFunctions.getId());

        // Then
        assertThat(functions).isEmpty();
    }

    @Test
    void testFindByUserIdAndNameContainingIgnoreCase() {
        // Given
        functionRepository.save(new FunctionEntity("SIN Function", "sin(x)", testUser));
        functionRepository.save(new FunctionEntity("Cosine function", "cos(x)", testUser));
        functionRepository.save(new FunctionEntity("Tangent Function", "tan(x)", testUser));
        functionRepository.save(new FunctionEntity("Exponential", "e^x", testUser));

        // When - поиск функций содержащих "func" (регистронезависимо)
        List<FunctionEntity> functions = functionRepository.findByUserIdAndNameContainingIgnoreCase(
                testUser.getId(),
                "func",
                Sort.by(Sort.Direction.ASC, "name")
        );

        // Then
        assertThat(functions).hasSize(3); // SIN Function, Cosine function, Tangent Function

        // Проверяем сортировку по имени
        assertThat(functions.get(0).getName()).isEqualTo("Cosine function");
        assertThat(functions.get(1).getName()).isEqualTo("SIN Function");
        assertThat(functions.get(2).getName()).isEqualTo("Tangent Function");

        // Проверяем, что Exponential не попал в результаты
        assertThat(functions.stream()
                .anyMatch(f -> f.getName().equals("Exponential")))
                .isFalse();
    }

    @Test
    void testFindByUserIdAndNameContainingIgnoreCaseCaseSensitive() {
        // Given
        functionRepository.save(new FunctionEntity("MyFunction", "x^2", testUser));
        functionRepository.save(new FunctionEntity("myfunction", "x^3", testUser));
        functionRepository.save(new FunctionEntity("MYFUNCTION", "x^4", testUser));

        // When - поиск с разным регистром
        List<FunctionEntity> lowerCase = functionRepository.findByUserIdAndNameContainingIgnoreCase(
                testUser.getId(), "myfunction", Sort.unsorted()
        );

        List<FunctionEntity> upperCase = functionRepository.findByUserIdAndNameContainingIgnoreCase(
                testUser.getId(), "MYFUNCTION", Sort.unsorted()
        );

        List<FunctionEntity> mixedCase = functionRepository.findByUserIdAndNameContainingIgnoreCase(
                testUser.getId(), "MyFuNcTiOn", Sort.unsorted()
        );

        // Then - все должны найти 3 функции
        assertThat(lowerCase).hasSize(3);
        assertThat(upperCase).hasSize(3);
        assertThat(mixedCase).hasSize(3);
    }

    @Test
    void testFindByIdInWithSort() {
        // Given
        FunctionEntity f1 = functionRepository.save(new FunctionEntity("func1", "x^2", testUser));
        FunctionEntity f2 = functionRepository.save(new FunctionEntity("func2", "sin(x)", testUser));
        FunctionEntity f3 = functionRepository.save(new FunctionEntity("func3", "cos(x)", testUser));
        FunctionEntity f4 = functionRepository.save(new FunctionEntity("func4", "tan(x)", testUser));

        // When - ищем только f1, f3, f4 с сортировкой по имени в обратном порядке
        List<Long> idsToFind = Arrays.asList(f1.getId(), f3.getId(), f4.getId());
        List<FunctionEntity> functions = functionRepository.findByIdIn(
                idsToFind,
                Sort.by(Sort.Direction.DESC, "name")
        );

        // Then
        assertThat(functions).hasSize(3);

        // Проверяем сортировку по убыванию имени
        assertThat(functions.get(0).getName()).isEqualTo("func4"); // tan(x)
        assertThat(functions.get(1).getName()).isEqualTo("func3"); // cos(x)
        assertThat(functions.get(2).getName()).isEqualTo("func1"); // x^2

        // Проверяем, что func2 не попал в результаты
        assertThat(functions.stream()
                .anyMatch(f -> f.getName().equals("func2")))
                .isFalse();
    }

    @Test
    void testFindByIdInEmptyCollection() {
        // When
        List<FunctionEntity> functions = functionRepository.findByIdIn(
                Arrays.asList(),
                Sort.by("name")
        );

        // Then
        assertThat(functions).isEmpty();
    }

    @Test
    void testUpdateFunction() {
        // Given
        FunctionEntity function = functionRepository.save(
                new FunctionEntity("old_name", "old_expression", testUser)
        );

        // When - обновляем
        function.setName("new_name");
        function.setExpression("new_expression");
        FunctionEntity updated = functionRepository.save(function);

        // Then
        assertThat(updated.getId()).isEqualTo(function.getId());
        assertThat(updated.getName()).isEqualTo("new_name");
        assertThat(updated.getExpression()).isEqualTo("new_expression");

        // Проверяем через поиск
        Optional<FunctionEntity> found = functionRepository.findById(function.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("new_name");
    }

    @Test
    void testDeleteFunction() {
        // Given
        FunctionEntity function = functionRepository.save(
                new FunctionEntity("to_delete", "x^2", testUser)
        );
        Long functionId = function.getId();

        // When
        functionRepository.delete(function);

        // Then
        Optional<FunctionEntity> deleted = functionRepository.findById(functionId);
        assertThat(deleted).isEmpty();

        // Проверяем, что у пользователя нет функций
        List<FunctionEntity> userFunctions = functionRepository.findByUserId(testUser.getId());
        assertThat(userFunctions).isEmpty();
    }

    @Test
    void testFindAllFunctions() {
        // Given
        functionRepository.save(new FunctionEntity("f1", "x^2", testUser));
        functionRepository.save(new FunctionEntity("f2", "sin(x)", testUser));

        UserEntity anotherUser = userRepository.save(new UserEntity("user2", "hash"));
        functionRepository.save(new FunctionEntity("f3", "cos(x)", anotherUser));

        // When
        List<FunctionEntity> allFunctions = functionRepository.findAll();

        // Then
        assertThat(allFunctions).hasSize(3);

        // Проверяем, что все функции создались
        List<String> functionNames = allFunctions.stream()
                .map(FunctionEntity::getName)
                .toList();
        assertThat(functionNames).containsExactlyInAnyOrder("f1", "f2", "f3");
    }

    @Test
    void testFunctionCount() {
        // Given
        functionRepository.save(new FunctionEntity("f1", "x^2", testUser));
        functionRepository.save(new FunctionEntity("f2", "sin(x)", testUser));

        // When
        long count = functionRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testSaveMultipleFunctions() {
        // Given
        List<FunctionEntity> functions = Arrays.asList(
                new FunctionEntity("multi1", "x^2", testUser),
                new FunctionEntity("multi2", "sin(x)", testUser),
                new FunctionEntity("multi3", "cos(x)", testUser)
        );

        // When
        List<FunctionEntity> savedFunctions = functionRepository.saveAll(functions);

        // Then
        assertThat(savedFunctions).hasSize(3);

        // Проверяем, что у всех есть ID
        savedFunctions.forEach(func ->
                assertThat(func.getId()).isNotNull()
        );

        // Проверяем общее количество
        assertThat(functionRepository.count()).isEqualTo(3);
    }
}
