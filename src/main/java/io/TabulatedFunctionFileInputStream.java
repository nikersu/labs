package io;

import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import operations.TabulatedDifferentialOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class TabulatedFunctionFileInputStream {
    private static final Logger logger = LoggerFactory.getLogger(TabulatedFunctionFileInputStream.class);
    public static void main(String[] args) {
        // Чтение функции из файла
        try (FileInputStream fileInputStream = new FileInputStream("input/binary function.bin");
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            ArrayTabulatedFunctionFactory arrayFactory = new ArrayTabulatedFunctionFactory();
            TabulatedFunction function = FunctionsIO.readTabulatedFunction(bufferedInputStream, arrayFactory);
            System.out.println(function.toString());

        } catch (IOException e) {
            logger.error("Error reading function from binary file: {}", e.getMessage(), e);
        }

        // Чтение функции из консоли
        System.out.println("Введите размер и значения функции");
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(System.in);
            
            LinkedListTabulatedFunctionFactory linkedListFactory = new LinkedListTabulatedFunctionFactory();
            TabulatedFunction consoleFunction = FunctionsIO.readTabulatedFunction(bufferedInputStream, linkedListFactory);
            
            TabulatedDifferentialOperator differentialOperator = new TabulatedDifferentialOperator();
            TabulatedFunction derivative = differentialOperator.derive(consoleFunction);
            System.out.println(derivative.toString());

        } catch (IOException e) {
            logger.error("Error reading function from console input: {}", e.getMessage(), e);
        }
    }
}

