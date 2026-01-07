package functions;

import exceptions.ArrayIsNotSortedException;
import exceptions.DifferentLengthOfArraysException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExceptionsTest {

    @Test
    public void testCheckLengthIsTheSame() {
        // одинаковые длины - не должно быть исключения
        double[] x1 = {1.0, 2.0, 3.0};
        double[] y1 = {4.0, 5.0, 6.0};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkLengthIsTheSame(x1, y1));

        // разные длины - должно быть исключение
        double[] x2 = {1.0, 2.0, 3.0};
        double[] y2 = {4.0, 5.0};
        assertThrows(DifferentLengthOfArraysException.class,
                () -> AbstractTabulatedFunction.checkLengthIsTheSame(x2, y2));
    }
    @Test
    public void testCheckSorted() {
        // отсортированный массив - не должно быть исключения
        double[] sorted = {1.0, 2.0, 3.0, 4.0};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkSorted(sorted));

        // неотсортированный массив - должно быть исключение
        double[] notSorted = {1.0, 3.0, 2.0, 4.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> AbstractTabulatedFunction.checkSorted(notSorted));
    }
    @Test
    public void testArrayTabulatedFunctionConstructorExceptions() {
        // разные длины массивов
        double[] x1 = {1.0, 2.0, 3.0};
        double[] y1 = {4.0, 5.0};
        assertThrows(IllegalArgumentException.class,
                () -> new ArrayTabulatedFunction(x1, y1));

        // неотсортированный массив
        double[] x2 = {3.0, 1.0, 2.0};
        double[] y2 = {4.0, 5.0, 6.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> new ArrayTabulatedFunction(x2, y2));
    }
    @Test
    public void testLinkedListTabulatedFunctionConstructorExceptions() {
        // разные длины массивов
        double[] x1 = {1.0, 2.0, 3.0};
        double[] y1 = {4.0, 5.0};
        assertThrows(DifferentLengthOfArraysException.class,
                () -> new LinkedListTabulatedFunction(x1, y1));
        // неотсортированный массив
        double[] x2 = {3.0, 1.0, 2.0};
        double[] y2 = {4.0, 5.0, 6.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> new LinkedListTabulatedFunction(x2, y2));
    }
    @Test
    public void testInterpolationException() {
        // функция для тестирования интерполяции
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // корректная интерполяция - не должно быть исключения
        assertDoesNotThrow(() -> function.apply(2.5));
    }
    @Test
    public void testIllegalArgumentExceptionForMinLength() {
        // слишком маленький массив
        double[] shortX = {1.0};
        double[] shortY = {2.0};
        assertThrows(IllegalArgumentException.class,
                () -> new ArrayTabulatedFunction(shortX, shortY));
        assertThrows(IllegalArgumentException.class,
                () -> new LinkedListTabulatedFunction(shortX, shortY));
    }
    @Test
    public void testCheckLengthIsTheSameMethod() {
        // массивы одинаковой длины - исключения не должно быть
        double[] xValues1 = {1.0, 2.0, 3.0};
        double[] yValues1 = {4.0, 5.0, 6.0};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkLengthIsTheSame(xValues1, yValues1));
        // массивы разной длины - должно быть исключение
        double[] xValues2 = {1.0, 2.0, 3.0};
        double[] yValues2 = {4.0, 5.0};
        assertThrows(DifferentLengthOfArraysException.class,
                () -> AbstractTabulatedFunction.checkLengthIsTheSame(xValues2, yValues2));
        // первый массив короче
        double[] xValues3 = {1.0, 2.0};
        double[] yValues3 = {4.0, 5.0, 6.0};
        assertThrows(DifferentLengthOfArraysException.class,
                () -> AbstractTabulatedFunction.checkLengthIsTheSame(xValues3, yValues3));
        // пустые массивы одинаковой длины
        double[] xValues4 = {};
        double[] yValues4 = {};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkLengthIsTheSame(xValues4, yValues4));
    }
    @Test
    public void testCheckSortedMethod() {
        // отсортированный массив - исключения не должно быть
        double[] sorted1 = {1.0, 2.0, 3.0, 4.0};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkSorted(sorted1));
        // неотсортированный массив - должно быть исключение
        double[] notSorted = {1.0, 3.0, 2.0, 4.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> AbstractTabulatedFunction.checkSorted(notSorted));
        // массив с одинаковыми значениями - должно быть исключение
        double[] duplicates = {1.0, 2.0, 2.0, 3.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> AbstractTabulatedFunction.checkSorted(duplicates));
        // Убывающий массив - должно быть исключение
        double[] descending = {4.0, 3.0, 2.0, 1.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> AbstractTabulatedFunction.checkSorted(descending));
        // массив из одного элемента должен быть отсортирован
        double[] single = {1.0};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkSorted(single));
        // пустой массив должен быть отсортирован
        double[] empty = {};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkSorted(empty));
    }
    @Test
    public void testConstructorExceptionsWithDetailedScenarios() {
        // (должна быть первая ошибка - разная длина)
        double[] xValues1 = {3.0, 1.0, 2.0}; // не отсортирован
        double[] yValues1 = {4.0, 5.0};      // разная длина
        assertThrows(IllegalArgumentException.class,
                () -> new ArrayTabulatedFunction(xValues1, yValues1));
        // одинаковая длина, но массив не отсортирован
        double[] xValues2 = {3.0, 1.0, 2.0};
        double[] yValues2 = {4.0, 5.0, 6.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> new ArrayTabulatedFunction(xValues2, yValues2));
    }
    @Test
    public void testInterpolationExceptionInLinkedListFunction() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);
        // для связного списка
        assertDoesNotThrow(() -> linkedListFunction.apply(2.5));
        assertDoesNotThrow(() -> linkedListFunction.apply(0.5));
        assertDoesNotThrow(() -> linkedListFunction.apply(4.5));
    }
}