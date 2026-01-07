package repositories;

import config.Application;
import entities.FunctionEntity;
import entities.PointEntity;
import entities.PointId;
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
public class PointRepositoryTest {

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompositeFunctionRepository compositeFunctionRepository;

    private UserEntity testUser;
    private FunctionEntity testFunction;

    @BeforeEach
    void setUp() {
        pointRepository.deleteAll();
        compositeFunctionRepository.deleteAll();
        functionRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестового пользователя и функцию
        testUser = userRepository.save(new UserEntity("point_test_user", "password_hash"));
        testFunction = functionRepository.save(new FunctionEntity("test_function", "x^2", testUser));
    }

    @Test
    void testSaveAndFindPoint() {
        // Given
        PointEntity point = new PointEntity(testFunction, 2.0, 4.0);

        // When
        PointEntity savedPoint = pointRepository.save(point);

        // Then
        assertThat(savedPoint).isNotNull();
        assertThat(savedPoint.getId()).isNotNull();
        assertThat(savedPoint.getId().getFunctionId()).isEqualTo(testFunction.getId());
        assertThat(savedPoint.getId().getXValue()).isEqualTo(2.0);
        assertThat(savedPoint.getY()).isEqualTo(4.0);
        assertThat(savedPoint.getFunction().getId()).isEqualTo(testFunction.getId());
    }

    @Test
    void testFindByFunctionId() {
        // Given
        pointRepository.save(new PointEntity(testFunction, 0.0, 0.0));
        pointRepository.save(new PointEntity(testFunction, 1.0, 1.0));
        pointRepository.save(new PointEntity(testFunction, 2.0, 4.0));

        // Create another function with points
        FunctionEntity anotherFunction = functionRepository.save(
                new FunctionEntity("another_func", "sin(x)", testUser)
        );
        pointRepository.save(new PointEntity(anotherFunction, 0.0, 0.0));

        // When
        List<PointEntity> functionPoints = pointRepository.findByIdFunctionId(testFunction.getId());

        // Then
        assertThat(functionPoints).hasSize(3);

        // Проверяем, что все точки принадлежат правильной функции
        functionPoints.forEach(point ->
                assertThat(point.getFunction().getId()).isEqualTo(testFunction.getId())
        );

        // Проверяем координаты
        List<Double> xValues = functionPoints.stream()
                .map(point -> point.getId().getXValue())
                .toList();
        assertThat(xValues).containsExactlyInAnyOrder(0.0, 1.0, 2.0);
    }

    @Test
    void testFindByFunctionIdEmptyResult() {
        // Given - функция без точек

        // When
        List<PointEntity> points = pointRepository.findByIdFunctionId(testFunction.getId());

        // Then
        assertThat(points).isEmpty();
    }


    @Test
    void testFindByIdFunctionIdIn() {
        // Given - создаем несколько функций с точками
        FunctionEntity func1 = functionRepository.save(new FunctionEntity("func1", "x^2", testUser));
        FunctionEntity func2 = functionRepository.save(new FunctionEntity("func2", "sin(x)", testUser));
        FunctionEntity func3 = functionRepository.save(new FunctionEntity("func3", "cos(x)", testUser));

        // Добавляем точки
        pointRepository.save(new PointEntity(func1, 0.0, 0.0));
        pointRepository.save(new PointEntity(func1, 1.0, 1.0));
        pointRepository.save(new PointEntity(func2, 0.0, 0.0));
        pointRepository.save(new PointEntity(func2, 1.0, 0.841));
        pointRepository.save(new PointEntity(func3, 0.0, 1.0));

        // When - ищем точки для func1 и func3 с сортировкой по functionId и x
        List<Long> functionIds = Arrays.asList(func1.getId(), func3.getId());
        List<PointEntity> points = pointRepository.findByIdFunctionIdIn(
                functionIds,
                Sort.by(Sort.Direction.ASC, "id.functionId", "id.xValue")
        );

        // Then
        assertThat(points).hasSize(3); // 2 точки из func1 + 1 точка из func3

        // Проверяем сортировку: сначала func1 (x=0, x=1), потом func3 (x=0)
        assertThat(points.get(0).getFunction().getId()).isEqualTo(func1.getId());
        assertThat(points.get(0).getId().getXValue()).isEqualTo(0.0);

        assertThat(points.get(1).getFunction().getId()).isEqualTo(func1.getId());
        assertThat(points.get(1).getId().getXValue()).isEqualTo(1.0);

        assertThat(points.get(2).getFunction().getId()).isEqualTo(func3.getId());
        assertThat(points.get(2).getId().getXValue()).isEqualTo(0.0);

        // Проверяем, что точки из func2 не попали в результаты
        assertThat(points.stream()
                .anyMatch(p -> p.getFunction().getId().equals(func2.getId())))
                .isFalse();
    }

    @Test
    void testFindByIdFunctionIdInEmptyCollection() {
        // When
        List<PointEntity> points = pointRepository.findByIdFunctionIdIn(
                Arrays.asList(),
                Sort.by("id.functionId")
        );

        // Then
        assertThat(points).isEmpty();
    }

    @Test
    void testUpdatePoint() {
        // Given
        PointEntity point = pointRepository.save(new PointEntity(testFunction, 1.0, 1.0));
        PointId pointId = point.getId();

        // When - обновляем y значение
        point.setY(2.0);
        PointEntity updated = pointRepository.save(point);

        // Then
        assertThat(updated.getId()).isEqualTo(pointId);
        assertThat(updated.getY()).isEqualTo(2.0);

        // Проверяем через поиск по составному ключу
        Optional<PointEntity> found = pointRepository.findById(pointId);
        assertThat(found).isPresent();
        assertThat(found.get().getY()).isEqualTo(2.0);
    }

    @Test
    void testDeletePoint() {
        // Given
        PointEntity point = pointRepository.save(new PointEntity(testFunction, 2.0, 4.0));
        PointId pointId = point.getId();

        // When
        pointRepository.delete(point);

        // Then
        Optional<PointEntity> deleted = pointRepository.findById(pointId);
        assertThat(deleted).isEmpty();

        // Проверяем, что у функции нет точек
        List<PointEntity> functionPoints = pointRepository.findByIdFunctionId(testFunction.getId());
        assertThat(functionPoints).isEmpty();
    }

    @Test
    void testSaveMultiplePoints() {
        // Given
        List<PointEntity> points = Arrays.asList(
                new PointEntity(testFunction, -2.0, 4.0),
                new PointEntity(testFunction, -1.0, 1.0),
                new PointEntity(testFunction, 0.0, 0.0),
                new PointEntity(testFunction, 1.0, 1.0),
                new PointEntity(testFunction, 2.0, 4.0)
        );

        // When
        List<PointEntity> savedPoints = pointRepository.saveAll(points);

        // Then
        assertThat(savedPoints).hasSize(5);

        // Проверяем, что все точки сохранились с правильными ID
        savedPoints.forEach(point -> {
            assertThat(point.getId()).isNotNull();
            assertThat(point.getId().getFunctionId()).isEqualTo(testFunction.getId());
        });

        // Проверяем общее количество точек
        assertThat(pointRepository.count()).isEqualTo(5);
    }

    @Test
    void testPointWithSameXValueForDifferentFunctions() {
        // Given - создаем вторую функцию
        FunctionEntity func2 = functionRepository.save(new FunctionEntity("func2", "x^3", testUser));

        // When - сохраняем точки с одинаковым x для разных функций
        PointEntity point1 = pointRepository.save(new PointEntity(testFunction, 1.0, 1.0));
        PointEntity point2 = pointRepository.save(new PointEntity(func2, 1.0, 1.0));

        // Then - обе точки должны сохраниться
        assertThat(point1.getId().getFunctionId()).isEqualTo(testFunction.getId());
        assertThat(point1.getId().getXValue()).isEqualTo(1.0);

        assertThat(point2.getId().getFunctionId()).isEqualTo(func2.getId());
        assertThat(point2.getId().getXValue()).isEqualTo(1.0);

        // Проверяем, что это разные точки
        assertThat(point1.getId()).isNotEqualTo(point2.getId());
    }

    @Test
    void testFindPointByCompositeId() {
        // Given
        PointEntity point = pointRepository.save(new PointEntity(testFunction, 3.14, 9.8696));
        PointId expectedId = new PointId(testFunction.getId(), 3.14);

        // When
        Optional<PointEntity> found = pointRepository.findById(expectedId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(point.getId());
        assertThat(found.get().getId().getFunctionId()).isEqualTo(testFunction.getId());
        assertThat(found.get().getId().getXValue()).isEqualTo(3.14);
        assertThat(found.get().getY()).isEqualTo(9.8696);
    }

    @Test
    void testFindAllPoints() {
        // Given
        pointRepository.save(new PointEntity(testFunction, 0.0, 0.0));
        pointRepository.save(new PointEntity(testFunction, 1.0, 1.0));

        // Create another function with points
        FunctionEntity anotherFunction = functionRepository.save(
                new FunctionEntity("another_func", "sin(x)", testUser)
        );
        pointRepository.save(new PointEntity(anotherFunction, 0.0, 0.0));

        // When
        List<PointEntity> allPoints = pointRepository.findAll();

        // Then
        assertThat(allPoints).hasSize(3);

        // Проверяем, что все точки создались
        long testFuncPoints = allPoints.stream()
                .filter(p -> p.getFunction().getId().equals(testFunction.getId()))
                .count();
        assertThat(testFuncPoints).isEqualTo(2);
    }

    @Test
    void testPointCount() {
        // Given
        pointRepository.save(new PointEntity(testFunction, 0.0, 0.0));
        pointRepository.save(new PointEntity(testFunction, 1.0, 1.0));
        pointRepository.save(new PointEntity(testFunction, 2.0, 4.0));

        // When
        long count = pointRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }
}