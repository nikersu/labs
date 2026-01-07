package io;

import functions.TabulatedFunction;
import functions.factory.TabulatedFunctionFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import functions.Point;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FunctionsIO {
    private static final Logger logger = LoggerFactory.getLogger(FunctionsIO.class);
    private FunctionsIO() {
        throw new UnsupportedOperationException();
    }

    public static void writeTabulatedFunction(BufferedOutputStream outputStream, TabulatedFunction function) throws IOException {
        logger.debug("Writing tabulated function to binary stream ({} points)", function.getCount());
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(function.getCount());
        for (Point point : function) {
            dataOutputStream.writeDouble(point.x);
            dataOutputStream.writeDouble(point.y);
        }
        outputStream.flush();
        logger.trace("Successfully wrote {} points to binary stream", function.getCount());
    }


    public static TabulatedFunction readTabulatedFunction(BufferedInputStream inputStream, TabulatedFunctionFactory factory) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int count = dataInputStream.readInt();
        double[] xValues = new double[count];
        double[] yValues = new double[count];
        for (int i = 0; i < count; i++) {
            xValues[i] = dataInputStream.readDouble();
            yValues[i] = dataInputStream.readDouble();
        }
        return factory.create(xValues, yValues);
    }

    public static TabulatedFunction deserialize(BufferedInputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        return (TabulatedFunction) objectInputStream.readObject();
    }

    public static void serialize(BufferedOutputStream stream, TabulatedFunction function) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
        objectOutputStream.writeObject(function);
        stream.flush();
    }
    public static void writeTabulatedFunction(BufferedWriter writer, TabulatedFunction function) throws IOException {
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println(function.getCount());
        for (Point point : function) {
            printWriter.printf("%f %f\n", point.x, point.y);
        }
        printWriter.flush();
    }
    public static TabulatedFunction readTabulatedFunction(BufferedReader reader, TabulatedFunctionFactory factory) throws IOException {
        String countLine = reader.readLine();
        int count = Integer.parseInt(countLine);

        double[] xValues = new double[count];
        double[] yValues = new double[count];
        NumberFormat format = NumberFormat.getInstance(Locale.forLanguageTag("ru"));
        for (int i = 0; i < count; i++) {
            String line = reader.readLine();
            String[] parts = line.split(" ");
            try {
                Number xNumber = format.parse(parts[0]);
                Number yNumber = format.parse(parts[1]);
                xValues[i] = xNumber.doubleValue();
                yValues[i] = yNumber.doubleValue();
            } catch (ParseException e) {
                logger.error("Parse error at line {}: '{}'", i + 2, line, e);
                throw new IOException(e);
            }
        }
        return factory.create(xValues, yValues);
    }
}