package concurrent;

import functions.ArrayTabulatedFunction;
import functions.LinkedListTabulatedFunction;
import functions.Point;
import functions.TabulatedFunction;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import static org.junit.jupiter.api.Assertions.*;

public class SynchronizedTabulatedFunctionTest {

    @Test
    public void testSynchronizedArrayFunction() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedFunction synchronizedFunction = new SynchronizedTabulatedFunction(arrayFunction);
        // проверка основных методов
        assertEquals(4, synchronizedFunction.getCount());
        assertEquals(1.0, synchronizedFunction.getX(0));
        assertEquals(4.0, synchronizedFunction.getY(1));
        assertEquals(1.0, synchronizedFunction.leftBound());
        assertEquals(4.0, synchronizedFunction.rightBound());
        // изменение значений
        synchronizedFunction.setY(2, 10.0);
        assertEquals(10.0, synchronizedFunction.getY(2));
        // поиск индексов
        assertEquals(1, synchronizedFunction.indexOfX(2.0));
        assertEquals(3, synchronizedFunction.indexOfY(16.0));
        // применение функции
        assertEquals(4.0, synchronizedFunction.apply(2.0));
    }

    @Test
    public void testSynchronizedLinkedListFunction() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);
        TabulatedFunction synchronizedFunction = new SynchronizedTabulatedFunction(linkedListFunction);
        // основные методы
        assertEquals(4, synchronizedFunction.getCount());
        assertEquals(1.0, synchronizedFunction.getX(0));
        assertEquals(4.0, synchronizedFunction.getY(1));
        // изменение значений
        synchronizedFunction.setY(3, 10.0);
        assertEquals(10.0, synchronizedFunction.getY(3));
    }

    @Test
    void testDoSynchronouslyWithReturnValue() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);
        Double sum = syncFunc.doSynchronously(func -> {
            double total = 0;
            for (int i = 0; i < func.getCount(); i++) {
                total += func.getY(i);
            }
            return total;
        });
        assertEquals(30.0, sum, 1e-9);
    }

    @Test
    void testDoSynchronouslyWithVoid() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 2.0, 3.0};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);
        Void result = syncFunc.doSynchronously(func -> {
            for (int i = 0; i < func.getCount(); i++) {
                func.setY(i, func.getY(i) * 2);
            }
            return null;
        });
        assertNull(result);
        // проверяем изменений
        assertEquals(2.0, syncFunc.getY(0), 1e-9);
        assertEquals(4.0, syncFunc.getY(1), 1e-9);
        assertEquals(6.0, syncFunc.getY(2), 1e-9);
    }

    @Test
    void testDoSynchronouslyWithComplexOperation() {
        double[] xValues = {0.0, 1.0, 2.0};
        double[] yValues = {0.0, 1.0, 4.0};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);
        String result = syncFunc.doSynchronously(func -> {
            String info = "Count: " + func.getCount() +
                    ", Left: " + func.leftBound() +
                    ", Right: " + func.rightBound();
            func.setY(1, 100.0);
            return info;
        });
        assertEquals("Count: 3, Left: 0.0, Right: 2.0", result);
        assertEquals(100.0, syncFunc.getY(1), 1e-9);
    }

    @Test
    void testDoSynchronouslyWithLambda() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);
        // использование лямбда-выражения для операции
        Double average = syncFunc.doSynchronously(func -> {
            double sum = 0;
            for (int i = 0; i < func.getCount(); i++) {
                sum += func.getY(i);
            }
            return sum / func.getCount();
        });
        assertEquals(20.0, average, 1e-9);
    }
    @Test
    void testDoSynchronouslyWithArrayResult() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);
        // операция, возвращающая массив
        double[] squaredValues = syncFunc.doSynchronously(func -> {
            double[] result = new double[func.getCount()];
            for (int i = 0; i < func.getCount(); i++) {
                result[i] = func.getY(i) * func.getY(i);
            }
            return result;
        });
        assertArrayEquals(new double[]{1.0, 16.0, 81.0}, squaredValues, 1e-9);
    }

    @Test
    void testIteratorReturnsSnapshot() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {2.0, 4.0, 6.0};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);

        Iterator<Point> iterator = syncFunc.iterator();
        syncFunc.setY(1, 100.0);

        assertTrue(iterator.hasNext());
        Point first = iterator.next();
        assertEquals(1.0, first.x);
        assertEquals(2.0, first.y);

        Point second = iterator.next();
        assertEquals(2.0, second.x);
        assertEquals(4.0, second.y);

        Point third = iterator.next();
        assertEquals(3.0, third.x);
        assertEquals(6.0, third.y);

        assertFalse(iterator.hasNext());
    }

    @Test
    void testIteratorOrder() {
        double[] xValues = {0.0, 0.5, 1.0, 1.5};
        double[] yValues = {0.0, 0.25, 1.0, 2.25};
        TabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunc = new SynchronizedTabulatedFunction(arrayFunc);

        Iterator<Point> iterator = syncFunc.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Point point = iterator.next();
            assertEquals(xValues[index], point.x);
            assertEquals(yValues[index], point.y);
            index++;
        }
        assertEquals(xValues.length, index);
    }
}