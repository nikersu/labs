package io;

import functions.ArrayTabulatedFunction;
import functions.TabulatedFunction;
import operations.TabulatedDifferentialOperator;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArrayTabulatedFunctionSerialization {
    private static final Logger logger = LoggerFactory.getLogger(ArrayTabulatedFunctionSerialization.class);
    public static void main(String[] args) {
        // исходная функция
        double[] xValues = {0.0, 1.0, 2.0, 3.0, 4.0};
        double[] yValues = {0.0, 1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // производные
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction firstDerivative = operator.derive(function);
        TabulatedFunction secondDerivative = operator.derive(firstDerivative);

        // сериализация
        try (FileOutputStream fileOutputStream = new FileOutputStream("output/serialized array functions.bin");
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
            FunctionsIO.serialize(bufferedOutputStream, function);
            FunctionsIO.serialize(bufferedOutputStream, firstDerivative);
            FunctionsIO.serialize(bufferedOutputStream, secondDerivative);
            System.out.println("the serialization was successful");

        } catch (IOException e) {
            logger.error("Serialization failed: {}", e.getMessage(), e);
        }

        // десериализация
        try (FileInputStream fileInputStream = new FileInputStream("output/serialized array functions.bin");
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            TabulatedFunction deserializedFunction = FunctionsIO.deserialize(bufferedInputStream);
            TabulatedFunction deserializedFirstDerivative = FunctionsIO.deserialize(bufferedInputStream);
            TabulatedFunction deserializedSecondDerivative = FunctionsIO.deserialize(bufferedInputStream);

            System.out.println("The original function: " + deserializedFunction.toString());
            System.out.println("The first derivative: " + deserializedFirstDerivative.toString());
            System.out.println("The second derivative: " + deserializedSecondDerivative.toString());
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Deserialization failed: {}", e.getMessage(), e);
        }
    }
}