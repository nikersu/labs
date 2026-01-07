package util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FunctionDataSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(FunctionDataSerializer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Serializes double array to JSON string
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
            logger.error("Error serializing array to JSON", e);
            throw new RuntimeException("Failed to serialize array to JSON", e);
        }
    }
    
    /**
     * Deserializes JSON string to double array
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
            logger.error("Error deserializing JSON to array: {}", json, e);
            throw new RuntimeException("Failed to deserialize JSON to array", e);
        }
    }
    
    /**
     * Serializes two arrays (x and y values) to JSON strings
     */
    public static String[] serializeFunctionData(double[] xValues, double[] yValues) {
        return new String[]{
            serializeArray(xValues),
            serializeArray(yValues)
        };
    }
}


