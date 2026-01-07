package operations;

import functions.ArrayTabulatedFunction;
import functions.LinkedListTabulatedFunction;
import functions.Point;
import functions.TabulatedFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import exceptions.InconsistentFunctionsException;

public class TabulatedFunctionOperationServiceTest {

    @Test
    public void testAsPointsWithArrayTabulatedFunction() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};

        // создаем табулированную функцию на основе массива
        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        // преобразуем функцию в массив точек
        Point[] points = TabulatedFunctionOperationService.asPoints(function);
        // проверка на корректность преобразования
        assertEquals(xValues.length, points.length);

        for (int i = 0; i < points.length; i++) {
            assertEquals(xValues[i], points[i].x, 1e-10);
            assertEquals(yValues[i], points[i].y, 1e-10);
        }
    }

    @Test
    public void testAsPointsWithLinkedListTabulatedFunction() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {2.0, 4.0, 6.0, 8.0};
        // создаем табулированную функцию на основе связного списка
        TabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        // преобразуем функцию в массив точек
        Point[] points = TabulatedFunctionOperationService.asPoints(function);

        // проверка на корректность преобразования
        assertEquals(xValues.length, points.length);
        for (int i = 0; i < points.length; i++) {
            assertEquals(xValues[i], points[i].x, 1e-10);
            assertEquals(yValues[i], points[i].y, 1e-10);
        }
    }

    @Test
    public void testAsPointsWithSinglePoint() {
        // тест для функции с минимальным количеством точек
        double[] xValues = {1.0, 2.0};
        double[] yValues = {3.0, 4.0};

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        Point[] points = TabulatedFunctionOperationService.asPoints(function);

        assertEquals(2, points.length);
        assertEquals(1.0, points[0].x, 1e-10);
        assertEquals(3.0, points[0].y, 1e-10);
        assertEquals(2.0, points[1].x, 1e-10);
        assertEquals(4.0, points[1].y, 1e-10);
    }

    @Test
    public void testAsPointsOrder() {
        // проверка порядка точек в массиве
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};

        TabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        Point[] points = TabulatedFunctionOperationService.asPoints(function);

        // проверка, что точки идут в правильном порядке
        assertEquals(1.0, points[0].x, 1e-10);
        assertEquals(10.0, points[0].y, 1e-10);
        assertEquals(2.0, points[1].x, 1e-10);
        assertEquals(20.0, points[1].y, 1e-10);
        assertEquals(3.0, points[2].x, 1e-10);
        assertEquals(30.0, points[2].y, 1e-10);
    }
    @Test
    public void testAddSameTypeFunctions() {
        // тестирование сложения функций одного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0, 6.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.add(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(5.0, result.getY(0), 1e-10); // 1.0 + 4.0
        assertEquals(7.0, result.getY(1), 1e-10); // 2.0 + 5.0
        assertEquals(9.0, result.getY(2), 1e-10); // 3.0 + 6.0
    }

    @Test
    public void testAddDifferentTypeFunctions() {
        // тестирование сложения функций разного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0, 6.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new LinkedListTabulatedFunction(xValues, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.add(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(5.0, result.getY(0), 1e-10);
        assertEquals(7.0, result.getY(1), 1e-10);
        assertEquals(9.0, result.getY(2), 1e-10);
    }

    @Test
    public void testSubtractDifferentTypeFunctions() {
        // тестирование вычитания функций разного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {5.0, 7.0, 9.0};
        double[] yValues2 = {1.0, 2.0, 3.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new LinkedListTabulatedFunction(xValues, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.subtract(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(4.0, result.getY(0), 1e-10); // 5.0 - 1.0
        assertEquals(5.0, result.getY(1), 1e-10); // 7.0 - 2.0
        assertEquals(6.0, result.getY(2), 1e-10); // 9.0 - 3.0
    }

    @Test
    public void testAddWithDifferentFactory() {
        // тестирование с другой фабрикой
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0, 6.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new LinkedListTabulatedFunctionFactory());
        TabulatedFunction result = service.add(function1, function2);
        assertEquals(3, result.getCount());
        assertInstanceOf(LinkedListTabulatedFunction.class, result);
    }

    @Test
    public void testInconsistentFunctionsExceptionForDifferentCount() {
        // тестирование исключения при разном количестве точек
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] xValues2 = {1.0, 2.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues2, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertThrows(InconsistentFunctionsException.class, () -> service.add(function1, function2));
    }

    @Test
    public void testInconsistentFunctionsExceptionForDifferentX() {
        // тестирование исключения при разных x-координатах
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] xValues2 = {1.0, 2.5, 3.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0, 6.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues2, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertThrows(InconsistentFunctionsException.class, () -> service.add(function1, function2));
    }

    @Test
    public void testFactoryGetterAndSetter() {
        // тестирование геттера и сеттера фабрики
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertInstanceOf(ArrayTabulatedFunctionFactory.class, service.getFactory());
        service.setFactory(new LinkedListTabulatedFunctionFactory());
        assertInstanceOf(LinkedListTabulatedFunctionFactory.class, service.getFactory());
    }
    @Test
    public void testMultiplySameTypeFunctions() {
        // тестирование умножения функций одного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {2.0, 3.0, 4.0};
        double[] yValues2 = {5.0, 6.0, 7.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.multiply(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(10.0, result.getY(0), 1e-10); // 2.0 * 5.0
        assertEquals(18.0, result.getY(1), 1e-10); // 3.0 * 6.0
        assertEquals(28.0, result.getY(2), 1e-10); // 4.0 * 7.0
    }

    @Test
    public void testMultiplyDifferentTypeFunctions() {
        // тестирование умножения функций разного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {2.0, 3.0, 4.0};
        double[] yValues2 = {5.0, 6.0, 7.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new LinkedListTabulatedFunction(xValues, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.multiply(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(10.0, result.getY(0), 1e-10);
        assertEquals(18.0, result.getY(1), 1e-10);
        assertEquals(28.0, result.getY(2), 1e-10);
    }

    @Test
    public void testMultiplyWithDifferentFactory() {
        // тестирование умножения с другой фабрикой
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {2.0, 3.0, 4.0};
        double[] yValues2 = {5.0, 6.0, 7.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new LinkedListTabulatedFunctionFactory());
        TabulatedFunction result = service.multiply(function1, function2);
        assertEquals(3, result.getCount());
        assertInstanceOf(LinkedListTabulatedFunction.class, result);
    }

    @Test
    public void testDivideSameTypeFunctions() {
        // тестирование деления функций одного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {10.0, 20.0, 30.0};
        double[] yValues2 = {2.0, 4.0, 5.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.divide(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(5.0, result.getY(0), 1e-10); // 10.0 / 2.0
        assertEquals(5.0, result.getY(1), 1e-10); // 20.0 / 4.0
        assertEquals(6.0, result.getY(2), 1e-10); // 30.0 / 5.0
    }

    @Test
    public void testDivideDifferentTypeFunctions() {
        // тестирование деления функций разного типа
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {15.0, 24.0, 35.0};
        double[] yValues2 = {3.0, 4.0, 5.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new LinkedListTabulatedFunction(xValues, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        TabulatedFunction result = service.divide(function1, function2);

        assertEquals(3, result.getCount());
        assertEquals(5.0, result.getY(0), 1e-10); // 15.0 / 3.0
        assertEquals(6.0, result.getY(1), 1e-10); // 24.0 / 4.0
        assertEquals(7.0, result.getY(2), 1e-10); // 35.0 / 5.0
    }

    @Test
    public void testDivideWithDifferentFactory() {
        // тестирование деления с другой фабрикой
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues1 = {12.0, 18.0, 28.0};
        double[] yValues2 = {3.0, 6.0, 7.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new LinkedListTabulatedFunctionFactory());
        TabulatedFunction result = service.divide(function1, function2);
        assertEquals(3, result.getCount());
        assertInstanceOf(LinkedListTabulatedFunction.class, result);
        assertEquals(4.0, result.getY(0), 1e-10); // 12.0 / 3.0
        assertEquals(3.0, result.getY(1), 1e-10); // 18.0 / 6.0
        assertEquals(4.0, result.getY(2), 1e-10); // 28.0 / 7.0
    }

    @Test
    public void testMultiplyInconsistentFunctionsExceptionForDifferentCount() {
        // тестирование исключения при умножении функций с разным количеством точек
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] xValues2 = {1.0, 2.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues2, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertThrows(InconsistentFunctionsException.class, () -> service.multiply(function1, function2));
    }

    @Test
    public void testDivideInconsistentFunctionsExceptionForDifferentCount() {
        // тестирование исключения при делении функций с разным количеством точек
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] xValues2 = {1.0, 2.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues2, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertThrows(InconsistentFunctionsException.class, () -> service.divide(function1, function2));
    }

    @Test
    public void testMultiplyInconsistentFunctionsExceptionForDifferentX() {
        // тестирование исключения при умножении функций с разными x-координатами
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] xValues2 = {1.0, 2.5, 3.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0, 6.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues2, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertThrows(InconsistentFunctionsException.class, () -> service.multiply(function1, function2));
    }

    @Test
    public void testDivideInconsistentFunctionsExceptionForDifferentX() {
        // тестирование исключения при делении функций с разными x-координатами
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] xValues2 = {1.0, 2.5, 3.0};
        double[] yValues1 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0, 6.0};

        TabulatedFunction function1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction function2 = new ArrayTabulatedFunction(xValues2, yValues2);
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();
        assertThrows(InconsistentFunctionsException.class, () -> service.divide(function1, function2));
    }
}
