package servlets;

import DTO.User;
import JDBC.repository.UserRepository;
import JDBC.repository.database.DatabaseInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

public class AuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        this.userRepository = new UserRepository();
        this.objectMapper = new ObjectMapper();
        
        // Инициализация БД в фоне - не блокируем загрузку сервлета
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            // Игнорируем ошибки инициализации БД - таблицы могут уже существовать
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();
        
        if (!"OPTIONS".equals(method) && !"GET".equals(method) && !"POST".equals(method)) {
            super.service(req, resp);
            return;
        }
        
        if ("OPTIONS".equals(method)) {
            doOptions(req, resp);
        } else if ("GET".equals(method)) {
            doGet(req, resp);
        } else if ("POST".equals(method)) {
            doPost(req, resp);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Обработка preflight запросов для CORS
        ServletHelper.setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Установка CORS заголовков
        ServletHelper.setCorsHeaders(resp);
        
        String pathInfo = req.getPathInfo();
        String message = "HTTP метод GET не поддерживается для этого endpoint. Используйте POST для регистрации и входа.";
        
        logger.warn("Получен GET запрос на {} (не поддерживается)", pathInfo);
        sendError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Установка CORS заголовков
        ServletHelper.setCorsHeaders(resp);
        
        String pathInfo = req.getPathInfo();
        logger.info("Получен POST запрос на pathInfo: {}", pathInfo);
        
        // (POST /api/auth/register) - регистрация
        if (pathInfo == null || "/register".equals(pathInfo)) {
            handleRegister(req, resp);
            return;
        }
        
        // (POST /api/auth/login) - вход
        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
            return;
        }
        
        // (POST /api/auth/create-admin) - создание администратора (только если его нет)
        if ("/create-admin".equals(pathInfo)) {
            handleCreateAdmin(req, resp);
            return;
        }
        
        logger.warn("POST запрос на неизвестный endpoint: {}", pathInfo);
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
    }
    
    // Обработка регистрации
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("Получен запрос на регистрацию нового пользователя");
        // чтение тела
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(sb.toString());

            // проверка обязательных полей
            if (!jsonNode.has("username") || !jsonNode.has("password")) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Требуются поля: username и password"); // 400
                return;
            }

            String username = jsonNode.get("username").asText().trim();
            String plainPassword = jsonNode.get("password").asText();

            if (username.isEmpty() || plainPassword.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "username и password не могут быть пустыми"); // 400
                return;
            }
            // проверка существует ли уже пользователь с таким именем
            User existing = userRepository.findByUsername(username);
            if (existing != null) {
                sendError(resp, HttpServletResponse.SC_CONFLICT,
                        "Пользователь с таким именем уже существует"); // 409
                return;
            }
            // хеширование
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(hashedPassword);
            user.setRole("USER");

            // сохранение в бд
            Integer id;
            try {
                id = userRepository.insert(user);
            } catch (RuntimeException e) {
                logger.error("Ошибка при вставке пользователя в БД: {}", e.getMessage(), e);
                Throwable cause = e.getCause();
                if (cause instanceof SQLException) {
                    SQLException sqlEx = (SQLException) cause;
                    logger.error("SQL ошибка: код={}, состояние={}, сообщение={}", 
                            sqlEx.getErrorCode(), sqlEx.getSQLState(), sqlEx.getMessage());
                    // Проверяем, не является ли это ошибкой дублирования (например, уникальный ключ)
                    if (sqlEx.getSQLState() != null && sqlEx.getSQLState().startsWith("23")) {
                        sendError(resp, HttpServletResponse.SC_CONFLICT,
                                "Пользователь с таким именем уже существует");
                    } else {
                        sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Ошибка базы данных при создании пользователя");
                    }
                } else {
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Ошибка при создании пользователя");
                }
                return;
            }
            
            if (id == null) {
                logger.error("userRepository.insert() вернул null для пользователя {}", username);
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Ошибка при создании пользователя");
                return;
            }
            user.setId(id);
            user.setPasswordHash(null);
            
            // Установка заголовков ПЕРЕД записью в ответ
            ServletHelper.setCorsHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // тело ответа
            String responseJson = objectMapper.writeValueAsString(user);
            try (PrintWriter writer = resp.getWriter()) {
                writer.print(responseJson);
                writer.flush();
            }
            logger.info("пользователь создан с ID: {}", id);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Ошибка парсинга JSON при регистрации: {}", e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Неверный формат JSON");  // 400
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при регистрации", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Внутренняя ошибка сервера"); // 500
        }
    }
    
    // Обработка входа
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Установка CORS заголовков
        ServletHelper.setCorsHeaders(resp);
        
        logger.info("Получен запрос на вход пользователя");
        // чтение тела запроса
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(sb.toString());
            
            // проверка обязательных полей
            if (!jsonNode.has("username") || !jsonNode.has("password")) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Требуются поля: username и password"); // 400
                return;
            }
            
            String username = jsonNode.get("username").asText().trim();
            String plainPassword = jsonNode.get("password").asText();
            
            if (username.isEmpty() || plainPassword.isEmpty()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "username и password не могут быть пустыми"); // 400
                return;
            }
            
            // поиск пользователя
            User user = userRepository.findByUsername(username);
            if (user == null) {
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        "Неверное имя пользователя или пароль"); // 401
                return;
            }
            
            // проверка пароля через BCrypt
            String storedHash = user.getPasswordHash();
            if (!BCrypt.checkpw(plainPassword, storedHash)) {
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        "Неверное имя пользователя или пароль"); // 401
                return;
            }
            
            // успешная аутентификация
            user.setPasswordHash(null); // не возвращаем хеш пароля
            resp.setStatus(HttpServletResponse.SC_OK); // 200
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            
            // тело ответа
            String responseJson = objectMapper.writeValueAsString(user);
            try (PrintWriter writer = resp.getWriter()) {
                writer.print(responseJson);
            }
            logger.info("Пользователь {} успешно вошел в систему", username);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Неверный формат JSON"); // 400
        } catch (Exception e) {
            logger.error("Ошибка при входе", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Внутренняя ошибка сервера"); // 500
        }
    }

    // Создание администратора (только если его нет)
    private void handleCreateAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletHelper.setCorsHeaders(resp);
        logger.info("Получен запрос на создание администратора");
        
        try {
            // Проверяем, существует ли уже пользователь admin
            User existing = userRepository.findByUsername("admin");
            if (existing != null) {
                sendError(resp, HttpServletResponse.SC_CONFLICT, "Администратор уже существует");
                return;
            }
            
            // Создаем администратора
            String hashedPassword = BCrypt.hashpw("admin", BCrypt.gensalt());
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(hashedPassword);
            admin.setRole("ADMIN");
            
            Integer id = userRepository.insert(admin);
            if (id == null) {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Не удалось создать администратора");
                return;
            }
            
            admin.setId(id);
            admin.setPasswordHash(null);
            
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            
            String responseJson = objectMapper.writeValueAsString(admin);
            try (PrintWriter writer = resp.getWriter()) {
                writer.print(responseJson);
            }
            logger.info("Администратор создан с ID: {}", id);
        } catch (Exception e) {
            logger.error("Ошибка при создании администратора", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
        }
    }

    // метод для возврата ошибок (формат JSON)
    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        // Установка CORS заголовков
        ServletHelper.setCorsHeaders(resp);
        
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String errorJson = String.format("{\"error\": \"%s\"}", message);
        try (PrintWriter writer = resp.getWriter()) {
            writer.print(errorJson);
        }
        logger.warn("Ошибка {}: {}", status, message);
    }
}