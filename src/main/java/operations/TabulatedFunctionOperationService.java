package operations;

import functions.TabulatedFunction;
import functions.Point;
import functions.factory.TabulatedFunctionFactory;
import functions.factory.ArrayTabulatedFunctionFactory;
import exceptions.InconsistentFunctionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabulatedFunctionOperationService {
    private static final Logger logger = LoggerFactory.getLogger(TabulatedFunctionOperationService.class);
    private TabulatedFunctionFactory factory;

    public TabulatedFunctionOperationService() {
        factory = new ArrayTabulatedFunctionFactory();
    }
    public TabulatedFunctionOperationService(TabulatedFunctionFactory factory) {
        this.factory = factory;
    }
    public TabulatedFunctionFactory getFactory() {
        return factory;
    }
    public void setFactory(TabulatedFunctionFactory factory) {
        this.factory = factory;
    }
    public static Point[] asPoints(TabulatedFunction tabulatedFunction) {
        // создаем массив точек размером, равным количеству точек в функции
        Point[] points = new Point[tabulatedFunction.getCount()];
        int i = 0;
        // цикл for-each для итерации по точкам функции
        for (Point point : tabulatedFunction) {
            points[i] = point;
            i++;
        }
        logger.trace("Converted function to {} points array", points.length);
        return points;
    }
    private interface BiOperation {
        double apply(double u, double v);
    }

    private TabulatedFunction doOperation(TabulatedFunction a, TabulatedFunction b, BiOperation operation) {
        if (a.getCount() != b.getCount()) {
            logger.error("Function count mismatch: {} != {}", a.getCount(), b.getCount());
            throw new InconsistentFunctionsException("The number of points in the functions does not match");
        }
        // получение точек функций
        Point[] pointsA = asPoints(a);
        Point[] pointsB = asPoints(b);

        // создание массивов для результатов
        double[] xValues = new double[a.getCount()];
        double[] yValues = new double[a.getCount()];

        // выполнение операции
        for (int i = 0; i < a.getCount(); i++) {
            if (pointsA[i].x != pointsB[i].x) {
                logger.error("X-coordinate mismatch at index {}: {} != {}", i, pointsA[i].x, pointsB[i].x);
                throw new InconsistentFunctionsException("The X-coordinates don't match");
            }
            xValues[i] = pointsA[i].x;
            yValues[i] = operation.apply(pointsA[i].y, pointsB[i].y);
        }
        // создание новой функции
        return factory.create(xValues, yValues);
    }
    public TabulatedFunction add(TabulatedFunction a, TabulatedFunction b) {
        return doOperation(a, b, (double f, double s) -> f + s); // сложение
    }
    public TabulatedFunction subtract(TabulatedFunction a, TabulatedFunction b) {
        return doOperation(a, b, (double f, double s) -> f - s); // вычитание
    }
    public TabulatedFunction multiply(TabulatedFunction a, TabulatedFunction b) {
        return doOperation(a, b, (double f, double s) -> f * s); // умножение
    }
    public TabulatedFunction divide(TabulatedFunction a, TabulatedFunction b) {
        return doOperation(a, b, (double f, double s) -> f / s); // деление
    }
}
