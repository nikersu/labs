package operations;

import functions.TabulatedFunction;
import functions.Point;
import functions.factory.TabulatedFunctionFactory;
import functions.factory.ArrayTabulatedFunctionFactory;
import concurrent.SynchronizedTabulatedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabulatedDifferentialOperator implements DifferentialOperator<TabulatedFunction> {
    private static final Logger logger = LoggerFactory.getLogger(TabulatedDifferentialOperator.class);
    private TabulatedFunctionFactory factory;

    public TabulatedDifferentialOperator() {
        this.factory = new ArrayTabulatedFunctionFactory();
    }

    public TabulatedDifferentialOperator(TabulatedFunctionFactory factory) {
        this.factory = factory;
    }

    public TabulatedFunctionFactory getFactory() {
        return factory;
    }

    public void setFactory(TabulatedFunctionFactory factory) {
        this.factory = factory;
    }

    @Override
    public TabulatedFunction derive(TabulatedFunction function) {
        logger.info("Calculating derivative for function with {} points", function.getCount());
        // Получаем все точки входной функции
        Point[] points = TabulatedFunctionOperationService.asPoints(function);
        int count = points.length;

        // Создаём массивы xValues и yValues такой же длины
        double[] xValues = new double[count];
        double[] yValues = new double[count];

        // Заполняем xValues (они остаются теми же)
        for (int i = 0; i < count; i++) {
            xValues[i] = points[i].x;
        }

        // Вычисляем производную с помощью численного дифференцирования
        if (count == 1) {
            // Если только одна точка, производная равна 0
            yValues[0] = 0.0;
        } else {
            // Первая точка: правая разность (forward difference)
            yValues[0] = (points[1].y - points[0].y) / (points[1].x - points[0].x);

            // Внутренние точки: центральная разность (central difference)
            for (int i = 1; i < count - 1; i++) {
                yValues[i] = (points[i + 1].y - points[i - 1].y) / (points[i + 1].x - points[i - 1].x);
            }

            // Последняя точка: левая разность (backward difference)
            yValues[count - 1] = (points[count - 1].y - points[count - 2].y) / (points[count - 1].x - points[count - 2].x);
        }

        // Создаём новый экземпляр табулированной функции с помощью фабрики
        logger.info("Successfully created derivative function");
        return factory.create(xValues, yValues);
    }
    public TabulatedFunction deriveSynchronously(TabulatedFunction function) {
        logger.debug("Starting synchronous derivative calculation");
        SynchronizedTabulatedFunction syncFunction = (function instanceof SynchronizedTabulatedFunction)
                ? (SynchronizedTabulatedFunction) function
                : new SynchronizedTabulatedFunction(function);
        TabulatedFunction result = syncFunction.doSynchronously(this::derive);
        logger.info("Synchronous derivative calculation completed");
        return result;

    }
}
