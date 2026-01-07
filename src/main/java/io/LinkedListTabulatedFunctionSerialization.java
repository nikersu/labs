package io;

import functions.LinkedListTabulatedFunction;
import functions.TabulatedFunction;
import functions.SqrFunction;
import functions.factory.LinkedListTabulatedFunctionFactory;
import operations.TabulatedDifferentialOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class LinkedListTabulatedFunctionSerialization {
    private static final Logger logger = LoggerFactory.getLogger(LinkedListTabulatedFunctionSerialization.class);
    public static void main(String[] args) {
        // Сериализация функций
        try (FileOutputStream fileOutputStream = new FileOutputStream("output/serialized linked list functions.bin");
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

            // Создание исходной функции
            SqrFunction sourceFunction = new SqrFunction();
            TabulatedFunction originalFunction = new LinkedListTabulatedFunction(sourceFunction, 0.0, 2.0, 5);

            // Вычисление производных
            LinkedListTabulatedFunctionFactory factory = new LinkedListTabulatedFunctionFactory();
            TabulatedDifferentialOperator differentialOperator = new TabulatedDifferentialOperator(factory);
            TabulatedFunction firstDerivative = differentialOperator.derive(originalFunction);
            TabulatedFunction secondDerivative = differentialOperator.derive(firstDerivative);

            // Сериализация всех трех функций
            FunctionsIO.serialize(bufferedOutputStream, originalFunction);
            FunctionsIO.serialize(bufferedOutputStream, firstDerivative);
            FunctionsIO.serialize(bufferedOutputStream, secondDerivative);

        } catch (IOException e) {
            logger.error("The stream was interrupted", e);
            Thread.currentThread().interrupt(); // восстанавливаем флаг прерывания
        }

        // Десериализация функций
        try (FileInputStream fileInputStream = new FileInputStream("output/serialized linked list functions.bin");
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            // Десериализация всех трех функций
            TabulatedFunction deserializedOriginal = FunctionsIO.deserialize(bufferedInputStream);
            TabulatedFunction deserializedFirstDerivative = FunctionsIO.deserialize(bufferedInputStream);
            TabulatedFunction deserializedSecondDerivative = FunctionsIO.deserialize(bufferedInputStream);

            // Вывод функций в консоль
            System.out.println("Original function:");
            System.out.println(deserializedOriginal.toString());
            System.out.println("\nFirst derivative:");
            System.out.println(deserializedFirstDerivative.toString());
            System.out.println("\nSecond derivative:");
            System.out.println(deserializedSecondDerivative.toString());

        } catch (IOException e) {
            logger.error("I/O error during deserialization", e);
        } catch (ClassNotFoundException e) {
            logger.error("Class not found during deserialization", e);
        }
    }
}

