package services;

import config.SecurityUserDetails;
import dto.CreateFunctionFromArraysRequest;
import dto.CreateFunctionFromMathRequest;
import entities.FunctionEntity;
import entities.UserEntity;
import functions.*;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import operations.TabulatedFunctionOperationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import repositories.FunctionRepository;
import repositories.UserRepository;
import repositories.CompositeFunctionRepository;
import entities.CompositeFunctionEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FunctionDataSerializer;

import java.util.*;

@Service
@Transactional
public class FunctionService {

    private static final Logger logger = LoggerFactory.getLogger(FunctionService.class);

    private final FunctionRepository functionRepository;
    private final UserRepository userRepository;
    private final CompositeFunctionRepository compositeFunctionRepository;

    // Map доступных математических функций
    private static final Map<String, MathFunction> MATH_FUNCTIONS = createMathFunctionsMap();

    private static Map<String, MathFunction> createMathFunctionsMap() {
        Map<String, MathFunction> map = new LinkedHashMap<>();
        map.put("SqrFunction", new SqrFunction());
        map.put("IdentityFunction", new IdentityFunction());
        map.put("UnitFunction", new UnitFunction());
        map.put("ZeroFunction", new ZeroFunction());
        map.put("ConstantFunction", new ConstantFunction(1.0));
        map.put("SinFunction", new SinFunction());
        map.put("CosFunction", new CosFunction());
        map.put("LnFunction", new LnFunction());
        map.put("ExpFunction", new ExpFunction());
        return map;
    }
    
    // Получает базовые математические функции (встроенные + компонентные пользователя, рекурсивно)
    private Map<String, MathFunction> getAllMathFunctionsForUser(Long userId) {
        Map<String, MathFunction> allFunctions = new LinkedHashMap<>(MATH_FUNCTIONS);
        
        // Добавляем компонентные функции пользователя (рекурсивно)
        List<CompositeFunctionEntity> userCompositeFunctions = compositeFunctionRepository.findByUserId(userId);
        Set<String> processed = new HashSet<>();
        Set<String> processing = new HashSet<>();
        
        // Обрабатываем компонентные функции, пока не обработаем все
        int maxIterations = userCompositeFunctions.size() + 1;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean progress = false;
            for (CompositeFunctionEntity compositeEntity : userCompositeFunctions) {
                if (processed.contains(compositeEntity.getName())) {
                    continue; // Уже обработана
                }
                
                // Пропускаем, если уже обрабатываем эту функцию (защита от циклических зависимостей)
                if (processing.contains(compositeEntity.getName())) {
                    continue;
                }
                
                try {
                    // Получаем базовые функции для внутренней и внешней функций
                    MathFunction innerFunction = getMathFunctionByName(compositeEntity.getInnerFunction(), userId, allFunctions, processing);
                    MathFunction outerFunction = getMathFunctionByName(compositeEntity.getOuterFunction(), userId, allFunctions, processing);
                    
                    if (innerFunction != null && outerFunction != null) {
                        CompositeFunction composite = new CompositeFunction(innerFunction, outerFunction);
                        allFunctions.put(compositeEntity.getName(), composite);
                        processed.add(compositeEntity.getName());
                        progress = true;
                    }
                } catch (Exception e) {
                    logger.warn("Error creating composite function {} for user {}: {}", 
                               compositeEntity.getName(), userId, e.getMessage());
                }
            }
            
            if (!progress) {
                break; // Нет прогресса, выходим
            }
        }
        
        return allFunctions;
    }
    
    // Получает математическую функцию по имени (может быть встроенной или компонентной)
    private MathFunction getMathFunctionByName(String functionName, Long userId, 
                                               Map<String, MathFunction> allFunctions, Set<String> processing) {
        // Сначала проверяем встроенные функции
        if (MATH_FUNCTIONS.containsKey(functionName)) {
            return MATH_FUNCTIONS.get(functionName);
        }
        
        // Если уже есть в allFunctions, возвращаем оттуда
        if (allFunctions.containsKey(functionName)) {
            return allFunctions.get(functionName);
        }
        
        // Ищем компонентную функцию пользователя
        Optional<CompositeFunctionEntity> compositeOpt = compositeFunctionRepository.findByUserIdAndNameIgnoreCase(userId, functionName);
        if (compositeOpt.isPresent()) {
            CompositeFunctionEntity compositeEntity = compositeOpt.get();
            
            // Защита от циклических зависимостей
            if (processing.contains(functionName)) {
                logger.warn("Circular dependency detected for function: {}", functionName);
                return null;
            }
            
            processing.add(functionName);
            try {
                MathFunction innerFunction = getMathFunctionByName(compositeEntity.getInnerFunction(), userId, allFunctions, processing);
                MathFunction outerFunction = getMathFunctionByName(compositeEntity.getOuterFunction(), userId, allFunctions, processing);
                
                if (innerFunction != null && outerFunction != null) {
                    CompositeFunction composite = new CompositeFunction(innerFunction, outerFunction);
                    allFunctions.put(functionName, composite);
                    processing.remove(functionName);
                    return composite;
                }
            } catch (Exception e) {
                logger.warn("Error creating composite function {}: {}", functionName, e.getMessage());
            } finally {
                processing.remove(functionName);
            }
        }
        
        return null;
    }

    public FunctionService(FunctionRepository functionRepository, UserRepository userRepository, 
                          CompositeFunctionRepository compositeFunctionRepository) {
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
        this.compositeFunctionRepository = compositeFunctionRepository;
    }

    public List<String> getAvailableMathFunctions() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            // Если пользователь не аутентифицирован, возвращаем только встроенные функции
            return new ArrayList<>(MATH_FUNCTIONS.keySet());
        }
        
        // Возвращаем встроенные функции + компонентные функции текущего пользователя
        List<String> allFunctions = new ArrayList<>(MATH_FUNCTIONS.keySet());
        List<CompositeFunctionEntity> userCompositeFunctions = compositeFunctionRepository.findByUserId(userId);
        for (CompositeFunctionEntity composite : userCompositeFunctions) {
            allFunctions.add(composite.getName());
        }
        return allFunctions;
    }

    public List<FunctionEntity> findAll() {
        return functionRepository.findAll();
    }

    public Optional<FunctionEntity> findById(Long id) {
        return functionRepository.findById(id);
    }

    public List<FunctionEntity> findByUserId(Long userId) {
        return functionRepository.findByUserId(userId);
    }

    public List<FunctionEntity> findByUserIdAndNameContaining(Long userId, String nameLike, Sort sort) {
        return functionRepository.findByUserIdAndNameContainingIgnoreCase(userId, nameLike, sort);
    }

    public FunctionEntity save(FunctionEntity function) {
        return functionRepository.save(function);
    }
    
    public FunctionEntity updateFunction(Long id, dto.FunctionDto functionDto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        
        FunctionEntity function = functionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Функция не найдена"));
        
        // Проверка доступа: администратор может обновлять любую функцию, обычный пользователь - только свою
        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && !function.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет доступа к функции");
        }
        
        // Проверка уникальности имени (исключая текущую функцию)
        // Для администратора проверяем уникальность в рамках владельца функции
        Long ownerId = function.getUser().getId();
        if (!function.getName().equalsIgnoreCase(functionDto.getName()) &&
            functionRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(ownerId, functionDto.getName(), id)) {
            throw new IllegalArgumentException("Функция с именем '" + functionDto.getName() + "' уже существует");
        }
        
        function.setName(functionDto.getName());
        if (functionDto.getExpression() != null) {
            function.setExpression(functionDto.getExpression());
        }
        
        return functionRepository.save(function);
    }

    public FunctionEntity createFunction(String name, String expression, Long userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Проверка уникальности имени функции для пользователя
        if (functionRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new IllegalArgumentException("Функция с именем '" + name + "' уже существует");
        }
        
        FunctionEntity function = new FunctionEntity(name, expression, userOpt.get());
        return functionRepository.save(function);
    }

    public void deleteById(Long id) {
        functionRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return functionRepository.existsById(id);
    }

    // Метод для предпросмотра функции без сохранения в БД
    public FunctionEntity previewFunctionFromMath(CreateFunctionFromMathRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        // Получение математической функции (включая составные функции пользователя)
        Map<String, MathFunction> allFunctions = getAllMathFunctionsForUser(userId);
        MathFunction mathFunction = allFunctions.get(request.getMathFunctionType());
        if (mathFunction == null) {
            throw new IllegalArgumentException("Unknown math function type: " + request.getMathFunctionType());
        }

        // Выбор фабрики
        TabulatedFunctionFactory factory = "LINKED_LIST".equals(request.getFactoryType())
                ? new LinkedListTabulatedFunctionFactory()
                : new ArrayTabulatedFunctionFactory();

        // Создание табулированной функции
        TabulatedFunction tabulatedFunction = factory.create(
                mathFunction,
                request.getXFrom(),
                request.getXTo(),
                request.getCount()
        );

        // Извлекаем массивы x и y значений из табулированной функции
        double[] xValues = new double[tabulatedFunction.getCount()];
        double[] yValues = new double[tabulatedFunction.getCount()];
        int index = 0;
        for (Point point : tabulatedFunction) {
            // Проверяем, что значения валидны
            if (Double.isNaN(point.x) || Double.isInfinite(point.x) ||
                Double.isNaN(point.y) || Double.isInfinite(point.y)) {
                logger.warn("Skipping point with invalid values: x={}, y={}", point.x, point.y);
                continue;
            }
            xValues[index] = point.x;
            yValues[index] = point.y;
            index++;
        }
        
        // Обрезаем массивы до реального размера (если были пропущены точки)
        if (index < xValues.length) {
            double[] trimmedX = new double[index];
            double[] trimmedY = new double[index];
            System.arraycopy(xValues, 0, trimmedX, 0, index);
            System.arraycopy(yValues, 0, trimmedY, 0, index);
            xValues = trimmedX;
            yValues = trimmedY;
        }

        // Создаем временную функцию для предпросмотра (НЕ сохраняем в БД)
        String dbExpression = request.getMathFunctionType() + "[" + request.getXFrom() + "," + request.getXTo() + "," + request.getCount() + "]";
        FunctionEntity function = new FunctionEntity(request.getName() != null ? request.getName() : "preview", dbExpression, userOpt.get());
        
        // Сохраняем данные функции как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(xValues, yValues);
        function.setXValuesJson(jsonData[0]);
        function.setYValuesJson(jsonData[1]);
        function.setCount(xValues.length);
        
        // НЕ сохраняем в БД - это только для предпросмотра
        logger.info("Function preview generated (not saved)");

        return function;
    }

    public FunctionEntity createFunctionFromMath(CreateFunctionFromMathRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        // Проверка уникальности имени функции для пользователя
        if (functionRepository.existsByUserIdAndNameIgnoreCase(userId, request.getName())) {
            throw new IllegalArgumentException("Функция с именем '" + request.getName() + "' уже существует");
        }

        // Получение математической функции (включая составные функции пользователя)
        Map<String, MathFunction> allFunctions = getAllMathFunctionsForUser(userId);
        MathFunction mathFunction = allFunctions.get(request.getMathFunctionType());
        if (mathFunction == null) {
            throw new IllegalArgumentException("Unknown math function type: " + request.getMathFunctionType());
        }

        // Выбор фабрики
        TabulatedFunctionFactory factory = "LINKED_LIST".equals(request.getFactoryType())
                ? new LinkedListTabulatedFunctionFactory()
                : new ArrayTabulatedFunctionFactory();

        // Создание табулированной функции
        TabulatedFunction tabulatedFunction = factory.create(
                mathFunction,
                request.getXFrom(),
                request.getXTo(),
                request.getCount()
        );

        // Извлекаем массивы x и y значений из табулированной функции
        double[] xValues = new double[tabulatedFunction.getCount()];
        double[] yValues = new double[tabulatedFunction.getCount()];
        int index = 0;
        for (Point point : tabulatedFunction) {
            // Проверяем, что значения валидны
            if (Double.isNaN(point.x) || Double.isInfinite(point.x) ||
                Double.isNaN(point.y) || Double.isInfinite(point.y)) {
                logger.warn("Skipping point with invalid values: x={}, y={}", point.x, point.y);
                continue;
            }
            xValues[index] = point.x;
            yValues[index] = point.y;
            index++;
        }
        
        // Обрезаем массивы до реального размера (если были пропущены точки)
        if (index < xValues.length) {
            double[] trimmedX = new double[index];
            double[] trimmedY = new double[index];
            System.arraycopy(xValues, 0, trimmedX, 0, index);
            System.arraycopy(yValues, 0, trimmedY, 0, index);
            xValues = trimmedX;
            yValues = trimmedY;
        }

        // Создание функции в БД с сохранением данных целиком как JSON
        String dbExpression = request.getMathFunctionType() + "[" + request.getXFrom() + "," + request.getXTo() + "," + request.getCount() + "]";
        FunctionEntity function = new FunctionEntity(request.getName(), dbExpression, userOpt.get());
        
        // Сохраняем данные функции как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(xValues, yValues);
        function.setXValuesJson(jsonData[0]);
        function.setYValuesJson(jsonData[1]);
        function.setCount(xValues.length);
        
        FunctionEntity savedFunction = functionRepository.save(function);
        logger.info("Function saved as whole (not point by point) with ID: {}", savedFunction.getId());

        return savedFunction;
    }

    public FunctionEntity createFunctionFromArrays(CreateFunctionFromArraysRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        // Проверка уникальности имени функции для пользователя
        if (functionRepository.existsByUserIdAndNameIgnoreCase(userId, request.getName())) {
            throw new IllegalArgumentException("Функция с именем '" + request.getName() + "' уже существует");
        }

        double[] xValues = request.getXValues();
        double[] yValues = request.getYValues();

        // Валидация
        if (xValues == null || yValues == null) {
            throw new IllegalArgumentException("xValues and yValues cannot be null");
        }
        if (xValues.length == 0 || yValues.length == 0) {
            throw new IllegalArgumentException("Arrays cannot be empty");
        }
        if (xValues.length != yValues.length) {
            throw new IllegalArgumentException("Arrays must have the same length");
        }

        // Выбор фабрики и валидация через создание функции
        TabulatedFunctionFactory factory = "LINKED_LIST".equals(request.getFactoryType())
                ? new LinkedListTabulatedFunctionFactory()
                : new ArrayTabulatedFunctionFactory();

        try {
            factory.create(xValues, yValues);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid function data: " + e.getMessage());
        }

        // Фильтруем невалидные значения
        List<Double> validX = new ArrayList<>();
        List<Double> validY = new ArrayList<>();
        for (int i = 0; i < xValues.length; i++) {
            if (Double.isNaN(xValues[i]) || Double.isInfinite(xValues[i]) ||
                Double.isNaN(yValues[i]) || Double.isInfinite(yValues[i])) {
                logger.warn("Skipping point with invalid values: x={}, y={}", xValues[i], yValues[i]);
                continue;
            }
            validX.add(xValues[i]);
            validY.add(yValues[i]);
        }
        
        if (validX.isEmpty()) {
            throw new IllegalArgumentException("No valid points after filtering");
        }
        
        // Преобразуем обратно в массивы
        double[] validXArray = validX.stream().mapToDouble(Double::doubleValue).toArray();
        double[] validYArray = validY.stream().mapToDouble(Double::doubleValue).toArray();

        // Создание функции в БД с сохранением данных целиком как JSON
        String expression = request.getFactoryType() != null ? request.getFactoryType() : "ARRAY";
        FunctionEntity function = new FunctionEntity(request.getName(), expression, userOpt.get());
        
        // Сохраняем данные функции как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(validXArray, validYArray);
        function.setXValuesJson(jsonData[0]);
        function.setYValuesJson(jsonData[1]);
        function.setCount(validXArray.length);
        
        FunctionEntity savedFunction = functionRepository.save(function);
        logger.info("Function saved as whole (not point by point) with ID: {}", savedFunction.getId());

        return savedFunction;
    }

    public FunctionEntity performOperation(Long functionId1, Long functionId2, String operation, String resultName, String factoryType) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        
        // Получаем функции
        FunctionEntity func1 = functionRepository.findById(functionId1)
                .orElseThrow(() -> new IllegalArgumentException("Функция 1 не найдена"));
        FunctionEntity func2 = functionRepository.findById(functionId2)
                .orElseThrow(() -> new IllegalArgumentException("Функция 2 не найдена"));
        
        // Проверяем права доступа: администратор может работать с любыми функциями
        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && (!func1.getUser().getId().equals(userId) || !func2.getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("Нет доступа к функциям");
        }
        
        // Получаем данные функций из JSON (не поточечно!)
        if (func1.getXValuesJson() == null || func1.getYValuesJson() == null ||
            func1.getXValuesJson().trim().isEmpty() || func1.getYValuesJson().trim().isEmpty()) {
            throw new IllegalArgumentException("Функция 1 не содержит данных (xValues или yValues пусты)");
        }
        if (func2.getXValuesJson() == null || func2.getYValuesJson() == null ||
            func2.getXValuesJson().trim().isEmpty() || func2.getYValuesJson().trim().isEmpty()) {
            throw new IllegalArgumentException("Функция 2 не содержит данных (xValues или yValues пусты)");
        }
        
        double[] x1 = FunctionDataSerializer.deserializeArray(func1.getXValuesJson());
        double[] y1 = FunctionDataSerializer.deserializeArray(func1.getYValuesJson());
        double[] x2 = FunctionDataSerializer.deserializeArray(func2.getXValuesJson());
        double[] y2 = FunctionDataSerializer.deserializeArray(func2.getYValuesJson());
        
        // Проверяем совместимость
        if (x1.length != x2.length) {
            throw new IllegalArgumentException("Функции имеют разное количество точек: " + x1.length + " vs " + x2.length);
        }
        
        if (x1.length == 0) {
            throw new IllegalArgumentException("Нет валидных точек для выполнения операции");
        }
        
        // Проверяем совпадение X значений
        for (int i = 0; i < x1.length; i++) {
            if (Math.abs(x1[i] - x2[i]) > 1e-10) {
                throw new IllegalArgumentException(
                    String.format("Функции имеют разные X значения на позиции %d: %.6f vs %.6f", i, x1[i], x2[i])
                );
            }
        }
        
        // Выбираем фабрику
        TabulatedFunctionFactory factory = "LINKED_LIST".equals(factoryType)
                ? new LinkedListTabulatedFunctionFactory()
                : new ArrayTabulatedFunctionFactory();
        
        // Создаем табулированные функции
        TabulatedFunction tabFunc1 = factory.create(x1, y1);
        TabulatedFunction tabFunc2 = factory.create(x2, y2);
        
        // Выполняем операцию
        TabulatedFunctionOperationService operationService = new TabulatedFunctionOperationService(factory);
        TabulatedFunction result;
        
        switch (operation.toUpperCase()) {
            case "ADD":
                result = operationService.add(tabFunc1, tabFunc2);
                break;
            case "SUBTRACT":
                result = operationService.subtract(tabFunc1, tabFunc2);
                break;
            case "MULTIPLY":
                result = operationService.multiply(tabFunc1, tabFunc2);
                break;
            case "DIVIDE":
                result = operationService.divide(tabFunc1, tabFunc2);
                break;
            default:
                throw new IllegalArgumentException("Неизвестная операция: " + operation);
        }
        
        // Извлекаем массивы x и y значений из результата
        double[] resultXValues = new double[result.getCount()];
        double[] resultYValues = new double[result.getCount()];
        int index = 0;
        for (Point point : result) {
            if (Double.isNaN(point.x) || Double.isInfinite(point.x) || 
                Double.isNaN(point.y) || Double.isInfinite(point.y)) {
                logger.warn("Skipping point with invalid values: x={}, y={}", point.x, point.y);
                continue;
            }
            resultXValues[index] = point.x;
            resultYValues[index] = point.y;
            index++;
        }
        
        // Обрезаем массивы до реального размера (если были пропущены точки)
        if (index < resultXValues.length) {
            double[] trimmedX = new double[index];
            double[] trimmedY = new double[index];
            System.arraycopy(resultXValues, 0, trimmedX, 0, index);
            System.arraycopy(resultYValues, 0, trimmedY, 0, index);
            resultXValues = trimmedX;
            resultYValues = trimmedY;
        }
        
        // Сохраняем результат
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        String expression = operation + "(" + func1.getName() + "," + func2.getName() + ")";
        FunctionEntity resultFunction = new FunctionEntity(resultName, expression, userOpt.get());
        
        // Сохраняем данные функции как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(resultXValues, resultYValues);
        resultFunction.setXValuesJson(jsonData[0]);
        resultFunction.setYValuesJson(jsonData[1]);
        resultFunction.setCount(resultXValues.length);
        
        FunctionEntity savedFunction = functionRepository.save(resultFunction);
        logger.info("Operation {} completed, result saved as whole (not point by point) with ID: {}", operation, savedFunction.getId());
        return savedFunction;
    }
    
    public Map<String, Object> createCompositeFunction(String name, String innerFunctionName, String outerFunctionName) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Проверка уникальности имени компонентной функции для пользователя
        if (compositeFunctionRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new IllegalArgumentException("Компонентная функция с именем '" + name + "' уже существует");
        }
        
        // Получаем доступные функции для пользователя
        Map<String, MathFunction> allFunctions = getAllMathFunctionsForUser(userId);
        
        MathFunction innerFunction = allFunctions.get(innerFunctionName);
        MathFunction outerFunction = allFunctions.get(outerFunctionName);
        
        if (innerFunction == null) {
            throw new IllegalArgumentException("Внутренняя функция не найдена: " + innerFunctionName);
        }
        if (outerFunction == null) {
            throw new IllegalArgumentException("Внешняя функция не найдена: " + outerFunctionName);
        }
        
        // Сохраняем компонентную функцию в БД
        CompositeFunctionEntity compositeEntity = new CompositeFunctionEntity(
            name, innerFunctionName, outerFunctionName, userOpt.get()
        );
        compositeFunctionRepository.save(compositeEntity);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", name);
        response.put("innerFunction", innerFunctionName);
        response.put("outerFunction", outerFunctionName);
        response.put("description", outerFunctionName + " ∘ " + innerFunctionName);
        
        logger.info("Создана составная функция для пользователя {}: {} = {} ∘ {}", 
                   userId, name, outerFunctionName, innerFunctionName);
        
        return response;
    }
    
    public FunctionEntity updateYValues(Long functionId, double[] yValues) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        
        FunctionEntity function = functionRepository.findById(functionId)
                .orElseThrow(() -> new IllegalArgumentException("Функция не найдена"));
        
        // Проверка доступа: администратор может обновлять любую функцию
        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && !function.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет доступа к функции");
        }
        
        // Получаем данные функции из JSON (не поточечно!)
        if (function.getXValuesJson() == null || function.getXValuesJson().trim().isEmpty()) {
            throw new IllegalArgumentException("Функция не содержит данных (xValues пусты)");
        }
        
        double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
        
        if (xValues.length != yValues.length) {
            throw new IllegalArgumentException("Количество Y значений не совпадает с количеством точек");
        }
        
        // Сохраняем обновленную функцию целиком как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(xValues, yValues);
        function.setXValuesJson(jsonData[0]);
        function.setYValuesJson(jsonData[1]);
        function.setCount(xValues.length);
        functionRepository.save(function);
        
        logger.info("Y values updated for function {}, function saved as whole (not point by point)", functionId);
        return function;
    }

    public FunctionEntity insertPoint(Long functionId, double x, double y) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        
        FunctionEntity function = functionRepository.findById(functionId)
                .orElseThrow(() -> new IllegalArgumentException("Функция не найдена"));
        
        // Проверка доступа: администратор может вставлять точки в любую функцию
        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && !function.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет доступа к этой функции");
        }
        
        // Проверяем, есть ли данные в функции
        boolean isEmpty = function.getXValuesJson() == null || function.getYValuesJson() == null ||
                         function.getXValuesJson().trim().isEmpty() || function.getYValuesJson().trim().isEmpty();
        
        double[] newXValues;
        double[] newYValues;
        
        if (isEmpty) {
            // Если функция пустая, создаем первую точку
            logger.info("Function {} is empty, creating first point: x={}, y={}", functionId, x, y);
            newXValues = new double[]{x};
            newYValues = new double[]{y};
        } else {
            // Получаем данные функции из JSON (не поточечно!)
            double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
            double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            
            // Если функция содержит только одну точку, просто добавляем новую
            if (xValues.length == 1) {
                newXValues = new double[]{xValues[0], x};
                newYValues = new double[]{yValues[0], y};
            } else {
                // Создаем TabulatedFunction для функций с двумя и более точками
                TabulatedFunctionFactory factory = "LINKED_LIST".equals(function.getExpression()) || 
                                                  "LinkedListTabulatedFunction".equals(function.getExpression())
                        ? new LinkedListTabulatedFunctionFactory()
                        : new ArrayTabulatedFunctionFactory();
                
                TabulatedFunction tabFunc = factory.create(xValues, yValues);
                
                // Проверяем что функция поддерживает Insertable
                if (!(tabFunc instanceof Insertable)) {
                    throw new IllegalArgumentException("Функция не поддерживает вставку точек");
                }
                
                // Вставляем точку
                ((Insertable) tabFunc).insert(x, y);
                
                // Извлекаем обновленные массивы
                newXValues = new double[tabFunc.getCount()];
                newYValues = new double[tabFunc.getCount()];
                int i = 0;
                for (Point point : tabFunc) {
                    if (Double.isNaN(point.x) || Double.isInfinite(point.x) ||
                        Double.isNaN(point.y) || Double.isInfinite(point.y)) {
                        logger.warn("Skipping point with invalid values: x={}, y={}", point.x, point.y);
                        continue;
                    }
                    newXValues[i] = point.x;
                    newYValues[i] = point.y;
                    i++;
                }
                
                // Обрезаем массивы до реального размера (если были пропущены точки)
                if (i < newXValues.length) {
                    double[] trimmedX = new double[i];
                    double[] trimmedY = new double[i];
                    System.arraycopy(newXValues, 0, trimmedX, 0, i);
                    System.arraycopy(newYValues, 0, trimmedY, 0, i);
                    newXValues = trimmedX;
                    newYValues = trimmedY;
                }
            }
        }
        
        // Сохраняем обновленную функцию целиком как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(newXValues, newYValues);
        function.setXValuesJson(jsonData[0]);
        function.setYValuesJson(jsonData[1]);
        function.setCount(newXValues.length);
        functionRepository.save(function);
        
        logger.info("Point inserted into function {}, function saved as whole (not point by point)", functionId);
        return function;
    }

    public FunctionEntity removePoint(Long functionId, int index) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }
        
        FunctionEntity function = functionRepository.findById(functionId)
                .orElseThrow(() -> new IllegalArgumentException("Функция не найдена"));
        
        // Проверка доступа: администратор может удалять точки из любой функции
        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && !function.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Нет доступа к этой функции");
        }
        
        // Получаем данные функции из JSON (не поточечно!)
        if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
            function.getXValuesJson().trim().isEmpty() || function.getYValuesJson().trim().isEmpty()) {
            throw new IllegalArgumentException("Функция не содержит данных (xValues или yValues пусты)");
        }
        
        double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
        double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
        
        if (xValues.length == 0) {
            throw new IllegalArgumentException("Функция не содержит точек");
        }
        
        if (index < 0 || index >= xValues.length) {
            throw new IllegalArgumentException("Некорректный индекс: " + index + " (допустимый диапазон: 0-" + (xValues.length - 1) + ")");
        }
        
        // Если останется только одна точка после удаления, просто очищаем функцию
        if (xValues.length == 1) {
            function.setXValuesJson("[]");
            function.setYValuesJson("[]");
            function.setCount(0);
            functionRepository.save(function);
            logger.info("Last point removed from function {}, function is now empty", functionId);
            return function;
        }
        
        // Если останется только одна точка после удаления (т.е. сейчас 2 точки), используем простой подход
        if (xValues.length == 2) {
            // Удаляем точку по индексу, оставляя только одну
            double[] newXValues = new double[1];
            double[] newYValues = new double[1];
            int newIndex = index == 0 ? 1 : 0;
            newXValues[0] = xValues[newIndex];
            newYValues[0] = yValues[newIndex];
            
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(newXValues, newYValues);
            function.setXValuesJson(jsonData[0]);
            function.setYValuesJson(jsonData[1]);
            function.setCount(1);
            functionRepository.save(function);
            logger.info("Point removed from function {}, function now has 1 point", functionId);
            return function;
        }
        
        // Создаем TabulatedFunction
        TabulatedFunctionFactory factory = "LINKED_LIST".equals(function.getExpression()) || 
                                          "LinkedListTabulatedFunction".equals(function.getExpression())
                ? new LinkedListTabulatedFunctionFactory()
                : new ArrayTabulatedFunctionFactory();
        
        TabulatedFunction tabFunc = factory.create(xValues, yValues);
        
        // Проверяем что функция поддерживает Removable
        if (!(tabFunc instanceof Removable)) {
            throw new IllegalArgumentException("Функция не поддерживает удаление точек");
        }
        
        // Удаляем точку по индексу
        ((Removable) tabFunc).remove(index);
        
        // Извлекаем обновленные массивы
        double[] newXValues = new double[tabFunc.getCount()];
        double[] newYValues = new double[tabFunc.getCount()];
        int i = 0;
        for (Point point : tabFunc) {
            if (Double.isNaN(point.x) || Double.isInfinite(point.x) ||
                Double.isNaN(point.y) || Double.isInfinite(point.y)) {
                logger.warn("Skipping point with invalid values: x={}, y={}", point.x, point.y);
                continue;
            }
            newXValues[i] = point.x;
            newYValues[i] = point.y;
            i++;
        }
        
        // Обрезаем массивы до реального размера (если были пропущены точки)
        if (i < newXValues.length) {
            double[] trimmedX = new double[i];
            double[] trimmedY = new double[i];
            System.arraycopy(newXValues, 0, trimmedX, 0, i);
            System.arraycopy(newYValues, 0, trimmedY, 0, i);
            newXValues = trimmedX;
            newYValues = trimmedY;
        }
        
        // Сохраняем обновленную функцию целиком как JSON (не поточечно!)
        String[] jsonData = FunctionDataSerializer.serializeFunctionData(newXValues, newYValues);
        function.setXValuesJson(jsonData[0]);
        function.setYValuesJson(jsonData[1]);
        function.setCount(newXValues.length);
        functionRepository.save(function);
        
        logger.info("Point removed from function {}, function saved as whole (not point by point)", functionId);
        return function;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUserDetails details) {
            return details.getId();
        }
        return null;
    }
    
    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}



