package JDBC.repository.database;

import JDBC.repository.SqlHelper;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static boolean initialized = false;

    /**
     * Инициализирует базу данных: проверяет и создает необходимые таблицы, если их нет
     */
    public static synchronized void initialize() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            
            if (!initialized) {
                logger.info("Начало инициализации базы данных...");

                // Проверяем и создаем таблицу users
                if (!tableExists(conn, "users")) {
                    logger.info("Таблица 'users' не найдена. Создание таблицы...");
                    createUsersTable(conn);
                    logger.info("Таблица 'users' успешно создана");
                } else {
                    logger.info("Таблица 'users' уже существует");
                }

                // Проверяем и создаем таблицу functions
                if (!tableExists(conn, "functions")) {
                    logger.info("Таблица 'functions' не найдена. Создание таблицы...");
                    createFunctionsTable(conn);
                    logger.info("Таблица 'functions' успешно создана");
                } else {
                    logger.info("Таблица 'functions' уже существует");
                    // Выполняем миграцию для добавления JSON полей (если их еще нет)
                    migrateFunctionsTable(conn);
                }

                // Проверяем и создаем таблицу points
                if (!tableExists(conn, "points")) {
                    logger.info("Таблица 'points' не найдена. Создание таблицы...");
                    createPointsTable(conn);
                    logger.info("Таблица 'points' успешно создана");
                } else {
                    logger.info("Таблица 'points' уже существует");
                }

                initialized = true;
                logger.info("Инициализация базы данных завершена успешно");
            } else {
                logger.info("База данных уже инициализирована, проверяем наличие администратора...");
            }

            // Создаем администратора по умолчанию, если его еще нет (всегда проверяем)
            createDefaultAdmin(conn);
        } catch (SQLException e) {
            logger.error("SQL ошибка при инициализации базы данных: код={}, состояние={}, сообщение={}", 
                    e.getErrorCode(), e.getSQLState(), e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать базу данных: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Ошибка при инициализации базы данных: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при инициализации базы данных: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать базу данных: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warn("Ошибка при закрытии соединения: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Проверяет существование таблицы в базе данных
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            // PostgreSQL приводит имена таблиц к нижнему регистру, если они не в кавычках
            try (ResultSet rs = metaData.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
                boolean exists = rs.next();
                logger.debug("Проверка существования таблицы '{}': {}", tableName, exists);
                return exists;
            }
        } catch (SQLException e) {
            logger.warn("Ошибка при проверке существования таблицы '{}': {}", tableName, e.getMessage());
            // Если не можем проверить, предполагаем что таблица не существует
            return false;
        }
    }

    /**
     * Создает таблицу users
     */
    private static void createUsersTable(Connection conn) throws SQLException {
        try {
            String sql = SqlHelper.loadSqlFromFile("scripts/users/create_users_table.sql");
            logger.debug("Выполнение SQL для создания таблицы users: {}", sql);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (RuntimeException e) {
            logger.error("Ошибка при загрузке SQL для таблицы users: {}", e.getMessage(), e);
            throw new SQLException("Не удалось загрузить SQL для создания таблицы users: " + e.getMessage(), e);
        }
    }

    /**
     * Создает таблицу functions
     */
    private static void createFunctionsTable(Connection conn) throws SQLException {
        try {
            String sql = SqlHelper.loadSqlFromFile("scripts/functions/create_functions_table.sql");
            logger.debug("Выполнение SQL для создания таблицы functions: {}", sql);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (RuntimeException e) {
            logger.error("Ошибка при загрузке SQL для таблицы functions: {}", e.getMessage(), e);
            throw new SQLException("Не удалось загрузить SQL для создания таблицы functions: " + e.getMessage(), e);
        }
    }

    /**
     * Создает таблицу points
     */
    private static void createPointsTable(Connection conn) throws SQLException {
        try {
            String sql = SqlHelper.loadSqlFromFile("scripts/points/create_points_table.sql");
            logger.debug("Выполнение SQL для создания таблицы points: {}", sql);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (RuntimeException e) {
            logger.error("Ошибка при загрузке SQL для таблицы points: {}", e.getMessage(), e);
            throw new SQLException("Не удалось загрузить SQL для создания таблицы points: " + e.getMessage(), e);
        }
    }

    /**
     * Выполняет миграцию таблицы functions: добавляет поля для хранения функций целиком как JSON
     */
    private static void migrateFunctionsTable(Connection conn) {
        try {
            // Проверяем, существуют ли уже поля x_values, y_values, count
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "functions", "x_values")) {
                if (rs.next()) {
                    logger.info("Поля x_values, y_values, count уже существуют в таблице functions, миграция не требуется");
                    return;
                }
            }
            
            // Выполняем миграцию
            logger.info("Выполнение миграции таблицы functions: добавление полей x_values, y_values, count...");
            String sql = SqlHelper.loadSqlFromFile("scripts/functions/alter_functions_add_json_fields.sql");
            logger.debug("Выполнение SQL миграции: {}", sql);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                logger.info("Миграция таблицы functions успешно выполнена");
            }
        } catch (SQLException e) {
            // Если поля уже существуют или другая ошибка - не критично
            if (e.getSQLState() != null && e.getSQLState().equals("42710")) {
                // Ошибка "duplicate column" - поля уже существуют
                logger.info("Поля x_values, y_values, count уже существуют в таблице functions");
            } else {
                logger.warn("Ошибка при выполнении миграции таблицы functions: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Неожиданная ошибка при выполнении миграции таблицы functions: {}", e.getMessage());
        }
    }

    /**
     * Создает пользователя-администратора по умолчанию (admin/admin), если его еще нет
     */
    private static void createDefaultAdmin(Connection conn) {
        final String adminUsername = "admin";
        final String adminPassword = "admin";
        final String adminRole = "ADMIN";

        try {
            // Проверяем, существует ли уже пользователь admin
            String checkSql = SqlHelper.loadSqlFromFile("scripts/users/select_user_username.sql");
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, adminUsername);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        logger.info("Пользователь '{}' уже существует, пропускаем создание администратора", adminUsername);
                        return;
                    }
                }
            }

            // Создаем администратора
            String hashedPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
            String insertSql = SqlHelper.loadSqlFromFile("scripts/users/insert_user.sql");
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, adminUsername);
                insertStmt.setString(2, hashedPassword);
                insertStmt.setString(3, adminRole);
                int affectedRows = insertStmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int adminId = generatedKeys.getInt(1);
                            logger.info("Создан пользователь-администратор по умолчанию: username='{}', id={}, role={}", 
                                    adminUsername, adminId, adminRole);
                        }
                    }
                } else {
                    logger.warn("Не удалось создать пользователя-администратора по умолчанию");
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании администратора по умолчанию: {}", e.getMessage(), e);
            // Не прерываем инициализацию, если не удалось создать администратора
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при создании администратора по умолчанию: {}", e.getMessage(), e);
            // Не прерываем инициализацию, если не удалось создать администратора
        }
    }
}

