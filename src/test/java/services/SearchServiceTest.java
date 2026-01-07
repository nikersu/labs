package services;

import entities.FunctionEntity;
import entities.UserEntity;
import dto.PointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import repositories.FunctionRepository;
import repositories.UserRepository;
import util.FunctionDataSerializer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FunctionRepository functionRepository;

    @InjectMocks
    private SearchService searchService;

    private UserEntity user;
    private FunctionEntity f1;
    private FunctionEntity f2;

    @BeforeEach
    void setUp() {
        user = new UserEntity("user1", "hash");
        setId(user, "id", 1L);

        f1 = new FunctionEntity("alpha", "x^2", user);
        f2 = new FunctionEntity("beta", "sin(x)", user);
        setId(f1, "id", 10L);
        setId(f2, "id", 20L);
        
        // Устанавливаем JSON данные для функций (не поточечно!)
        double[] xValues1 = {1.0, 2.0};
        double[] yValues1 = {1.0, 4.0};
        f1.setXValuesJson(FunctionDataSerializer.serializeArray(xValues1));
        f1.setYValuesJson(FunctionDataSerializer.serializeArray(yValues1));
        f1.setCount(2);
        
        double[] xValues2 = {0.0, 1.0};
        double[] yValues2 = {0.0, 0.841};
        f2.setXValuesJson(FunctionDataSerializer.serializeArray(xValues2));
        f2.setYValuesJson(FunctionDataSerializer.serializeArray(yValues2));
        f2.setCount(2);
    }

    // ---------- Одиночный поиск ----------

    @Test
    void findSingleEntities() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(functionRepository.findById(10L)).thenReturn(Optional.of(f1));

        assertThat(searchService.findUserByUsername("user1")).contains(user);
        assertThat(searchService.findFunctionById(10L)).contains(f1);
        
        Optional<PointDto> point = searchService.findPoint(10L, 1.0);
        assertThat(point).isPresent();
        assertThat(point.get().getXValue()).isEqualTo(1.0);
        assertThat(point.get().getYValue()).isEqualTo(1.0);

        // guard-ветка для null параметров
        assertThat(searchService.findPoint(null, 1.0)).isEmpty();
        assertThat(searchService.findPoint(10L, null)).isEmpty();
    }

    // ---------- Множественный поиск с сортировкой ----------

    @Test
    void multipleSearchWithSorting() {
        when(userRepository.findByUsernameIn(eq(List.of("user1")), any(Sort.class)))
                .thenReturn(List.of(user));
        when(functionRepository.findByUserIdAndNameContainingIgnoreCase(eq(1L), eq("a"), any(Sort.class)))
                .thenReturn(List.of(f1));
        when(functionRepository.findById(10L)).thenReturn(Optional.of(f1));

        var users = searchService.findUsers(List.of("user1"), Sort.by("username"));
        var functions = searchService.searchFunctions(1L, "a", Sort.by("name"));
        var points = searchService.searchPoints(10L, 0.0, 5.0, Sort.by(Sort.Order.desc("xValue")));

        assertThat(users).containsExactly(user);
        assertThat(functions).containsExactly(f1);
        assertThat(points).hasSize(2);
        assertThat(points.get(0).getXValue()).isEqualTo(2.0); // Сортировка по убыванию
        assertThat(points.get(1).getXValue()).isEqualTo(1.0);

        // Пустой / null ввод -> пустой результат без обращений к репозиторию пользователей
        assertThat(searchService.findUsers(List.of(), Sort.unsorted())).isEmpty();
        assertThat(searchService.findUsers(null, Sort.unsorted())).isEmpty();
        verify(userRepository, times(1)).findByUsernameIn(anyList(), any(Sort.class));
    }

    // ---------- Поиск по иерархии: в ширину и в глубину ----------

    @Test
    void breadthFirstAndDepthFirstHierarchy() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(functionRepository.findByUserId(1L)).thenReturn(List.of(f1, f2));

        var bfs = searchService.breadthFirstHierarchy(1L);
        var dfs = searchService.depthFirstHierarchy(1L);

        // BFS: пользователь, потом функции, потом точки (PointDto из JSON)
        assertThat(bfs).contains(user);
        assertThat(bfs.stream().filter(FunctionEntity.class::isInstance).count()).isEqualTo(2);
        assertThat(bfs.stream().filter(PointDto.class::isInstance).count()).isEqualTo(4); // 2 точки из f1 + 2 точки из f2

        // DFS: пользователь, затем каждая функция со своими точками
        assertThat(dfs.get(0)).isInstanceOf(UserEntity.class);
        assertThat(dfs.stream().anyMatch(PointDto.class::isInstance)).isTrue();

        // Если пользователь не найден – пустой результат
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThat(searchService.breadthFirstHierarchy(2L)).isEmpty();
        assertThat(searchService.depthFirstHierarchy(2L)).isEmpty();
    }

    // ---------- Фильтрация коллекции функций с сортировкой ----------

    @Test
    void filterFunctionsWithPredicateAndSorting() {
        var src = List.of(f2, f1); // заведомо «перемешанный» список

        // Фильтруем функции с именем, содержащим 'a', сортируем по имени
        var sorted = searchService.filterFunctions(
                src,
                f -> f.getName().contains("a"),
                Sort.by(Sort.Order.asc("name"))
        );

        assertThat(sorted).containsExactly(f1, f2); // alpha, beta

        // Пустой / null источник -> пустой результат
        assertThat(searchService.filterFunctions(List.of(), f -> true, Sort.unsorted())).isEmpty();
        assertThat(searchService.filterFunctions(null, f -> true, Sort.unsorted())).isEmpty();
    }

    // ---------- Вспомогательный метод для установки ID через reflection ----------

    private static void setId(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set id via reflection", e);
        }
    }
}



