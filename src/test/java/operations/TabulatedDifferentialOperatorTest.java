package operations;

import functions.ArrayTabulatedFunction;
import functions.LinkedListTabulatedFunction;
import functions.TabulatedFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import concurrent.SynchronizedTabulatedFunction;

public class TabulatedDifferentialOperatorTest {

    @Test
    public void testDefaultConstructor() {
        // Тест конструктора без аргументов
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        assertInstanceOf(ArrayTabulatedFunctionFactory.class, operator.getFactory());
    }

    @Test
    public void testConstructorWithFactory() {
        // Тест конструктора с фабрикой
        LinkedListTabulatedFunctionFactory factory = new LinkedListTabulatedFunctionFactory();
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(factory);
        assertEquals(factory, operator.getFactory());
    }

    @Test
    public void testFactoryGetterAndSetter() {
        // Тест геттера и сеттера фабрики
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        assertInstanceOf(ArrayTabulatedFunctionFactory.class, operator.getFactory());

        LinkedListTabulatedFunctionFactory newFactory = new LinkedListTabulatedFunctionFactory();
        operator.setFactory(newFactory);
        assertEquals(newFactory, operator.getFactory());
    }

    @Test
    public void testDeriveLinearFunctionWithArrayFactory() {
        // Тест производной линейной функции f(x) = x
        // Производная должна быть константой 1
        double[] xValues = {0.0, 1.0, 2.0, 3.0, 4.0};
        double[] yValues = {0.0, 1.0, 2.0, 3.0, 4.0};

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(new ArrayTabulatedFunctionFactory());
        TabulatedFunction derivative = operator.derive(function);

        assertEquals(5, derivative.getCount());
        // Проверяем, что производная близка к 1 для всех точек
        for (int i = 0; i < derivative.getCount(); i++) {
            assertEquals(1.0, derivative.getY(i), 1e-10, "Derivative of f(x)=x should be 1 at index " + i);
        }
    }

    @Test
    public void testDeriveLinearFunctionWithLinkedListFactory() {
        // Тест производной линейной функции с фабрикой LinkedList
        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues = {0.0, 1.0, 2.0, 3.0};

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(new LinkedListTabulatedFunctionFactory());
        TabulatedFunction derivative = operator.derive(function);

        assertInstanceOf(LinkedListTabulatedFunction.class, derivative);
        assertEquals(4, derivative.getCount());
        for (int i = 0; i < derivative.getCount(); i++) {
            assertEquals(1.0, derivative.getY(i), 1e-10);
        }
    }

    @Test
    public void testDeriveQuadraticFunction() {
        // Тест производной квадратичной функции f(x) = x²
        double[] xValues = {0.0, 1.0, 2.0, 3.0, 4.0};
        double[] yValues = {0.0, 1.0, 4.0, 9.0, 16.0}; // x²

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction derivative = operator.derive(function);

        assertEquals(5, derivative.getCount());
        // Первая точка: forward difference
        assertEquals(1.0, derivative.getY(0), 1e-10);

        // Внутренние точки: central difference
        // f'(1) ≈ (4-0)/(2-0) = 2
        assertEquals(2.0, derivative.getY(1), 1e-10);
        // f'(2) ≈ (9-1)/(3-1) = 4
        assertEquals(4.0, derivative.getY(2), 1e-10);
        // f'(3) ≈ (16-4)/(4-2) = 6
        assertEquals(6.0, derivative.getY(3), 1e-10);

        // Последняя точка: backward difference
        // f'(4) ≈ (16-9)/(4-3) = 7 (приблизительно)
        assertEquals(7.0, derivative.getY(4), 1e-10);
    }

    @Test
    public void testDeriveConstantFunction() {
        // Тест производной константной функции f(x) = 5
        // Производная должна быть 0
        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues = {5.0, 5.0, 5.0, 5.0};

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction derivative = operator.derive(function);

        assertEquals(4, derivative.getCount());
        // Производная константы должна быть 0
        for (int i = 0; i < derivative.getCount(); i++) {
            assertEquals(0.0, derivative.getY(i), 1e-10, "Derivative of constant function should be 0 at index " + i);
        }
    }

    @Test
    public void testDeriveWithTwoPoints() {
        // Тест с минимальным количеством точек (2 точки)
        double[] xValues = {0.0, 1.0};
        double[] yValues = {0.0, 2.0};

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction derivative = operator.derive(function);

        assertEquals(2, derivative.getCount());
        // Первая точка: forward difference = (2-0)/(1-0) = 2
        assertEquals(2.0, derivative.getY(0), 1e-10);
        // Последняя точка: backward difference = (2-0)/(1-0) = 2
        assertEquals(2.0, derivative.getY(1), 1e-10);
    }

    @Test
    public void testDeriveWithLinkedListFunction() {
        // Тест дифференцирования функции типа LinkedListTabulatedFunction
        double[] xValues = {0.0, 1.0, 2.0};
        double[] yValues = {0.0, 2.0, 4.0}; // f(x) = 2x

        TabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(new ArrayTabulatedFunctionFactory());
        TabulatedFunction derivative = operator.derive(function);

        assertEquals(3, derivative.getCount());
        // Производная f(x) = 2x должна быть 2
        assertEquals(2.0, derivative.getY(0), 1e-10); // forward difference
        assertEquals(2.0, derivative.getY(1), 1e-10); // central difference
        assertEquals(2.0, derivative.getY(2), 1e-10); // backward difference
    }

    @Test
    public void testDerivePreservesXValues() {
        // Тест, что x-координаты сохраняются
        double[] xValues = {1.0, 2.5, 3.7, 5.0};
        double[] yValues = {1.0, 2.5, 3.7, 5.0};

        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction derivative = operator.derive(function);

        assertEquals(4, derivative.getCount());
        for (int i = 0; i < derivative.getCount(); i++) {
            assertEquals(xValues[i], derivative.getX(i), 1e-10, "X values should be preserved at index " + i);
        }
    }

    @Test
    void testDeriveSynchronouslyWithArrayFunction() {
        // создаём тестовую функцию на основе массива
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction derivedNormal = operator.derive(function);
        TabulatedFunction derivedSync = operator.deriveSynchronously(function);

        // проверка, что результаты одинаковые
        assertEquals(derivedNormal.getCount(), derivedSync.getCount());
        for (int i = 0; i < derivedNormal.getCount(); i++) {
            assertEquals(derivedNormal.getX(i), derivedSync.getX(i), 1e-9);
            assertEquals(derivedNormal.getY(i), derivedSync.getY(i), 1e-9);
        }
    }

    @Test
    void testDeriveSynchronouslyWithLinkedListFunction() {
        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues = {0.0, 1.0, 8.0, 27.0};
        TabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        TabulatedFunction derivedNormal = operator.derive(function);
        TabulatedFunction derivedSync = operator.deriveSynchronously(function);
        assertEquals(derivedNormal.getCount(), derivedSync.getCount());
        for (int i = 0; i < derivedNormal.getCount(); i++) {
            assertEquals(derivedNormal.getX(i), derivedSync.getX(i), 1e-9);
            assertEquals(derivedNormal.getY(i), derivedSync.getY(i), 1e-9);
        }
    }

    @Test
    void testDeriveSynchronouslyWithAlreadySynchronizedFunction() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {2.0, 4.0, 6.0};
        TabulatedFunction originalFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(originalFunction);
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
        TabulatedFunction derivedSync = operator.deriveSynchronously(syncFunction);

        // проверка на корректность вычислений производной
        assertEquals(3, derivedSync.getCount());
        assertEquals(2.0, derivedSync.getY(0), 1e-9);
        assertEquals(2.0, derivedSync.getY(1), 1e-9);
        assertEquals(2.0, derivedSync.getY(2), 1e-9);
    }

    @Test
    void testDeriveSynchronouslyWithDifferentFactory() {
        // Тестируем с другой фабрикой
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 2.0, 3.0};
        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // Используем фабрику для связных списков
        TabulatedDifferentialOperator operator =
                new TabulatedDifferentialOperator(new LinkedListTabulatedFunctionFactory());

        TabulatedFunction derivedSync = operator.deriveSynchronously(function);

        // Проверяем, что функция создана правильной фабрикой
        assertInstanceOf(LinkedListTabulatedFunction.class, derivedSync);
        assertEquals(3, derivedSync.getCount());
    }
}