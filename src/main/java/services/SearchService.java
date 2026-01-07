package services;

import entities.FunctionEntity;
import entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repositories.FunctionRepository;
import repositories.UserRepository;
import util.FunctionDataSerializer;
import dto.PointDto;
import dto.FunctionDto;
import dto.UserDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final UserRepository userRepository;
    private final FunctionRepository functionRepository;

    public SearchService(UserRepository userRepository,
                         FunctionRepository functionRepository) {
        this.userRepository = userRepository;
        this.functionRepository = functionRepository;
    }

    // ---------- Single entity searches ----------

    public Optional<UserEntity> findUserByUsername(String username) {
        log.info("Searching user by username='{}'", username);
        return userRepository.findByUsername(username);
    }

    public Optional<FunctionEntity> findFunctionById(Long id) {
        log.info("Searching function by id={}", id);
        return functionRepository.findById(id);
    }

    public Optional<PointDto> findPoint(Long functionId, Double xValue) {
        log.info("Searching point by functionId={}, x={}", functionId, xValue);
        if (functionId == null || xValue == null) {
            return Optional.empty();
        }
        Optional<FunctionEntity> functionOpt = functionRepository.findById(functionId);
        if (functionOpt.isEmpty()) {
            return Optional.empty();
        }
        FunctionEntity function = functionOpt.get();
        
        // Извлекаем точки из JSON данных функции (не поточечно!)
        if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
            function.getXValuesJson().trim().isEmpty() || function.getYValuesJson().trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
            double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            
            for (int i = 0; i < xValues.length; i++) {
                if (Math.abs(xValues[i] - xValue) < 1e-10) {
                    return Optional.of(new PointDto(functionId, xValues[i], yValues[i]));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to deserialize points for function {}: {}", functionId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    // ---------- Multiple entity searches with sorting ----------

    public List<UserEntity> findUsers(Collection<String> usernames, Sort sort) {
        log.info("Searching users by usernames={} with sort={}", usernames, sort);
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        return userRepository.findByUsernameIn(usernames, sort);
    }

    public List<FunctionEntity> searchFunctions(Long userId, String nameLike, Sort sort) {
        log.info("Searching functions for userId={} nameLike='{}' sort={}", userId, nameLike, sort);
        if (userId == null) {
            return List.of();
        }
        String like = nameLike == null ? "" : nameLike;
        return functionRepository.findByUserIdAndNameContainingIgnoreCase(userId, like, sort);
    }

    public List<PointDto> searchPoints(Long functionId, Double fromX, Double toX, Sort sort) {
        log.info("Searching points for functionId={} fromX={} toX={} sort={}", functionId, fromX, toX, sort);
        if (functionId == null) {
            return List.of();
        }
        Optional<FunctionEntity> functionOpt = functionRepository.findById(functionId);
        if (functionOpt.isEmpty()) {
            return List.of();
        }
        FunctionEntity function = functionOpt.get();
        
        // Извлекаем точки из JSON данных функции (не поточечно!)
        if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
            function.getXValuesJson().trim().isEmpty() || function.getYValuesJson().trim().isEmpty()) {
            return List.of();
        }
        
        try {
            double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
            double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            
            double from = fromX == null ? Double.NEGATIVE_INFINITY : fromX;
            double to = toX == null ? Double.POSITIVE_INFINITY : toX;
            
            List<PointDto> points = new ArrayList<>();
            for (int i = 0; i < xValues.length; i++) {
                if (xValues[i] >= from && xValues[i] <= to) {
                    points.add(new PointDto(functionId, xValues[i], yValues[i]));
                }
            }
            
            // Применяем сортировку
            if (sort != null && sort.isSorted()) {
                points.sort((p1, p2) -> {
                    for (Sort.Order order : sort) {
                        int cmp = switch (order.getProperty()) {
                            case "xValue", "id.xValue" -> Double.compare(p1.getXValue(), p2.getXValue());
                            case "yValue" -> Double.compare(p1.getYValue(), p2.getYValue());
                            default -> 0;
                        };
                        if (cmp != 0) {
                            return order.isAscending() ? cmp : -cmp;
                        }
                    }
                    return 0;
                });
            }
            
            return points;
        } catch (Exception e) {
            log.error("Failed to deserialize points for function {}: {}", functionId, e.getMessage(), e);
            return List.of();
        }
    }

    // ---------- Hierarchical traversals ----------

    public List<Object> breadthFirstHierarchy(Long userId) {
        log.info("Breadth-first traversal for userId={}", userId);
        return traverseHierarchy(userId, false);
    }

    public List<Object> depthFirstHierarchy(Long userId) {
        log.info("Depth-first traversal for userId={}", userId);
        return traverseHierarchy(userId, true);
    }

    private List<Object> traverseHierarchy(Long userId, boolean depthFirst) {
        Optional<UserEntity> userOpt = userId == null ? Optional.empty() : userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found, traversal aborted for userId={}", userId);
            return List.of();
        }
        UserEntity user = userOpt.get();
        List<FunctionEntity> functions = functionRepository.findByUserId(user.getId());

        List<Object> result = new ArrayList<>();
        if (depthFirst) {
            depthFirstCollect(user, functions, result);
        } else {
            breadthFirstCollect(user, functions, result);
        }
        return result;
    }

    private void depthFirstCollect(UserEntity user, List<FunctionEntity> functions, List<Object> sink) {
        sink.add(user); // Возвращаем UserEntity, а не UserDto
        for (FunctionEntity f : functions) {
            sink.add(f); // Возвращаем FunctionEntity, а не FunctionDto
            // Извлекаем точки из JSON данных функции (не поточечно!)
            if (f.getXValuesJson() != null && f.getYValuesJson() != null &&
                !f.getXValuesJson().trim().isEmpty() && !f.getYValuesJson().trim().isEmpty()) {
                try {
                    double[] xValues = FunctionDataSerializer.deserializeArray(f.getXValuesJson());
                    double[] yValues = FunctionDataSerializer.deserializeArray(f.getYValuesJson());
                    for (int i = 0; i < xValues.length; i++) {
                        sink.add(new PointDto(f.getId(), xValues[i], yValues[i]));
                    }
                } catch (Exception e) {
                    log.warn("Failed to deserialize points for function {}: {}", f.getId(), e.getMessage());
                }
            }
        }
    }

    private void breadthFirstCollect(UserEntity user, List<FunctionEntity> functions, List<Object> sink) {
        Deque<Object> queue = new ArrayDeque<>();
        queue.add(user); // Возвращаем UserEntity, а не UserDto
        while (!queue.isEmpty()) {
            Object node = queue.poll();
            sink.add(node);
            if (node instanceof UserEntity u) {
                functions.stream()
                        .filter(f -> f.getUser().getId().equals(u.getId()))
                        .forEach(queue::add); // Добавляем FunctionEntity напрямую
            } else if (node instanceof FunctionEntity f) {
                // Извлекаем точки из JSON данных функции (не поточечно!)
                if (f.getXValuesJson() != null && f.getYValuesJson() != null &&
                    !f.getXValuesJson().trim().isEmpty() && !f.getYValuesJson().trim().isEmpty()) {
                    try {
                        double[] xValues = FunctionDataSerializer.deserializeArray(f.getXValuesJson());
                        double[] yValues = FunctionDataSerializer.deserializeArray(f.getYValuesJson());
                        for (int i = 0; i < xValues.length; i++) {
                            queue.add(new PointDto(f.getId(), xValues[i], yValues[i]));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to deserialize points for function {}: {}", f.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    // ---------- Utility filters ----------

    public List<FunctionEntity> filterFunctions(Collection<FunctionEntity> source, Predicate<FunctionEntity> predicate, Sort sort) {
        log.info("Filtering {} functions with custom predicate and sort={}", source == null ? 0 : source.size(), sort);
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(predicate)
                .sorted((a, b) -> {
                    if (sort.isUnsorted()) return 0;
                    return sort.get()
                            .map(order -> compareFunctions(order, a, b))
                            .findFirst()
                            .orElse(0);
                })
                .toList();
    }

    private int compareFunctions(Sort.Order order, FunctionEntity a, FunctionEntity b) {
        int cmp = switch (order.getProperty()) {
            case "name" -> a.getName().compareToIgnoreCase(b.getName());
            case "id" -> Long.compare(
                    a.getId() == null ? Long.MIN_VALUE : a.getId(),
                    b.getId() == null ? Long.MIN_VALUE : b.getId()
            );
            default -> 0;
        };
        return order.isAscending() ? cmp : -cmp;
    }
    
    private UserDto toUserDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole().name()
        );
    }
    
    private FunctionDto toFunctionDto(FunctionEntity function) {
        FunctionDto dto = new FunctionDto(
                function.getId(),
                function.getName(),
                function.getExpression(),
                function.getUser().getId()
        );
        
        // Загружаем данные функции из JSON (не поточечно!)
        try {
            double[] xValues;
            double[] yValues;
            
            // Читаем только из JSON (не поточечно!)
            if (function.getXValuesJson() != null && function.getYValuesJson() != null &&
                !function.getXValuesJson().trim().isEmpty() && !function.getYValuesJson().trim().isEmpty()) {
                xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
                yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            } else {
                // Нет данных - пустые массивы
                xValues = new double[0];
                yValues = new double[0];
            }
            
            // Преобразуем в списки точек для DTO
            List<PointDto> points = new ArrayList<>();
            for (int i = 0; i < xValues.length; i++) {
                points.add(new PointDto(function.getId(), xValues[i], yValues[i]));
            }
            dto.setPoints(points);
            dto.setXValues(xValues);
            dto.setYValues(yValues);
            dto.setCount(xValues.length);
            
            // Проверяем, поддерживает ли функция Insertable и Removable
            boolean isInsertable = xValues.length >= 2;
            boolean isRemovable = xValues.length >= 2;
            dto.setInsertable(isInsertable);
            dto.setRemovable(isRemovable);
        } catch (Exception e) {
            log.error("Error loading function data for function {}: {}", function.getId(), e.getMessage(), e);
            dto.setPoints(new ArrayList<>());
            dto.setXValues(new double[0]);
            dto.setYValues(new double[0]);
            dto.setCount(0);
            dto.setInsertable(false);
            dto.setRemovable(false);
        }
        
        return dto;
    }
}




