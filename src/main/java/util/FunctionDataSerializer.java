package util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для сериализации/десериализации данных функций в JSON.
 * Позволяет сохранять функции целиком, а не поточечно.
 */
public class FunctionDataSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(FunctionDataSerializer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Сериализует массив double в JSON строку
     */
    public static String serializeArray(double[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }
        try {
            List<Double> list = new ArrayList<>();
            for (double value : array) {
                list.add(value);
            }
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            logger.error("Ошибка при сериализации массива в JSON", e);
            throw new RuntimeException("Не удалось сериализовать массив в JSON", e);
        }
    }
    
    /**
     * Десериализует JSON строку в массив double
     */
    public static double[] deserializeArray(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new double[0];
        }
        try {
            List<Double> list = objectMapper.readValue(json, new TypeReference<List<Double>>() {});
            double[] array = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = list.get(i);
            }
            return array;
        } catch (Exception e) {
            logger.error("Ошибка при десериализации JSON в массив: {}", json, e);
            throw new RuntimeException("Не удалось десериализовать JSON в массив", e);
        }
    }
    
    /**
     * Сериализует два массива (x и y значения) в JSON строки
     * @return массив из двух строк: [xValuesJson, yValuesJson]
     */
    public static String[] serializeFunctionData(double[] xValues, double[] yValues) {
        return new String[]{
            serializeArray(xValues),
            serializeArray(yValues)
        };
    }
}


