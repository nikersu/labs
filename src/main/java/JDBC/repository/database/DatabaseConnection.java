package JDBC.repository.database;

import java.sql.Connection;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class.getName());
    private static final String PROPERTIES_FILE = "database.properties";
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/labs_oop";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "12345678";
    private static final Properties properties = loadProperties();
    
    static {
        // Явная регистрация драйвера PostgreSQL для совместимости с Java 9+ и Tomcat
        try {
            Class.forName("org.postgresql.Driver");
            logger.info("PostgreSQL driver registered successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to register PostgreSQL driver: {}", e.getMessage(), e);
        }
    }
    private static Properties loadProperties() {
        Properties loadedProperties = new Properties();
        try (InputStream inputStream = DatabaseConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream != null) {
                loadedProperties.load(inputStream);
                logger.info("Loaded database properties from {}", PROPERTIES_FILE);
            } else {
                logger.warn("Property file {} not found in classpath. Using defaults/environment variables.", PROPERTIES_FILE);
            }
        } catch (IOException exception) {
            logger.error("Failed to load {}: {}", PROPERTIES_FILE, exception.getMessage());
        }
        return loadedProperties;
    }

    private static String resolveSetting(String envName, String propertyKey, String defaultValue) {
        String environmentValue = System.getenv(envName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return defaultValue;
    }

    private static final String URL = resolveSetting("DB_URL", "db.url", DEFAULT_URL);
    private static final String USER = resolveSetting("DB_USERNAME", "db.username", DEFAULT_USER);
    private static final String PASSWORD = resolveSetting("DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            logger.info("Successful connection to the database");
            return connection;
        } catch (SQLException e) {
            logger.error("Database connection error: {}", e.getMessage());
            throw e;
        }
    }
}