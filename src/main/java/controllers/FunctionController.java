package controllers;

import dto.CreateFunctionFromArraysRequest;
import dto.CreateFunctionFromMathRequest;
import dto.FunctionDto;
import dto.PointDto;
import dto.FunctionOperationRequest;
import dto.UpdateYValuesRequest;
import dto.CreateCompositeFunctionRequest;
import entities.FunctionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/functions")
public class FunctionController {

    private static final Logger logger = LoggerFactory.getLogger(FunctionController.class);

    private final FunctionService functionService;
    private final AccessService accessService;

    public FunctionController(FunctionService functionService, AccessService accessService) {
        this.functionService = functionService;
        this.accessService = accessService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FunctionDto>> getAllFunctions() {
        List<FunctionDto> functions = functionService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(functions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<FunctionDto> getFunctionById(@PathVariable Long id) {
        return functionService.findById(id)
                .map(func -> ResponseEntity.ok(toDto(func)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@accessService.canAccessUserFunctions(#userId, authentication)")
    public ResponseEntity<List<FunctionDto>> getFunctionsByUserId(@PathVariable Long userId) {
        List<FunctionDto> functions = functionService.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(functions);
    }

    @GetMapping("/user/{userId}/search")
    @PreAuthorize("@accessService.canAccessUserFunctions(#userId, authentication)")
    public ResponseEntity<List<FunctionDto>> searchFunctions(
            @PathVariable Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        String nameLike = name == null ? "" : name;
        List<FunctionDto> functions = functionService.findByUserIdAndNameContaining(userId, nameLike, sort).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(functions);
    }

    @PostMapping
    public ResponseEntity<?> createFunction(@RequestBody FunctionDto functionDto) {
        // Валидация входных данных
        if (functionDto.getUserId() == null) {
            logger.error("Cannot create function: userId is null");
            return ResponseEntity.badRequest().body("{\"error\": \"userId is required\"}");
        }
        if (functionDto.getName() == null || functionDto.getName().trim().isEmpty()) {
            logger.error("Cannot create function: name is null or empty");
            return ResponseEntity.badRequest().body("{\"error\": \"name is required\"}");
        }
        
        // Проверка доступа после десериализации
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !accessService.canAccessUser(functionDto.getUserId(), authentication)) {
            logger.warn("Access denied for userId={}, user={}", 
                       functionDto.getUserId(), 
                       authentication != null ? authentication.getName() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Access denied to this user\"}");
        }
        
        try {
            FunctionEntity function = functionService.createFunction(
                    functionDto.getName(),
                    functionDto.getExpression(),
                    functionDto.getUserId()
            );
            logger.info("Function created via API. id={}, userId={}", function.getId(), function.getUser().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(function));
        } catch (IllegalArgumentException e) {
            logger.error("Cannot create function: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.error("Unexpected error creating function", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<?> updateFunction(@PathVariable Long id, @RequestBody FunctionDto functionDto) {
        try {
            FunctionEntity updated = functionService.updateFunction(id, functionDto);
            logger.info("Function updated via API. id={}", updated.getId());
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            logger.error("Cannot update function: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.error("Unexpected error updating function", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<Void> deleteFunction(@PathVariable Long id) {
        if (functionService.existsById(id)) {
            functionService.deleteById(id);
            logger.info("Function deleted via API. id={}", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/preview-from-math")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> previewFunctionFromMath(@RequestBody CreateFunctionFromMathRequest request) {
        logger.info("Previewing function from math: type={}, xFrom={}, xTo={}, count={}", 
                   request.getMathFunctionType(), 
                   request.getXFrom(), request.getXTo(), request.getCount());
        try {
            FunctionEntity function = functionService.previewFunctionFromMath(request);
            logger.info("Function preview generated");
            return ResponseEntity.ok(toDto(function));
        } catch (IllegalArgumentException e) {
            logger.error("Error previewing function from math: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/create-from-math")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> createFunctionFromMath(@RequestBody CreateFunctionFromMathRequest request) {
        logger.info("Creating function from math: name={}, type={}, xFrom={}, xTo={}, count={}", 
                   request.getName(), request.getMathFunctionType(), 
                   request.getXFrom(), request.getXTo(), request.getCount());
        try {
            FunctionEntity function = functionService.createFunctionFromMath(request);
            logger.info("Function created from math. id={}", function.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(function));
        } catch (IllegalArgumentException e) {
            logger.error("Error creating function from math: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/create-from-arrays")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> createFunctionFromArrays(@RequestBody CreateFunctionFromArraysRequest request) {
        logger.info("Creating function from arrays: name={}, points={}", 
                   request.getName(), 
                   request.getXValues() != null ? request.getXValues().length : 0);
        try {
            FunctionEntity function = functionService.createFunctionFromArrays(request);
            logger.info("Function created from arrays. id={}", function.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(function));
        } catch (IllegalArgumentException e) {
            logger.error("Error creating function from arrays: {}", e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/available-math-functions")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<String>> getAvailableMathFunctions() {
        logger.info("Getting available math functions");
        List<String> functions = functionService.getAvailableMathFunctions();
        return ResponseEntity.ok(functions);
    }

    @PostMapping("/operate")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> performOperation(@RequestBody FunctionOperationRequest request) {
        logger.info("Performing operation {} on functions {} and {}", 
                   request.getOperation(), request.getFunctionId1(), request.getFunctionId2());
        try {
            FunctionEntity result = functionService.performOperation(
                    request.getFunctionId1(),
                    request.getFunctionId2(),
                    request.getOperation(),
                    request.getResultName(),
                    request.getFactoryType() != null ? request.getFactoryType() : "ARRAY"
            );
            logger.info("Operation completed, result ID: {}", result.getId());
            return ResponseEntity.ok(toDto(result));
        } catch (IllegalArgumentException e) {
            logger.error("Error performing operation: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error performing operation", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Внутренняя ошибка сервера: " + e.getMessage());
            errorResponse.put("message", "Внутренняя ошибка сервера: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/composite")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> createCompositeFunction(@RequestBody CreateCompositeFunctionRequest request) {
        logger.info("Creating composite function: {} = {} ∘ {}", 
                   request.getName(), request.getOuterFunction(), request.getInnerFunction());
        try {
            Map<String, Object> result = functionService.createCompositeFunction(
                    request.getName(),
                    request.getInnerFunction(),
                    request.getOuterFunction()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating composite function: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/y-values")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<FunctionDto> updateYValues(
            @PathVariable Long id,
            @RequestBody UpdateYValuesRequest request) {
        logger.info("Updating Y values for function {}", id);
        try {
            FunctionEntity updated = functionService.updateYValues(id, request.getYValues());
            logger.info("Y values updated for function {}", id);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            logger.error("Error updating Y values: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/insert-point")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<FunctionDto> insertPoint(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        logger.info("Inserting point into function {}", id);
        try {
            Double x = request.get("x");
            Double y = request.get("y");
            if (x == null || y == null) {
                return ResponseEntity.badRequest().build();
            }
            FunctionEntity updated = functionService.insertPoint(id, x, y);
            logger.info("Point inserted into function {}", id);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            logger.error("Error inserting point: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/remove-point/{index}")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<FunctionDto> removePoint(
            @PathVariable Long id,
            @PathVariable int index) {
        logger.info("Removing point at index {} from function {}", index, id);
        try {
            FunctionEntity updated = functionService.removePoint(id, index);
            logger.info("Point removed from function {}", id);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            logger.error("Error removing point: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private FunctionDto toDto(FunctionEntity function) {
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
                logger.debug("Loaded function {} data from JSON: {} points", function.getId(), xValues.length);
            } else {
                // Нет данных - пустые массивы
                logger.warn("Function {} has no JSON data", function.getId());
                xValues = new double[0];
                yValues = new double[0];
            }
            
            // Преобразуем в списки точек для DTO
            List<PointDto> points = new ArrayList<>();
            for (int i = 0; i < xValues.length; i++) {
                points.add(new PointDto(function.getId(), xValues[i], yValues[i]));
            }
            dto.setPoints(points);
            
            // Устанавливаем массивы xValues и yValues
            dto.setXValues(xValues);
            dto.setYValues(yValues);
            dto.setCount(xValues.length);
            
            // Проверяем, поддерживает ли функция Insertable и Removable
            boolean isInsertable = xValues.length >= 2;
            boolean isRemovable = xValues.length >= 2;
            dto.setInsertable(isInsertable);
            dto.setRemovable(isRemovable);
            
            logger.debug("Loaded {} points for function {}", xValues.length, function.getId());
        } catch (Exception e) {
            logger.error("Error loading function data for function {}: {}", function.getId(), e.getMessage(), e);
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
