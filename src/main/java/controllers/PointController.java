package controllers;

import dto.PointDto;
import entities.FunctionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import services.AccessService;
import services.FunctionService;
import util.FunctionDataSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private static final Logger logger = LoggerFactory.getLogger(PointController.class);

    private final FunctionService functionService;
    private final AccessService accessService;

    public PointController(FunctionService functionService, AccessService accessService) {
        this.functionService = functionService;
        this.accessService = accessService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PointDto>> getAllPoints() {
        // Для админа возвращаем все точки из всех функций
        List<FunctionEntity> functions = functionService.findAll();
        List<PointDto> allPoints = new ArrayList<>();
        for (FunctionEntity function : functions) {
            allPoints.addAll(getPointsFromFunction(function));
        }
        return ResponseEntity.ok(allPoints);
    }

    @GetMapping("/function/{functionId}")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<List<PointDto>> getPointsByFunctionId(@PathVariable Long functionId) {
        return functionService.findById(functionId)
                .map(function -> ResponseEntity.ok(getPointsFromFunction(function)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/function/{functionId}/range")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<List<PointDto>> getPointsByFunctionIdAndRange(
            @PathVariable Long functionId,
            @RequestParam(required = false) Double fromX,
            @RequestParam(required = false) Double toX,
            @RequestParam(required = false, defaultValue = "xValue") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        return functionService.findById(functionId)
                .map(function -> {
                    List<PointDto> points = getPointsFromFunction(function);
                    
                    // Фильтрация по диапазону
                    double from = fromX == null ? Double.NEGATIVE_INFINITY : fromX;
                    double to = toX == null ? Double.POSITIVE_INFINITY : toX;
                    points = points.stream()
                            .filter(p -> p.getXValue() >= from && p.getXValue() <= to)
                            .collect(Collectors.toList());
                    
                    // Сортировка
                    boolean ascending = !sortDir.equalsIgnoreCase("desc");
                    Comparator<PointDto> comparator = switch (sortBy) {
                        case "xValue" -> Comparator.comparing(PointDto::getXValue);
                        case "yValue" -> Comparator.comparing(PointDto::getYValue);
                        default -> Comparator.comparing(PointDto::getXValue);
                    };
                    if (!ascending) {
                        comparator = comparator.reversed();
                    }
                    points.sort(comparator);
                    
                    return ResponseEntity.ok(points);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/function/{functionId}/x/{xValue}")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<PointDto> getPoint(@PathVariable Long functionId, @PathVariable Double xValue) {
        return functionService.findById(functionId)
                .map(function -> {
                    List<PointDto> points = getPointsFromFunction(function);
                    return points.stream()
                            .filter(p -> Math.abs(p.getXValue() - xValue) < 1e-10)
                            .findFirst()
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createPoint(@RequestBody PointDto pointDto) {
        // Логирование для отладки
        logger.info("Received createPoint request. pointDto={}, functionId={}, xValue={}, yValue={}", 
                   pointDto, 
                   pointDto != null ? pointDto.getFunctionId() : "null",
                   pointDto != null ? pointDto.getXValue() : "null",
                   pointDto != null ? pointDto.getYValue() : "null");
        
        // Проверка на null
        if (pointDto == null) {
            logger.error("Cannot create point: pointDto is null");
            return ResponseEntity.badRequest().body("{\"error\": \"Request body is required\"}");
        }
        
        // Валидация входных данных
        if (pointDto.getFunctionId() == null) {
            logger.error("Cannot create point: functionId is null");
            return ResponseEntity.badRequest().body("{\"error\": \"functionId is required\"}");
        }
        if (pointDto.getXValue() == null) {
            logger.error("Cannot create point: xValue is null");
            return ResponseEntity.badRequest().body("{\"error\": \"xValue is required\"}");
        }
        if (pointDto.getYValue() == null) {
            logger.error("Cannot create point: yValue is null");
            return ResponseEntity.badRequest().body("{\"error\": \"yValue is required\"}");
        }
        
        // Проверка доступа
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !accessService.canAccessFunction(pointDto.getFunctionId(), authentication)) {
            logger.warn("Access denied for functionId={}, user={}", 
                       pointDto.getFunctionId(), 
                       authentication != null ? authentication.getName() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Access denied to this function\"}");
        }
        
        try {
            // Используем insertPoint из FunctionService (работает с JSON, не поточечно!)
            functionService.insertPoint(pointDto.getFunctionId(), pointDto.getXValue(), pointDto.getYValue());
            logger.info("Point created via API. functionId={}, xValue={}", pointDto.getFunctionId(), pointDto.getXValue());
            return ResponseEntity.status(HttpStatus.CREATED).body(pointDto);
        } catch (IllegalArgumentException e) {
            logger.error("Cannot create point: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.error("Unexpected error creating point", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/function/{functionId}/x/{xValue}")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<PointDto> updatePoint(
            @PathVariable Long functionId,
            @PathVariable Double xValue,
            @RequestBody PointDto pointDto) {
        try {
            // Находим индекс точки и обновляем через updateYValues
            return functionService.findById(functionId)
                    .map(function -> {
                        if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
                            function.getXValuesJson().trim().isEmpty() || function.getYValuesJson().trim().isEmpty()) {
                            return ResponseEntity.notFound().<PointDto>build();
                        }
                        
                        double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
                        double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
                        
                        for (int i = 0; i < xValues.length; i++) {
                            if (Math.abs(xValues[i] - xValue) < 1e-10) {
                                yValues[i] = pointDto.getYValue();
                                functionService.updateYValues(functionId, yValues);
                                logger.info("Point updated via API. functionId={}, xValue={}", functionId, xValue);
                                return ResponseEntity.ok(new PointDto(functionId, xValue, pointDto.getYValue()));
                            }
                        }
                        return ResponseEntity.notFound().<PointDto>build();
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error updating point", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/function/{functionId}/x/{xValue}")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<?> deletePoint(@PathVariable Long functionId, @PathVariable Double xValue) {
        try {
            // Находим индекс точки и удаляем через removePoint
            return functionService.findById(functionId)
                    .map(function -> {
                        if (function.getXValuesJson() == null || function.getXValuesJson().trim().isEmpty()) {
                            logger.warn("Function {} has no data", functionId);
                            return ResponseEntity.notFound().<Void>build();
                        }
                        
                        double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
                        for (int i = 0; i < xValues.length; i++) {
                            if (Math.abs(xValues[i] - xValue) < 1e-10) {
                                try {
                                    functionService.removePoint(functionId, i);
                                    logger.info("Point deleted via API. functionId={}, xValue={}, index={}", functionId, xValue, i);
                                    return ResponseEntity.noContent().<Void>build();
                                } catch (IllegalArgumentException e) {
                                    logger.error("Cannot delete point: {}", e.getMessage());
                                    return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
                                }
                            }
                        }
                        logger.warn("Point with xValue={} not found in function {}", xValue, functionId);
                        return ResponseEntity.notFound().<Void>build();
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Unexpected error deleting point", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private List<PointDto> getPointsFromFunction(FunctionEntity function) {
        List<PointDto> points = new ArrayList<>();
        if (function.getXValuesJson() != null && function.getYValuesJson() != null &&
            !function.getXValuesJson().trim().isEmpty() && !function.getYValuesJson().trim().isEmpty()) {
            try {
                double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
                double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
                for (int i = 0; i < xValues.length; i++) {
                    points.add(new PointDto(function.getId(), xValues[i], yValues[i]));
                }
            } catch (Exception e) {
                logger.error("Error deserializing points for function {}: {}", function.getId(), e.getMessage());
            }
        }
        return points;
    }
}
