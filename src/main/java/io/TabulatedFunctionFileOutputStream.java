package io;

import functions.ArrayTabulatedFunction;
import functions.LinkedListTabulatedFunction;
import functions.TabulatedFunction;
import functions.SqrFunction;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabulatedFunctionFileOutputStream {
    private static final Logger logger = LoggerFactory.getLogger(TabulatedFunctionFileOutputStream.class);
    public static void main(String[] args) {
        try (FileOutputStream arrayOutputStream = new FileOutputStream("output/array function.bin");
             FileOutputStream linkedListOutputStream = new FileOutputStream("output/linked list function.bin");
             BufferedOutputStream arrayBufferedStream = new BufferedOutputStream(arrayOutputStream);
             BufferedOutputStream linkedListBufferedStream = new BufferedOutputStream(linkedListOutputStream)) {

            // Создание функции на основе массива
            double[] xValues = {0.0, 0.5, 1.0, 1.5, 2.0};
            double[] yValues = {0.0, 0.25, 1.0, 2.25, 4.0};
            TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);

            // Создание функции на основе связного списка
            SqrFunction sourceFunction = new SqrFunction();
            TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(sourceFunction, 0.0, 2.0, 5);

            // Запись функций в соответствующие потоки
            FunctionsIO.writeTabulatedFunction(arrayBufferedStream, arrayFunction);
            FunctionsIO.writeTabulatedFunction(linkedListBufferedStream, linkedListFunction);

        } catch (IOException e) {
            logger.error("IOException while writing functions to files: {}", e.getMessage(), e);
        }
    }
}

