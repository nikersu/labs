package io;

import functions.TabulatedFunction;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import java.io.*;
import java.io.BufferedReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabulatedFunctionFileReader {
    private static final Logger logger = LoggerFactory.getLogger(TabulatedFunctionFileReader.class);
    public static void main(String[] args) {
        try (
                FileReader fileReader1 = new FileReader("input/function.txt");
                FileReader fileReader2 = new FileReader("input/function.txt");
                BufferedReader bufferedReader1 = new BufferedReader(fileReader1);
                BufferedReader bufferedReader2 = new BufferedReader(fileReader2)
        ) {
            TabulatedFunctionFactory arrayFactory = new ArrayTabulatedFunctionFactory();
            TabulatedFunctionFactory linkedListFactory = new LinkedListTabulatedFunctionFactory();

            TabulatedFunction arrayFunction = FunctionsIO.readTabulatedFunction(bufferedReader1, arrayFactory);
            TabulatedFunction linkedListFunction = FunctionsIO.readTabulatedFunction(bufferedReader2, linkedListFactory);

            System.out.println(arrayFunction.toString());
            System.out.println(linkedListFunction.toString());

        } catch (IOException e) {
            logger.error("IOException while reading functions from file: {}", e.getMessage(), e);
        }
    }
}