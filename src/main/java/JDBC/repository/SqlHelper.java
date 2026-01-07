package JDBC.repository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlHelper {
    private static final Logger logger = LoggerFactory.getLogger(SqlHelper.class);
    public static String loadSqlFromFile(String filePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                logger.error("SQL файл не найден: {}", filePath);
                throw new IOException("SQL файл не найден: " + filePath);
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            logger.info("SQL файл успешно загружен: {}", filePath);
            return sql.trim(); // Убираем лишние пробелы и переносы строк
        } catch (IOException e) {
            logger.error("Ошибка при загрузке SQL файла {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Не удалось загрузить SQL файл: " + filePath, e);
        }
    }
}