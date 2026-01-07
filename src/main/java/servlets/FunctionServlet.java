package servlets;

import DTO.Function;
import DTO.Point;
import DTO.User;
import JDBC.repository.FunctionRepository;
import JDBC.repository.PointRepository;
import JDBC.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import functions.*;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import operations.TabulatedFunctionOperationService;
import util.FunctionDataSerializer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class FunctionServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(FunctionServlet.class);
    private FunctionRepository functionRepository;
    private UserRepository userRepository;
    private PointRepository pointRepository;
    private ObjectMapper objectMapper;
    
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

    @Override
    public void init() {
        this.functionRepository = new FunctionRepository();
        this.userRepository = new UserRepository();
        this.pointRepository = new PointRepository();
        this.objectMapper = new ObjectMapper();
    }
    // формирование тела (в формате JSON)
    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        PrintWriter out = resp.getWriter();
        out.print(objectMapper.writeValueAsString(error));
        out.flush();
    }
    // аутентификация
    private User authenticate(HttpServletRequest req) {
        return ServletHelper.authenticateUser(req, userRepository);
    }
    
    // Вспомогательный метод для создания расширенного JSON ответа с точками
    // СТРОГАЯ ЛОГИКА: табулированные функции должны быть сохранены в JSON полях
    // Компонентные функции (метаданные) могут не иметь JSON полей - это нормально
    private ObjectNode functionToJsonWithPoints(Function function) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("id", function.getId());
        json.put("name", function.getName());
        json.put("expression", function.getExpression());
        json.put("userId", function.getUserId());
        
        // Проверяем, является ли функция компонентной (метаданные без точек)
        boolean isComposite = function.getExpression() != null && function.getExpression().contains(" ∘ ");
        boolean hasJsonData = function.getXValuesJson() != null && function.getYValuesJson() != null &&
                            !function.getXValuesJson().isEmpty() && !function.getYValuesJson().isEmpty();
        
        double[] xValues;
        double[] yValues;
        List<Point> points = new ArrayList<>();
        
        if (isComposite && !hasJsonData) {
            // Компонентная функция без точек (метаданные) - возвращаем пустые массивы
            xValues = new double[0];
            yValues = new double[0];
            logger.debug("Function {} is composite (metadata only), returning empty arrays", function.getId());
        } else if (hasJsonData) {
            // Табулированная функция с JSON данными - десериализуем
            try {
                xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
                yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
                
                if (xValues.length != yValues.length) {
                    logger.error("Function {} has inconsistent array lengths: xValues={}, yValues={}", 
                            function.getId(), xValues.length, yValues.length);
                    throw new IllegalStateException("Function " + function.getId() + " has inconsistent data.");
                }
                
                // Создаем список точек из массивов
                for (int i = 0; i < xValues.length; i++) {
                    Point p = new Point(function.getId(), xValues[i], yValues[i]);
                    points.add(p);
                }
            } catch (Exception e) {
                logger.error("Error deserializing JSON fields for function {}: {}", function.getId(), e.getMessage(), e);
                throw new IllegalStateException("Function " + function.getId() + " has invalid JSON data: " + e.getMessage());
            }
        } else {
            // Табулированная функция БЕЗ JSON данных - возвращаем пустые массивы и логируем предупреждение
            // Это может произойти для старых функций, созданных до внедрения JSON полей
            logger.warn("Function {} is not composite but does not have JSON fields - returning empty arrays. Function may need to be recreated.", function.getId());
            xValues = new double[0];
            yValues = new double[0];
        }
        
        // Добавляем массивы
        com.fasterxml.jackson.databind.node.ArrayNode xArray = objectMapper.createArrayNode();
        com.fasterxml.jackson.databind.node.ArrayNode yArray = objectMapper.createArrayNode();
        for (double x : xValues) {
            xArray.add(x);
        }
        for (double y : yValues) {
            yArray.add(y);
        }
        json.set("xValues", xArray);
        json.set("yValues", yArray);
        
        // Добавляем точки
        com.fasterxml.jackson.databind.node.ArrayNode pointsArray = objectMapper.createArrayNode();
        for (Point p : points) {
            ObjectNode pointNode = objectMapper.createObjectNode();
            pointNode.put("functionId", p.getFunctionId());
            pointNode.put("xValue", p.getXValue());
            pointNode.put("yValue", p.getYValue());
            pointsArray.add(pointNode);
        }
        json.set("points", pointsArray);
        
        // Добавляем метаданные
        int pointCount = function.getCount() != null ? function.getCount() : xValues.length;
        json.put("count", pointCount);
        json.put("isInsertable", pointCount >= 2);
        json.put("isRemovable", pointCount >= 2);
        
        return json;
    }
    
    // Вспомогательный метод для создания списка функций с точками
    private com.fasterxml.jackson.databind.node.ArrayNode functionsListToJsonWithPoints(List<Function> functions) throws IOException {
        com.fasterxml.jackson.databind.node.ArrayNode array = objectMapper.createArrayNode();
        for (Function f : functions) {
            try {
                array.add(functionToJsonWithPoints(f));
            } catch (Exception e) {
                logger.error("Error processing function {}: {}", f.getId(), e.getMessage(), e);
                // Пропускаем проблемную функцию и продолжаем обработку остальных
            }
        }
        return array;
    }

    @Override
    // (GET /api/functions)
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) { // проверка аутентификации
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }

        // /api/functions — получить все функции (доступ - ADMIN)
        if (pathInfo == null || "/".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<Function> functions = functionRepository.findAll();
            resp.setContentType("application/json");
            com.fasterxml.jackson.databind.node.ArrayNode jsonArray = functionsListToJsonWithPoints(functions);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonArray));
            logger.info("Отправлены все функции");
            return;
        }
        // убираем начальный '/'
        pathInfo = pathInfo.substring(1);

        // /api/functions/sorted - получить отсортированные функции
        if ("sorted".equals(pathInfo)) {
            if (!"ADMIN".equals(authUser.getRole())) { // (доступ - ADMIN)
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN"); // 403
                return;
            }
            List<Function> functions = functionRepository.findAllSortedByName();
            resp.setContentType("application/json");
            com.fasterxml.jackson.databind.node.ArrayNode jsonArray = functionsListToJsonWithPoints(functions);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonArray));
            logger.info("Отправлены отсортированные функции");
            return;
        }

        // /api/functions/user/{userId} - получить все функции пользователя
        if (pathInfo.startsWith("user/")) { // (доступ ADMIN или пользователь)
            String rest = pathInfo.substring("user/".length());
            
            // /api/functions/user/{userId}/search - поиск функций с фильтрацией
            if (rest.contains("/search")) {
                String userIdStr = rest.substring(0, rest.indexOf("/search"));
                try {
                    int userId = Integer.parseInt(userIdStr);
                    if (userId != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                        sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                        return;
                    }
                    
                    // Получаем параметры запроса
                    String nameParam = req.getParameter("name");
                    String sortBy = req.getParameter("sortBy");
                    if (sortBy == null || sortBy.isEmpty()) {
                        sortBy = "id";
                    }
                    String sortDir = req.getParameter("sortDir");
                    if (sortDir == null || sortDir.isEmpty()) {
                        sortDir = "asc";
                    }
                    
                    // Получаем все функции пользователя
                    List<Function> functions = functionRepository.findByUserId(userId);
                    
                    // Фильтрация по имени (если указано)
                    if (nameParam != null && !nameParam.trim().isEmpty()) {
                        String nameFilter = nameParam.toLowerCase();
                        functions = functions.stream()
                            .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(nameFilter))
                            .collect(java.util.stream.Collectors.toList());
                    }
                    
                    // Сортировка
                    final String finalSortBy = sortBy;
                    final boolean ascending = !"desc".equalsIgnoreCase(sortDir);
                    functions.sort((f1, f2) -> {
                        int result = 0;
                        switch (finalSortBy) {
                            case "name":
                                String n1 = f1.getName() != null ? f1.getName() : "";
                                String n2 = f2.getName() != null ? f2.getName() : "";
                                result = n1.compareToIgnoreCase(n2);
                                break;
                            case "id":
                            default:
                                result = Integer.compare(f1.getId() != null ? f1.getId() : 0, 
                                                       f2.getId() != null ? f2.getId() : 0);
                                break;
                        }
                        return ascending ? result : -result;
                    });
                    
                    resp.setContentType("application/json");
                    com.fasterxml.jackson.databind.node.ArrayNode jsonArray = functionsListToJsonWithPoints(functions);
                    resp.getWriter().print(objectMapper.writeValueAsString(jsonArray));
                    logger.info("Отправлены отфильтрованные функции пользователя {}", userId);
                    return;
                } catch (NumberFormatException e) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId"); // 400
                    return;
                }
            }
            
            // Обычный запрос функций пользователя
            try {
                int userId = Integer.parseInt(rest);
                if (userId != authUser.getId() && !"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                    return;
                }
                List<Function> functions = functionRepository.findByUserId(userId);
                resp.setContentType("application/json");
                com.fasterxml.jackson.databind.node.ArrayNode jsonArray = functionsListToJsonWithPoints(functions);
                resp.getWriter().print(objectMapper.writeValueAsString(jsonArray));
                logger.info("Отправлены функции пользователя {}", userId);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId"); // 400
                return;
            }
        }
        
        // /api/functions/available-math-functions - список доступных математических функций
        if ("available-math-functions".equals(pathInfo)) {
            List<String> mathFunctions = new ArrayList<>();
            mathFunctions.add("SqrFunction");
            mathFunctions.add("IdentityFunction");
            mathFunctions.add("UnitFunction");
            mathFunctions.add("ZeroFunction");
            mathFunctions.add("ConstantFunction");
            mathFunctions.add("SinFunction");
            mathFunctions.add("CosFunction");
            mathFunctions.add("LnFunction");
            mathFunctions.add("ExpFunction");
            
            // Добавляем компонентные функции пользователя
            // Компонентная функция может иметь точки (табулированная) или не иметь (метаданные)
            // Главное - выражение должно содержать " ∘ "
            List<Function> userFunctions = functionRepository.findByUserId(authUser.getId());
            for (Function func : userFunctions) {
                if (func.getExpression().contains(" ∘ ")) {
                    // Это компонентная функция - добавляем в список математических функций
                    // Независимо от того, есть ли у неё точки или нет
                    mathFunctions.add(func.getName());
                }
            }
            
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(mathFunctions));
            logger.info("Отправлен список доступных математических функций");
            return;
        }
        // /api/functions/{id} - получить функцию по id
        // (доступ - ADMIN или владелец)
        try {
            int id = Integer.parseInt(pathInfo);
            Function f = functionRepository.findById(id);
            if (f == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                return;
            }
            if (!Objects.equals(f.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(f);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Отправлена функция {}", id);
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
        }
    }

    @Override
    // (POST /api/functions) - создание новой функции
    // (доступ - любой авторизированный пользователь)
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        
        // чтение тела запроса
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        String requestBody = sb.toString();
        
        // /api/functions/preview-from-math - предпросмотр функции из математического выражения
        if (pathInfo != null && "/preview-from-math".equals(pathInfo)) {
            handlePreviewFromMath(req, resp, authUser, requestBody);
            return;
        }
        
        // /api/functions/create-from-math - создание функции из математического выражения
        if (pathInfo != null && "/create-from-math".equals(pathInfo)) {
            handleCreateFromMath(req, resp, authUser, requestBody);
            return;
        }
        
        // /api/functions/create-from-arrays - создание функции из массивов
        if (pathInfo != null && "/create-from-arrays".equals(pathInfo)) {
            handleCreateFromArrays(req, resp, authUser, requestBody);
            return;
        }
        
        // /api/functions/operate - выполнение операций над функциями
        if (pathInfo != null && "/operate".equals(pathInfo)) {
            handleOperate(req, resp, authUser, requestBody);
            return;
        }
        
        // /api/functions/composite - создание композитной функции
        if (pathInfo != null && "/composite".equals(pathInfo)) {
            handleComposite(req, resp, authUser, requestBody);
            return;
        }
        
        // /api/functions/{id}/insert-point - вставка точки в функцию
        if (pathInfo != null && pathInfo.matches("/\\d+/insert-point")) {
            String idStr = pathInfo.substring(1, pathInfo.indexOf("/insert-point"));
            try {
                int functionId = Integer.parseInt(idStr);
                handleInsertPoint(req, resp, authUser, functionId, requestBody);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
                return;
            }
        }
        
        // Обычное создание функции (POST /api/functions)
        // ВАЖНО: Этот эндпоинт создает только метаданные функции без точек
        // Для создания функции с точками используйте /api/functions/create-from-math или /api/functions/create-from-arrays
        if (pathInfo == null || "/".equals(pathInfo)) {
            try {
                Function f = objectMapper.readValue(requestBody, Function.class);
                if (f.getName() == null || f.getName().trim().isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function name is required"); // 400
                    return;
                }
                if (f.getExpression() == null || f.getExpression().trim().isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Expression is required"); // 400
                    return;
                }
                
                // Проверяем, является ли функция composite (композитная функция может не иметь точек)
                boolean isComposite = f.getExpression() != null && f.getExpression().contains(" ∘ ");
                
                // Если функция не composite и не имеет JSON полей, это ошибка
                // Для создания табулированной функции используйте специальные эндпоинты
                if (!isComposite && (f.getXValuesJson() == null || f.getYValuesJson() == null || 
                    f.getXValuesJson().isEmpty() || f.getYValuesJson().isEmpty())) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        "Tabulated functions must be created with points. " +
                        "Use /api/functions/create-from-math or /api/functions/create-from-arrays endpoints, " +
                        "or provide xValuesJson and yValuesJson fields."); // 400
                    return;
                }
                
                f.setUserId(authUser.getId()); // текущий пользователь
                Integer id = functionRepository.insert(f);
                if (id != null) {
                    f.setId(id);
                    resp.setStatus(HttpServletResponse.SC_CREATED); // 201
                    resp.setContentType("application/json");
                    ObjectNode jsonResponse = functionToJsonWithPoints(f);
                    resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
                    logger.info("Функция создана (composite или с JSON полями)");
                } else {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Insert failed"); // 500
                }
            } catch (JsonProcessingException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format"); // 400
                logger.warn("Некорректный JSON при создании функции: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Ошибка при создании функции", e);
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error"); // 500
            }
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found"); // 404
        }
    }

    @Override
    // (PUT /api/functions/{id}) - обновление функции (доступ - владелец или ADMIN)
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function ID required"); // 400
            return;
        }
        
        // /api/functions/{id}/y-values - обновление Y значений
        if (pathInfo.matches("/\\d+/y-values")) {
            String idStr = pathInfo.substring(1, pathInfo.indexOf("/y-values"));
            try {
                int functionId = Integer.parseInt(idStr);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = req.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
                handleUpdateYValues(req, resp, authUser, functionId, sb.toString());
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID");
                return;
            }
        }
        
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Function existing = functionRepository.findById(id);
            if (existing == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 404
                return;
            }
            // либо владелец, либо ADMIN
            if (!Objects.equals(existing.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            // чтение тела запроса
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            ObjectNode update = (ObjectNode) objectMapper.readTree(sb.toString());
            Integer requestedUserId = null;
            if (update.has("userId") && !update.get("userId").isNull()) {
                if (!update.get("userId").canConvertToInt()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId"); // 400
                    return;
                }
                requestedUserId = update.get("userId").asInt();
            }
            // Определяем, какой userId будет у функции после обновления (для проверки уникальности)
            Integer targetUserId = existing.getUserId();
            if (requestedUserId != null && !Objects.equals(existing.getUserId(), requestedUserId)) {
                if (!"ADMIN".equals(authUser.getRole())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only ADMIN can change userId"); // 403
                    return;
                }
                User newOwner = userRepository.findById(requestedUserId);
                if (newOwner == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found"); // 404
                    return;
                }
                targetUserId = requestedUserId;
            }
            
            // обновление полей
            if (update.has("name") && !update.get("name").isNull()) {
                String newName = update.get("name").asText().trim();
                if (newName.isEmpty()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function name cannot be empty"); // 400
                    return;
                }
                
                // Проверяем уникальность только если имя действительно изменилось
                if (!newName.equalsIgnoreCase(existing.getName())) {
                    // Проверка уникальности имени среди функций целевого пользователя (исключая текущую редактируемую функцию)
                    List<Function> userFunctions = functionRepository.findByUserId(targetUserId);
                    for (Function func : userFunctions) {
                        if (!func.getId().equals(id) && func.getName().equalsIgnoreCase(newName)) {
                            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                                    "Функция с именем '" + newName + "' уже существует");
                            return;
                        }
                    }
                }
                
                existing.setName(newName);
            }
            if (update.has("expression") && !update.get("expression").isNull()) {
                existing.setExpression(update.get("expression").asText());
            }
            if (requestedUserId != null && !Objects.equals(existing.getUserId(), requestedUserId)) {
                existing.setUserId(requestedUserId);
            }
            if (functionRepository.update(existing)) {
                Function updated = functionRepository.findById(id);
                if (updated == null) {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load updated function"); // 500
                    return;
                }
                resp.setContentType("application/json");
                ObjectNode jsonResponse = functionToJsonWithPoints(updated);
                resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
                logger.info("Обновлена функция {}", id);
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update failed"); // 500
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
        }
    }

    @Override
    // (DELETE /api/functions/{id}) - удаление функции
    // доступ - владелец или ADMIN
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        User authUser = authenticate(req);
        if (authUser == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
            return;
        }
        if (pathInfo == null || "/".equals(pathInfo)) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function ID required"); // 400
            return;
        }
        
        // /api/functions/{id}/remove-point/{index} - удаление точки по индексу
        if (pathInfo.matches("/\\d+/remove-point/\\d+")) {
            String[] parts = pathInfo.substring(1).split("/remove-point/");
            try {
                int functionId = Integer.parseInt(parts[0]);
                int index = Integer.parseInt(parts[1]);
                handleRemovePoint(req, resp, authUser, functionId, index);
                return;
            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID or index");
                return;
            }
        }
        
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Function f = functionRepository.findById(id);
            if (f == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found"); // 400
                return;
            }
            // либо ADMIN, либо владелец
            if (!Objects.equals(f.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied"); // 403
                return;
            }
            // Удаляем функцию (точки хранятся в JSON полях, не нужно удалять отдельно)
            if (functionRepository.delete(id)) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
                logger.info("Удалена функция {}", id);
            } else {
                // Если функция не найдена или уже удалена, возвращаем 404, а не 500
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found or already deleted"); // 404
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function ID"); // 400
        }
    }
    
    // Вспомогательные методы для обработки новых endpoints
    
    private void handlePreviewFromMath(HttpServletRequest req, HttpServletResponse resp, User authUser, String requestBody) throws IOException {
        try {
            logger.info("handlePreviewFromMath: Received request body length = {}", requestBody != null ? requestBody.length() : 0);
            logger.debug("handlePreviewFromMath: requestBody = {}", requestBody);
            
            if (requestBody == null || requestBody.trim().isEmpty()) {
                logger.error("handlePreviewFromMath: Request body is null or empty");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body is required");
                return;
            }
            
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(requestBody);
                logger.debug("handlePreviewFromMath: JSON parsed successfully");
            } catch (Exception e) {
                logger.error("handlePreviewFromMath: Failed to parse JSON: {}", e.getMessage(), e);
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format: " + e.getMessage());
                return;
            }
            
            logger.debug("handlePreviewFromMath: Available JSON fields: {}", jsonNode.fieldNames());
            
            String mathFunctionType = null;
            if (jsonNode.has("mathFunctionType")) {
                JsonNode mathFunctionTypeNode = jsonNode.get("mathFunctionType");
                logger.debug("handlePreviewFromMath: mathFunctionType node exists, isNull = {}, type = {}", 
                        mathFunctionTypeNode.isNull(), mathFunctionTypeNode.getNodeType());
                if (!mathFunctionTypeNode.isNull()) {
                    mathFunctionType = mathFunctionTypeNode.asText();
                    logger.debug("handlePreviewFromMath: mathFunctionType extracted = '{}'", mathFunctionType);
                    if (mathFunctionType != null && mathFunctionType.trim().isEmpty()) {
                        logger.warn("handlePreviewFromMath: mathFunctionType is empty string, setting to null");
                        mathFunctionType = null;
                    }
                } else {
                    logger.warn("handlePreviewFromMath: mathFunctionType node is null");
                }
            } else {
                logger.warn("handlePreviewFromMath: mathFunctionType field is missing from JSON");
            }
            
            double xFrom = jsonNode.has("xFrom") && !jsonNode.get("xFrom").isNull() ? jsonNode.get("xFrom").asDouble() : 0.0;
            double xTo = jsonNode.has("xTo") && !jsonNode.get("xTo").isNull() ? jsonNode.get("xTo").asDouble() : 1.0;
            int count = jsonNode.has("count") && !jsonNode.get("count").isNull() ? jsonNode.get("count").asInt() : 10;
            String name = jsonNode.has("name") && !jsonNode.get("name").isNull() ? jsonNode.get("name").asText() : "preview";
            
            logger.info("handlePreviewFromMath: Parsed values - name = '{}', mathFunctionType = '{}', xFrom = {}, xTo = {}, count = {}", 
                    name, mathFunctionType, xFrom, xTo, count);
            
            if (mathFunctionType == null || mathFunctionType.trim().isEmpty()) {
                logger.error("handlePreviewFromMath: mathFunctionType is missing or empty. Available fields: {}", jsonNode.fieldNames());
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "mathFunctionType is required");
                return;
            }
            
            MathFunction mathFunction = null;
            
            // Сначала проверяем стандартные математические функции
            if (MATH_FUNCTIONS.containsKey(mathFunctionType)) {
                mathFunction = MATH_FUNCTIONS.get(mathFunctionType);
            } else {
                // Если не найдено в стандартных функциях, проверяем, является ли это компонентной функцией пользователя
                List<Function> userFunctions = functionRepository.findByUserId(authUser.getId());
                for (Function func : userFunctions) {
                    if (func.getName().equals(mathFunctionType) && func.getExpression() != null && func.getExpression().contains(" ∘ ")) {
                        // Это компонентная функция (может иметь точки или не иметь)
                        // Рекурсивно строим композицию из выражения
                        mathFunction = buildCompositeFunctionFromExpression(func.getExpression(), authUser.getId());
                        if (mathFunction != null) {
                            break;
                        }
                    }
                }
            }
            
            if (mathFunction == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown math function type: " + mathFunctionType);
                return;
            }
            
            // Создаем массивы x и y вручную
            double[] xValues = new double[count];
            double[] yValues = new double[count];
            double step = (xTo - xFrom) / (count > 1 ? (count - 1) : 1);
            
            for (int i = 0; i < count; i++) {
                xValues[i] = xFrom + i * step;
                yValues[i] = mathFunction.apply(xValues[i]);
            }
            
            // Извлекаем точки
            List<Point> points = new ArrayList<>();
            for (int i = 0; i < xValues.length; i++) {
                if (!Double.isNaN(xValues[i]) && !Double.isInfinite(xValues[i]) &&
                    !Double.isNaN(yValues[i]) && !Double.isInfinite(yValues[i])) {
                    points.add(new Point(null, xValues[i], yValues[i]));
                }
            }
            
            // Создаем временную функцию для предпросмотра (НЕ сохраняем в БД)
            Function previewFunction = new Function();
            previewFunction.setName(name);
            previewFunction.setExpression(mathFunctionType + "[" + xFrom + "," + xTo + "," + count + "]");
            previewFunction.setUserId(authUser.getId());
            
            // Создаем DTO с точками для ответа
            ObjectNode response = objectMapper.createObjectNode();
            response.put("id", 0); // Временный ID для предпросмотра
            response.put("name", previewFunction.getName());
            response.put("expression", previewFunction.getExpression());
            response.put("userId", previewFunction.getUserId());
            
            // Создаем массивы x и y
            com.fasterxml.jackson.databind.node.ArrayNode xArray = objectMapper.createArrayNode();
            com.fasterxml.jackson.databind.node.ArrayNode yArray = objectMapper.createArrayNode();
            com.fasterxml.jackson.databind.node.ArrayNode pointsArray = objectMapper.createArrayNode();
            
            for (Point p : points) {
                xArray.add(p.getXValue());
                yArray.add(p.getYValue());
                
                ObjectNode pointNode = objectMapper.createObjectNode();
                pointNode.put("functionId", 0);
                pointNode.put("xValue", p.getXValue());
                pointNode.put("yValue", p.getYValue());
                pointsArray.add(pointNode);
            }
            
            response.set("xValues", xArray);
            response.set("yValues", yArray);
            response.set("points", pointsArray);
            response.put("count", points.size());
            response.put("isInsertable", points.size() >= 2);
            response.put("isRemovable", points.size() >= 2);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(response));
            logger.info("Function preview created successfully");
        } catch (Exception e) {
            logger.error("Error creating function preview: {}", e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleCreateFromMath(HttpServletRequest req, HttpServletResponse resp, User authUser, String requestBody) throws IOException {
        try {
            logger.info("handleCreateFromMath: Received request body length = {}", requestBody != null ? requestBody.length() : 0);
            logger.debug("handleCreateFromMath: requestBody = {}", requestBody);
            
            if (requestBody == null || requestBody.trim().isEmpty()) {
                logger.error("handleCreateFromMath: Request body is null or empty");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body is required");
                return;
            }
            
            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(requestBody);
                logger.debug("handleCreateFromMath: JSON parsed successfully");
            } catch (Exception e) {
                logger.error("handleCreateFromMath: Failed to parse JSON: {}", e.getMessage(), e);
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format: " + e.getMessage());
                return;
            }
            
            // Log all available fields in JSON
            logger.debug("handleCreateFromMath: Available JSON fields: {}", jsonNode.fieldNames());
            
            String name = null;
            if (jsonNode.has("name")) {
                JsonNode nameNode = jsonNode.get("name");
                if (!nameNode.isNull()) {
                    name = nameNode.asText();
                }
            }
            
            String mathFunctionType = null;
            if (jsonNode.has("mathFunctionType")) {
                JsonNode mathFunctionTypeNode = jsonNode.get("mathFunctionType");
                logger.debug("handleCreateFromMath: mathFunctionType node exists, isNull = {}, type = {}", 
                        mathFunctionTypeNode.isNull(), mathFunctionTypeNode.getNodeType());
                if (!mathFunctionTypeNode.isNull()) {
                    mathFunctionType = mathFunctionTypeNode.asText();
                    logger.debug("handleCreateFromMath: mathFunctionType extracted = '{}'", mathFunctionType);
                    if (mathFunctionType != null && mathFunctionType.trim().isEmpty()) {
                        logger.warn("handleCreateFromMath: mathFunctionType is empty string, setting to null");
                        mathFunctionType = null;
                    }
                } else {
                    logger.warn("handleCreateFromMath: mathFunctionType node is null");
                }
            } else {
                logger.warn("handleCreateFromMath: mathFunctionType field is missing from JSON");
            }
            
            double xFrom = jsonNode.has("xFrom") && !jsonNode.get("xFrom").isNull() ? jsonNode.get("xFrom").asDouble() : 0.0;
            double xTo = jsonNode.has("xTo") && !jsonNode.get("xTo").isNull() ? jsonNode.get("xTo").asDouble() : 1.0;
            int count = jsonNode.has("count") && !jsonNode.get("count").isNull() ? jsonNode.get("count").asInt() : 10;
            
            logger.info("handleCreateFromMath: Parsed values - name = '{}', mathFunctionType = '{}', xFrom = {}, xTo = {}, count = {}", 
                    name, mathFunctionType, xFrom, xTo, count);
            
            if (name == null || name.trim().isEmpty()) {
                logger.error("handleCreateFromMath: Function name is missing or empty");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function name is required");
                return;
            }
            
            if (mathFunctionType == null || mathFunctionType.trim().isEmpty()) {
                logger.error("handleCreateFromMath: mathFunctionType is missing or empty. Available fields: {}", jsonNode.fieldNames());
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "mathFunctionType is required");
                return;
            }
            
            MathFunction mathFunction = null;
            
            // Сначала проверяем стандартные математические функции
            if (MATH_FUNCTIONS.containsKey(mathFunctionType)) {
                mathFunction = MATH_FUNCTIONS.get(mathFunctionType);
            } else {
                // Если не найдено в стандартных функциях, проверяем, является ли это компонентной функцией пользователя
                List<Function> userFunctions = functionRepository.findByUserId(authUser.getId());
                for (Function func : userFunctions) {
                    if (func.getName().equals(mathFunctionType) && func.getExpression() != null && func.getExpression().contains(" ∘ ")) {
                        // Это компонентная функция, рекурсивно строим композицию
                        mathFunction = buildCompositeFunctionFromExpression(func.getExpression(), authUser.getId());
                        if (mathFunction != null) {
                            break;
                        }
                    }
                }
            }
            
            if (mathFunction == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown math function type: " + mathFunctionType);
                return;
            }
            
            // Создаем массивы x и y вручную
            double[] xValues = new double[count];
            double[] yValues = new double[count];
            double step = (xTo - xFrom) / (count > 1 ? (count - 1) : 1);
            
            for (int i = 0; i < count; i++) {
                xValues[i] = xFrom + i * step;
                yValues[i] = mathFunction.apply(xValues[i]);
            }
            
            // Проверяем уникальность имени функции для пользователя
            // Если функция с таким именем уже существует:
            // - Если это компонентная функция (с точками или без) - обновляем её
            // - Если это математическая функция (expression содержит [xFrom,xTo,count]) - обновляем её (редактирование)
            // - Если это табулированная функция (ARRAY/LINKED_LIST) - ошибка (нельзя перезаписать табулированную математической)
            List<Function> existingFunctions = functionRepository.findByUserId(authUser.getId());
            Function existingFunctionToUpdate = null;
            for (Function func : existingFunctions) {
                if (func.getName().equalsIgnoreCase(name)) {
                    // Проверяем JSON поля для определения типа функции
                    boolean hasJsonData = func.getXValuesJson() != null && func.getYValuesJson() != null &&
                                        !func.getXValuesJson().isEmpty() && !func.getYValuesJson().isEmpty();
                    
                    // Сначала проверяем, является ли это компонентной функцией (может иметь точки или не иметь)
                    boolean isComposite = func.getExpression() != null && func.getExpression().contains(" ∘ ");
                    
                    if (isComposite) {
                        // Это компонентная функция - обновляем её (независимо от наличия точек)
                        existingFunctionToUpdate = func;
                        break;
                    } else if (!hasJsonData) {
                        // Функция без JSON данных и не компонентная - это старая некорректная функция, обновляем её
                        existingFunctionToUpdate = func;
                        break;
                    } else if (hasJsonData) {
                        // Проверяем, является ли это математической функцией (expression содержит [xFrom,xTo,count])
                        String expression = func.getExpression();
                        if (expression != null && expression.matches(".*\\[.*,.*,.*\\]")) {
                            // Это математическая функция - обновляем её (редактирование)
                            existingFunctionToUpdate = func;
                            break;
                        } else {
                            // Это табулированная функция (ARRAY/LINKED_LIST) - нельзя перезаписать
                            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Функция с именем '" + name + "' уже существует");
                            return;
                        }
                    }
                }
            }
            
            // Если нашли существующую функцию для обновления, обновляем её вместо создания новой
            Integer functionId;
            Function function;
            if (existingFunctionToUpdate != null) {
                // Обновляем существующую функцию
                functionId = existingFunctionToUpdate.getId();
                function = existingFunctionToUpdate;
                
                // Если это компонентная функция, НЕ обновляем выражение
                // Оставляем исходное выражение "outer ∘ inner" для компонентных функций
                // Компонентная функция может иметь точки (табулированная) или не иметь (метаданные)
                if (existingFunctionToUpdate.getExpression() == null || !existingFunctionToUpdate.getExpression().contains(" ∘ ")) {
                    // Для обычных математических функций обновляем expression
                    function.setExpression(mathFunctionType + "[" + xFrom + "," + xTo + "," + count + "]");
                    functionRepository.update(function);
                }
                
                logger.info("Обновлена функция (пересозданы данные): {}", functionId);
            } else {
                // Создаем новую функцию
                function = new Function();
                function.setName(name);
                function.setExpression(mathFunctionType + "[" + xFrom + "," + xTo + "," + count + "]");
                function.setUserId(authUser.getId());
                
                functionId = functionRepository.insert(function);
                if (functionId == null) {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create function");
                    return;
                }
                function.setId(functionId);
            }
            
            // Фильтруем невалидные значения перед сохранением
            List<Double> validX = new ArrayList<>();
            List<Double> validY = new ArrayList<>();
            for (int i = 0; i < xValues.length; i++) {
                if (!Double.isNaN(xValues[i]) && !Double.isInfinite(xValues[i]) &&
                    !Double.isNaN(yValues[i]) && !Double.isInfinite(yValues[i])) {
                    validX.add(xValues[i]);
                    validY.add(yValues[i]);
                }
            }
            
            if (validX.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No valid points after filtering");
                return;
            }
            
            // Преобразуем в массивы
            double[] validXArray = validX.stream().mapToDouble(Double::doubleValue).toArray();
            double[] validYArray = validY.stream().mapToDouble(Double::doubleValue).toArray();
            
            // Сохраняем функцию целиком как JSON (НЕ поточечно!)
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(validXArray, validYArray);
            function.setXValuesJson(jsonData[0]);
            function.setYValuesJson(jsonData[1]);
            function.setCount(validXArray.length);
            
            // Обновляем функцию с JSON данными
            if (existingFunctionToUpdate != null) {
                functionRepository.update(function);
            } else {
                // Для новой функции нужно обновить после insert
                functionRepository.update(function);
            }
            
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(function);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Function created from math expression: {} (saved as whole, not point by point)", functionId);
        } catch (Exception e) {
            logger.error("Error creating function from math: {}", e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleCreateFromArrays(HttpServletRequest req, HttpServletResponse resp, User authUser, String requestBody) throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            String name = jsonNode.has("name") ? jsonNode.get("name").asText() : null;
            String factoryType = jsonNode.has("factoryType") ? jsonNode.get("factoryType").asText() : "ARRAY";
            
            if (name == null || name.trim().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function name is required");
                return;
            }
            
            // Получаем массивы x и y
            JsonNode xValuesNode = jsonNode.get("xValues");
            JsonNode yValuesNode = jsonNode.get("yValues");
            
            if (xValuesNode == null || yValuesNode == null || !xValuesNode.isArray() || !yValuesNode.isArray()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "xValues and yValues arrays are required");
                return;
            }
            
            List<Double> xList = new ArrayList<>();
            List<Double> yList = new ArrayList<>();
            
            for (JsonNode xNode : xValuesNode) {
                xList.add(xNode.asDouble());
            }
            for (JsonNode yNode : yValuesNode) {
                yList.add(yNode.asDouble());
            }
            
            if (xList.size() != yList.size()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "xValues and yValues must have the same length");
                return;
            }
            
            if (xList.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Arrays cannot be empty");
                return;
            }
            
            // Фильтруем невалидные значения
            List<Double> validX = new ArrayList<>();
            List<Double> validY = new ArrayList<>();
            for (int i = 0; i < xList.size(); i++) {
                double x = xList.get(i);
                double y = yList.get(i);
                if (!Double.isNaN(x) && !Double.isInfinite(x) &&
                    !Double.isNaN(y) && !Double.isInfinite(y)) {
                    validX.add(x);
                    validY.add(y);
                }
            }
            
            if (validX.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No valid points after filtering");
                return;
            }
            
            // Преобразуем в массивы
            double[] xValues = validX.stream().mapToDouble(Double::doubleValue).toArray();
            double[] yValues = validY.stream().mapToDouble(Double::doubleValue).toArray();
            
            // Валидация через создание функции
            TabulatedFunctionFactory factory = "LINKED_LIST".equals(factoryType) 
                ? new LinkedListTabulatedFunctionFactory() 
                : new ArrayTabulatedFunctionFactory();
            
            try {
                factory.create(xValues, yValues);
            } catch (Exception e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid function data: " + e.getMessage());
                return;
            }
            
            // Проверка уникальности имени функции для пользователя
            List<Function> existingFunctions = functionRepository.findByUserId(authUser.getId());
            for (Function func : existingFunctions) {
                if (func.getName().equalsIgnoreCase(name)) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Функция с именем '" + name + "' уже существует");
                    return;
                }
            }
            
            // Создаем функцию в БД
            Function function = new Function();
            function.setName(name);
            function.setExpression(factoryType);
            function.setUserId(authUser.getId());
            
            // Сохраняем функцию целиком как JSON (НЕ поточечно!)
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(xValues, yValues);
            function.setXValuesJson(jsonData[0]);
            function.setYValuesJson(jsonData[1]);
            function.setCount(xValues.length);
            
            Integer functionId = functionRepository.insert(function);
            if (functionId == null) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create function");
                return;
            }
            function.setId(functionId);
            
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(function);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Функция создана из массивов: {} (saved as whole, not point by point)", functionId);
        } catch (Exception e) {
            logger.error("Ошибка при создании функции из массивов", e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleOperate(HttpServletRequest req, HttpServletResponse resp, User authUser, String requestBody) throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            int functionId1 = jsonNode.has("functionId1") ? jsonNode.get("functionId1").asInt() : 0;
            int functionId2 = jsonNode.has("functionId2") ? jsonNode.get("functionId2").asInt() : 0;
            String operation = jsonNode.has("operation") ? jsonNode.get("operation").asText() : null;
            String resultName = jsonNode.has("resultName") ? jsonNode.get("resultName").asText() : null;
            String factoryType = jsonNode.has("factoryType") ? jsonNode.get("factoryType").asText() : "ARRAY";
            
            if (functionId1 == 0 || functionId2 == 0) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "functionId1 and functionId2 are required");
                return;
            }
            if (operation == null || resultName == null || resultName.trim().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "operation and resultName are required");
                return;
            }
            
            // Получаем функции
            Function func1 = functionRepository.findById(functionId1);
            Function func2 = functionRepository.findById(functionId2);
            
            if (func1 == null || func2 == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "One or both functions not found");
                return;
            }
            
            // Проверка доступа
            boolean isAdmin = "ADMIN".equals(authUser.getRole());
            if (!isAdmin && (!Objects.equals(func1.getUserId(), authUser.getId()) || 
                            !Objects.equals(func2.getUserId(), authUser.getId()))) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied to functions");
                return;
            }
            
            // СТРОГАЯ ПРОВЕРКА: функции должны быть сохранены в JSON полях
            if (func1.getXValuesJson() == null || func1.getYValuesJson() == null ||
                func1.getXValuesJson().isEmpty() || func1.getYValuesJson().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        "Function " + func1.getId() + " is not saved correctly. Functions must be saved as whole, not point by point.");
                return;
            }
            
            if (func2.getXValuesJson() == null || func2.getYValuesJson() == null ||
                func2.getXValuesJson().isEmpty() || func2.getYValuesJson().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        "Function " + func2.getId() + " is not saved correctly. Functions must be saved as whole, not point by point.");
                return;
            }
            
            // Десериализуем JSON поля
            double[] x1 = FunctionDataSerializer.deserializeArray(func1.getXValuesJson());
            double[] y1 = FunctionDataSerializer.deserializeArray(func1.getYValuesJson());
            double[] x2 = FunctionDataSerializer.deserializeArray(func2.getXValuesJson());
            double[] y2 = FunctionDataSerializer.deserializeArray(func2.getYValuesJson());
            
            if (x1.length == 0 || x2.length == 0) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Functions must have points");
                return;
            }
            
            if (x1.length != x2.length) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Functions must have the same number of points");
                return;
            }
            
            // Проверяем совпадение X значений (массивы уже должны быть отсортированы при сохранении)
            for (int i = 0; i < x1.length; i++) {
                if (Math.abs(x1[i] - x2[i]) > 1e-10) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Functions have different X values");
                    return;
                }
            }
            
            // Создаем табулированные функции
            TabulatedFunctionFactory factory = "LINKED_LIST".equals(factoryType) 
                ? new LinkedListTabulatedFunctionFactory() 
                : new ArrayTabulatedFunctionFactory();
            
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
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown operation: " + operation);
                    return;
            }
            
            // Проверка уникальности имени функции результата для пользователя
            List<Function> existingFunctions = functionRepository.findByUserId(authUser.getId());
            for (Function func : existingFunctions) {
                if (func.getName().equalsIgnoreCase(resultName)) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Функция с именем '" + resultName + "' уже существует");
                    return;
                }
            }
            
            // Извлекаем массивы x и y из результата
            List<Double> resultXList = new ArrayList<>();
            List<Double> resultYList = new ArrayList<>();
            for (functions.Point point : result) {
                if (!Double.isNaN(point.x) && !Double.isInfinite(point.x) &&
                    !Double.isNaN(point.y) && !Double.isInfinite(point.y)) {
                    resultXList.add(point.x);
                    resultYList.add(point.y);
                }
            }
            
            if (resultXList.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Result function has no valid points");
                return;
            }
            
            double[] resultXValues = resultXList.stream().mapToDouble(Double::doubleValue).toArray();
            double[] resultYValues = resultYList.stream().mapToDouble(Double::doubleValue).toArray();
            
            // Создаем функцию результата
            Function resultFunction = new Function();
            resultFunction.setName(resultName);
            resultFunction.setExpression(operation + "(" + func1.getName() + "," + func2.getName() + ")");
            resultFunction.setUserId(authUser.getId());
            
            // Сохраняем функцию результата целиком как JSON (НЕ поточечно!)
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(resultXValues, resultYValues);
            resultFunction.setXValuesJson(jsonData[0]);
            resultFunction.setYValuesJson(jsonData[1]);
            resultFunction.setCount(resultXValues.length);
            
            Integer resultId = functionRepository.insert(resultFunction);
            if (resultId == null) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create result function");
                return;
            }
            resultFunction.setId(resultId);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(resultFunction);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Операция {} выполнена, создана функция {} (saved as whole, not point by point)", operation, resultId);
        } catch (Exception e) {
            logger.error("Ошибка при выполнении операции", e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleComposite(HttpServletRequest req, HttpServletResponse resp, User authUser, String requestBody) throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            String name = jsonNode.has("name") ? jsonNode.get("name").asText() : null;
            String innerFunction = jsonNode.has("innerFunction") ? jsonNode.get("innerFunction").asText() : null;
            String outerFunction = jsonNode.has("outerFunction") ? jsonNode.get("outerFunction").asText() : null;
            
            if (name == null || innerFunction == null || outerFunction == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "name, innerFunction and outerFunction are required");
                return;
            }
            
            // Проверяем, что функции существуют (может быть стандартная функция или компонентная функция пользователя)
            boolean innerExists = MATH_FUNCTIONS.containsKey(innerFunction);
            boolean outerExists = MATH_FUNCTIONS.containsKey(outerFunction);
            
            // Если не найдено в стандартных функциях, проверяем компонентные функции пользователя
            List<Function> userFunctions = functionRepository.findByUserId(authUser.getId());
            if (!innerExists) {
                for (Function func : userFunctions) {
                    if (func.getName().equals(innerFunction) && func.getExpression().contains(" ∘ ")) {
                        // Компонентная функция может не иметь JSON данных (только метаданные)
                        boolean hasJsonData = func.getXValuesJson() != null && func.getYValuesJson() != null &&
                                            !func.getXValuesJson().isEmpty() && !func.getYValuesJson().isEmpty();
                        if (!hasJsonData) {
                            // Это компонентная функция (метаданные без точек)
                            innerExists = true;
                            break;
                        }
                    }
                }
            }
            if (!outerExists) {
                for (Function func : userFunctions) {
                    if (func.getName().equals(outerFunction) && func.getExpression().contains(" ∘ ")) {
                        // Компонентная функция может не иметь JSON данных (только метаданные)
                        boolean hasJsonData = func.getXValuesJson() != null && func.getYValuesJson() != null &&
                                            !func.getXValuesJson().isEmpty() && !func.getYValuesJson().isEmpty();
                        if (!hasJsonData) {
                            // Это компонентная функция (метаданные без точек)
                            outerExists = true;
                            break;
                        }
                    }
                }
            }
            
            if (!innerExists || !outerExists) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "One or both math functions not found");
                return;
            }
            
            // Сохраняем компонентную функцию как метаданные (БЕЗ точек) в таблицу functions
            // Это позволит использовать её как математическую функцию для создания табулированных функций
            // Проверка уникальности имени функции для пользователя (как в framework)
            // Если функция с таким именем уже существует (любая - табулированная или компонентная), выдаем ошибку
            List<Function> existingFunctions = functionRepository.findByUserId(authUser.getId());
            for (Function func : existingFunctions) {
                if (func.getName().equalsIgnoreCase(name)) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Функция с именем '" + name + "' уже существует");
                    return;
                }
            }
            
            Function compositeFunction = new Function();
            compositeFunction.setName(name);
            compositeFunction.setExpression(outerFunction + " ∘ " + innerFunction);
            compositeFunction.setUserId(authUser.getId());
            
            Integer id = functionRepository.insert(compositeFunction);
            if (id == null) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create composite function");
                return;
            }
            compositeFunction.setId(id);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("name", name);
            response.put("innerFunction", innerFunction);
            response.put("outerFunction", outerFunction);
            response.put("description", outerFunction + " ∘ " + innerFunction);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().print(objectMapper.writeValueAsString(response));
            logger.info("Композитная функция создана (метаданные): {}", id);
        } catch (Exception e) {
            logger.error("Ошибка при валидации композитной функции", e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleInsertPoint(HttpServletRequest req, HttpServletResponse resp, User authUser, int functionId, String requestBody) throws IOException {
        try {
            Function function = functionRepository.findById(functionId);
            if (function == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found");
                return;
            }
            
            // Проверка доступа
            if (!Objects.equals(function.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            double x = jsonNode.has("x") ? jsonNode.get("x").asDouble() : 0.0;
            double y = jsonNode.has("y") ? jsonNode.get("y").asDouble() : 0.0;
            
            // СТРОГАЯ ПРОВЕРКА: функция должна быть сохранена в JSON полях
            if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
                function.getXValuesJson().isEmpty() || function.getYValuesJson().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        "Function is not saved correctly. Functions must be saved as whole, not point by point.");
                return;
            }
            
            // Десериализуем JSON поля
            double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
            double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            
            if (xValues.length < 2) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function must have at least 2 points to insert");
                return;
            }
            
            // Проверяем, существует ли точка с таким x
            for (double xVal : xValues) {
                if (Math.abs(xVal - x) < 1e-10) {
                    sendError(resp, HttpServletResponse.SC_CONFLICT, "Point with this x value already exists");
                    return;
                }
            }
            
            // Создаем табулированную функцию
            TabulatedFunctionFactory factory = new ArrayTabulatedFunctionFactory();
            TabulatedFunction tabulated = factory.create(xValues, yValues);
            
            // Проверяем, что функция поддерживает вставку
            if (!(tabulated instanceof Insertable)) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function does not support point insertion");
                return;
            }
            
            // Вставляем точку
            ((Insertable) tabulated).insert(x, y);
            
            // Извлекаем обновленные массивы
            List<Double> newXList = new ArrayList<>();
            List<Double> newYList = new ArrayList<>();
            for (functions.Point point : tabulated) {
                if (!Double.isNaN(point.x) && !Double.isInfinite(point.x) &&
                    !Double.isNaN(point.y) && !Double.isInfinite(point.y)) {
                    newXList.add(point.x);
                    newYList.add(point.y);
                }
            }
            
            double[] newXValues = newXList.stream().mapToDouble(Double::doubleValue).toArray();
            double[] newYValues = newYList.stream().mapToDouble(Double::doubleValue).toArray();
            
            // Сохраняем обновленную функцию целиком как JSON (НЕ поточечно!)
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(newXValues, newYValues);
            function.setXValuesJson(jsonData[0]);
            function.setYValuesJson(jsonData[1]);
            function.setCount(newXValues.length);
            functionRepository.update(function);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(function);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Точка вставлена в функцию {} (saved as whole, not point by point)", functionId);
        } catch (Exception e) {
            logger.error("Ошибка при вставке точки", e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleUpdateYValues(HttpServletRequest req, HttpServletResponse resp, User authUser, int functionId, String requestBody) throws IOException {
        try {
            Function function = functionRepository.findById(functionId);
            if (function == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found");
                return;
            }
            
            // Проверка доступа
            if (!Objects.equals(function.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            JsonNode yValuesNode = jsonNode.get("yValues");
            
            if (yValuesNode == null || !yValuesNode.isArray()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "yValues array is required");
                return;
            }
            
            // СТРОГАЯ ПРОВЕРКА: функция должна быть сохранена в JSON полях
            if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
                function.getXValuesJson().isEmpty() || function.getYValuesJson().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        "Function is not saved correctly. Functions must be saved as whole, not point by point.");
                return;
            }
            
            // Десериализуем JSON поля
            double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
            double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            
            if (xValues.length != yValuesNode.size() || yValues.length != yValuesNode.size()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Number of Y values must match number of points");
                return;
            }
            
            // Обновляем Y значения
            double[] newYValues = new double[yValuesNode.size()];
            for (int i = 0; i < yValuesNode.size(); i++) {
                newYValues[i] = yValuesNode.get(i).asDouble();
            }
            
            // Сохраняем обновленную функцию целиком как JSON (НЕ поточечно!)
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(xValues, newYValues);
            function.setXValuesJson(jsonData[0]);
            function.setYValuesJson(jsonData[1]);
            function.setCount(xValues.length);
            functionRepository.update(function);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(function);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Y значения обновлены для функции {} (saved as whole, not point by point)", functionId);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении Y значений", e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    private void handleRemovePoint(HttpServletRequest req, HttpServletResponse resp, User authUser, int functionId, int index) throws IOException {
        try {
            Function function = functionRepository.findById(functionId);
            if (function == null) {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found");
                return;
            }
            
            // Проверка доступа
            if (!Objects.equals(function.getUserId(), authUser.getId()) && !"ADMIN".equals(authUser.getRole())) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            // СТРОГАЯ ПРОВЕРКА: функция должна быть сохранена в JSON полях
            if (function.getXValuesJson() == null || function.getYValuesJson() == null ||
                function.getXValuesJson().isEmpty() || function.getYValuesJson().isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                        "Function is not saved correctly. Functions must be saved as whole, not point by point.");
                return;
            }
            
            // Десериализуем JSON поля
            double[] xValues = FunctionDataSerializer.deserializeArray(function.getXValuesJson());
            double[] yValues = FunctionDataSerializer.deserializeArray(function.getYValuesJson());
            
            if (xValues.length < 3) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function must have at least 3 points to remove");
                return;
            }
            
            if (index < 0 || index >= xValues.length) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid index: " + index);
                return;
            }
            
            // Создаем табулированную функцию
            TabulatedFunctionFactory factory = new ArrayTabulatedFunctionFactory();
            TabulatedFunction tabulated = factory.create(xValues, yValues);
            
            // Проверяем, что функция поддерживает удаление
            if (!(tabulated instanceof Removable)) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Function does not support point removal");
                return;
            }
            
            // Удаляем точку по индексу
            ((Removable) tabulated).remove(index);
            
            // Извлекаем обновленные массивы
            List<Double> newXList = new ArrayList<>();
            List<Double> newYList = new ArrayList<>();
            for (functions.Point point : tabulated) {
                if (!Double.isNaN(point.x) && !Double.isInfinite(point.x) &&
                    !Double.isNaN(point.y) && !Double.isInfinite(point.y)) {
                    newXList.add(point.x);
                    newYList.add(point.y);
                }
            }
            
            double[] newXValues = newXList.stream().mapToDouble(Double::doubleValue).toArray();
            double[] newYValues = newYList.stream().mapToDouble(Double::doubleValue).toArray();
            
            // Сохраняем обновленную функцию целиком как JSON (НЕ поточечно!)
            String[] jsonData = FunctionDataSerializer.serializeFunctionData(newXValues, newYValues);
            function.setXValuesJson(jsonData[0]);
            function.setYValuesJson(jsonData[1]);
            function.setCount(newXValues.length);
            functionRepository.update(function);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            ObjectNode jsonResponse = functionToJsonWithPoints(function);
            resp.getWriter().print(objectMapper.writeValueAsString(jsonResponse));
            logger.info("Точка удалена из функции {} по индексу {} (saved as whole, not point by point)", functionId, index);
        } catch (Exception e) {
            logger.error("Ошибка при удалении точки", e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Error: " + e.getMessage());
        }
    }
    
    // Вспомогательный метод для построения композитной функции из выражения
    // Поддерживает вложенные компонентные функции
    private MathFunction buildCompositeFunctionFromExpression(String expression, Integer userId) {
        if (expression == null || !expression.contains(" ∘ ")) {
            return null;
        }
        
        String[] parts = expression.split(" ∘ ");
        if (parts.length != 2) {
            return null;
        }
        
        String outerFunctionName = parts[0].trim();
        String innerFunctionName = parts[1].trim();
        
        // Получаем внутреннюю функцию
        MathFunction inner = null;
        if (MATH_FUNCTIONS.containsKey(innerFunctionName)) {
            inner = MATH_FUNCTIONS.get(innerFunctionName);
        } else {
            // Проверяем, является ли это компонентной функцией пользователя
            List<Function> userFunctions = functionRepository.findByUserId(userId);
            for (Function func : userFunctions) {
                if (func.getName().equals(innerFunctionName) && func.getExpression().contains(" ∘ ")) {
                    inner = buildCompositeFunctionFromExpression(func.getExpression(), userId);
                    break;
                }
            }
        }
        
        // Получаем внешнюю функцию
        MathFunction outer = null;
        if (MATH_FUNCTIONS.containsKey(outerFunctionName)) {
            outer = MATH_FUNCTIONS.get(outerFunctionName);
        } else {
            // Проверяем, является ли это компонентной функцией пользователя
            List<Function> userFunctions = functionRepository.findByUserId(userId);
            for (Function func : userFunctions) {
                if (func.getName().equals(outerFunctionName) && func.getExpression().contains(" ∘ ")) {
                    outer = buildCompositeFunctionFromExpression(func.getExpression(), userId);
                    break;
                }
            }
        }
        
        if (inner != null && outer != null) {
            return new CompositeFunction(inner, outer);
        }
        
        return null;
    }
}