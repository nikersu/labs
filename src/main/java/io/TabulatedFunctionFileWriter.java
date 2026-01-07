package io;

import functions.TabulatedFunction;
import functions.ArrayTabulatedFunction;
import functions.LinkedListTabulatedFunction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabulatedFunctionFileWriter {
    private static final Logger logger = LoggerFactory.getLogger(TabulatedFunctionFileWriter.class);
    public static void main(String[] args) {
        double[] xValues = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0, 25.0};
        // создаем две табулированные функции
        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);

        try (
                FileWriter arrayFileWriter = new FileWriter("output/array function.txt");
                FileWriter linkedListFileWriter = new FileWriter("output/linked list function.txt");
                BufferedWriter arrayBufferedWriter = new BufferedWriter(arrayFileWriter);
                BufferedWriter linkedListBufferedWriter = new BufferedWriter(linkedListFileWriter)
        ) {
            FunctionsIO.writeTabulatedFunction(arrayBufferedWriter, arrayFunction);
            FunctionsIO.writeTabulatedFunction(linkedListBufferedWriter, linkedListFunction);
            System.out.println("Functions have been successfully written to files");

        } catch (IOException e) {
            logger.error("IOException while writing functions to text files: {}", e.getMessage(), e);
        }
    }
}