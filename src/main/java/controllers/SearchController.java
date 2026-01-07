package controllers;

import dto.FunctionDto;
import dto.PointDto;
import dto.UserDto;
import entities.FunctionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import services.SearchService;
import util.FunctionDataSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/users/username/{username}")
    @PreAuthorize("@accessService.canAccessUsername(#username, authentication)")
    public ResponseEntity<UserDto> findUserByUsername(@PathVariable String username) {
        return searchService.findUserByUsername(username)
                .map(user -> ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/functions/{id}")
    @PreAuthorize("@accessService.canAccessFunction(#id, authentication)")
    public ResponseEntity<FunctionDto> findFunctionById(@PathVariable Long id) {
        return searchService.findFunctionById(id)
                .map(func -> ResponseEntity.ok(toFunctionDto(func)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/points/function/{functionId}/x/{xValue}")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<PointDto> findPoint(@PathVariable Long functionId, @PathVariable Double xValue) {
        return searchService.findPoint(functionId, xValue)
                .map(point -> ResponseEntity.ok(point))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/functions/user/{userId}")
    @PreAuthorize("@accessService.canAccessUserFunctions(#userId, authentication)")
    public ResponseEntity<List<FunctionDto>> searchFunctions(
            @PathVariable Long userId,
            @RequestParam(required = false) String nameLike,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        List<FunctionDto> functions = searchService.searchFunctions(userId, nameLike, sort).stream()
                .map(this::toFunctionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(functions);
    }

    @GetMapping("/points/function/{functionId}")
    @PreAuthorize("@accessService.canAccessFunction(#functionId, authentication)")
    public ResponseEntity<List<PointDto>> searchPoints(
            @PathVariable Long functionId,
            @RequestParam(required = false) Double fromX,
            @RequestParam(required = false) Double toX,
            @RequestParam(required = false, defaultValue = "id.xValue") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        List<PointDto> points = searchService.searchPoints(functionId, fromX, toX, sort);
        return ResponseEntity.ok(points);
    }

    @GetMapping("/hierarchy/user/{userId}/breadth-first")
    @PreAuthorize("@accessService.canAccessUser(#userId, authentication)")
    public ResponseEntity<List<Object>> breadthFirstHierarchy(@PathVariable Long userId) {
        List<Object> hierarchy = searchService.breadthFirstHierarchy(userId);
        logger.info("Hierarchy breadth-first requested. userId={}", userId);
        return ResponseEntity.ok(hierarchy);
    }

    @GetMapping("/hierarchy/user/{userId}/depth-first")
    @PreAuthorize("@accessService.canAccessUser(#userId, authentication)")
    public ResponseEntity<List<Object>> depthFirstHierarchy(@PathVariable Long userId) {
        List<Object> hierarchy = searchService.depthFirstHierarchy(userId);
        logger.info("Hierarchy depth-first requested. userId={}", userId);
        return ResponseEntity.ok(hierarchy);
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
        } catch (Exception e) {
            logger.error("Error loading function data for function {}: {}", function.getId(), e.getMessage(), e);
            dto.setPoints(new ArrayList<>());
            dto.setXValues(new double[0]);
            dto.setYValues(new double[0]);
            dto.setCount(0);
        }
        
        return dto;
    }
}
