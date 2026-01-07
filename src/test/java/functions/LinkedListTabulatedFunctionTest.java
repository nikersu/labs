package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

class LinkedListTabulatedFunctionTest {
    @Test
    public void testConstructorWithArrays() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {2.0, 4.0, 6.0, 8.0};

        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(4, function.getCount());
        assertEquals(1.0, function.leftBound(), 0.0001);
        assertEquals(4.0, function.rightBound(), 0.0001);
    }

    @Test
    public void testConstructorWithFunction() {
        MathFunction source = new SqrFunction();
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(source, 0.0, 4.0, 5);

        assertEquals(5, function.getCount());
        assertEquals(0.0, function.getX(0), 0.0001);
        assertEquals(4.0, function.getX(4), 0.0001);
        assertEquals(0.0, function.getY(0), 0.0001);
        assertEquals(16.0, function.getY(4), 0.0001);
    }

    @Test
    public void testConstructorWithReversedBounds() {
        MathFunction source = x -> x + 1;
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(source, 5.0, 1.0, 5);

        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(5.0, function.getX(4), 0.0001);
    }

    @Test
    public void testConstructorWithEqualBounds() {
        MathFunction source = new SqrFunction();
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(source, 3.0, 3.0, 4);

        assertEquals(4, function.getCount());
        assertEquals(3.0, function.getX(0), 0.0001);
        assertEquals(3.0, function.getX(3), 0.0001);
        assertEquals(9.0, function.getY(0), 0.0001);
        assertEquals(9.0, function.getY(3), 0.0001);
    }

    @Test
    public void testGetXGetY() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(2.0, function.getX(1), 0.0001);
        assertEquals(3.0, function.getX(2), 0.0001);

        assertEquals(10.0, function.getY(0), 0.0001);
        assertEquals(20.0, function.getY(1), 0.0001);
        assertEquals(30.0, function.getY(2), 0.0001);
    }

    @Test
    public void testSetY() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.setY(1, 25.0);
        assertEquals(25.0, function.getY(1), 0.0001);
    }

    @Test
    public void testIndexOfX() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(0, function.indexOfX(1.0));
        assertEquals(2, function.indexOfX(3.0));
        assertEquals(-1, function.indexOfX(1.5));
        assertEquals(-1, function.indexOfX(5.0));
    }

    @Test
    public void testIndexOfY() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(0, function.indexOfY(1.0));
        assertEquals(2, function.indexOfY(9.0));
        assertEquals(-1, function.indexOfY(5.0));
    }

    @Test
    public void testFloorIndexOfX() {
        double[] xValues = {1.0, 3.0, 5.0, 7.0};
        double[] yValues = {1.0, 9.0, 25.0, 49.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        // Теперь при x < leftBound должно выбрасываться исключение
        assertThrows(IllegalArgumentException.class, () -> function.floorIndexOfX(0.5));

        assertEquals(0, function.floorIndexOfX(1.0));  // Равно первому
        assertEquals(0, function.floorIndexOfX(2.0));  // Между 1 и 3
        assertEquals(1, function.floorIndexOfX(4.0));  // Между 3 и 5
        assertEquals(2, function.floorIndexOfX(6.0));  // Между 5 и 7
        assertEquals(3, function.floorIndexOfX(7.0));  // Равно последнему - теперь должно возвращать 3
        assertEquals(4, function.floorIndexOfX(8.0));  // Больше всех
    }

    @Test
    public void testApplyExactMatch() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(1.0, function.apply(1.0), 0.0001);
        assertEquals(9.0, function.apply(3.0), 0.0001);
    }

    @Test
    public void testApplyInterpolation() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(6.5, function.apply(2.5), 0.0001);
    }

    @Test
    public void testApplyExtrapolationLeft() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(-2.0, function.apply(0.0), 0.0001);
    }

    @Test
    public void testApplyExtrapolationRight() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(23.0, function.apply(5.0), 0.0001);
    }

    @Test
    public void testInsertAtBeginning() {
        double[] xValues = {2.0, 3.0, 4.0};
        double[] yValues = {4.0, 9.0, 16.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.insert(1.0, 1.0);

        assertEquals(4, function.getCount());
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(1.0, function.getY(0), 0.0001);
        assertEquals(2.0, function.getX(1), 0.0001);
    }

    @Test
    public void testInsertAtEnd() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.insert(4.0, 16.0);

        assertEquals(4, function.getCount());
        assertEquals(4.0, function.getX(3), 0.0001);
        assertEquals(16.0, function.getY(3), 0.0001);
        assertEquals(3.0, function.getX(2), 0.0001);
    }

    @Test
    public void testInsertInMiddle() {
        double[] xValues = {1.0, 3.0, 5.0};
        double[] yValues = {1.0, 9.0, 25.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.insert(2.0, 4.0);

        assertEquals(4, function.getCount());
        assertEquals(2.0, function.getX(1), 0.0001);
        assertEquals(4.0, function.getY(1), 0.0001);
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(3.0, function.getX(2), 0.0001);
    }

    @Test
    public void testInsertReplaceExisting() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.insert(2.0, 5.0);

        assertEquals(3, function.getCount()); // Количество не изменилось
        assertEquals(2.0, function.getX(1), 0.0001);
        assertEquals(5.0, function.getY(1), 0.0001); // Y изменился
    }

    @Test
    public void testRemoveFirst() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.remove(0);

        assertEquals(2, function.getCount());
        assertEquals(2.0, function.getX(0), 0.0001);
        assertEquals(3.0, function.getX(1), 0.0001);
    }

    @Test
    public void testRemoveLast() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.remove(2);

        assertEquals(2, function.getCount());
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(2.0, function.getX(1), 0.0001);
    }

    @Test
    public void testRemoveMiddle() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.remove(1);

        assertEquals(2, function.getCount());
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(3.0, function.getX(1), 0.0001);
    }

    @Test
    public void testComplexInsertAndRemove() {
        // Начинаем с минимального количества точек (2)
        double[] xValues = {1.0, 3.0};
        double[] yValues = {1.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        // Добавляем элемент в середину
        function.insert(2.0, 4.0);

        assertEquals(3, function.getCount());
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(2.0, function.getX(1), 0.0001);
        assertEquals(3.0, function.getX(2), 0.0001);

        // Удаляем средний элемент (теперь останется 2 точки - минимум)
        function.remove(1);

        assertEquals(2, function.getCount());
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(3.0, function.getX(1), 0.0001);

        // Добавляем новый элемент в середину
        function.insert(2.0, 4.0);

        assertEquals(3, function.getCount());
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(2.0, function.getX(1), 0.0001);
        assertEquals(3.0, function.getX(2), 0.0001);
    }

    @Test
    public void testConstructorWithSinglePointThrowsException() {
        double[] x = {1.0};
        double[] y = {2.0};

        assertThrows(IllegalArgumentException.class, () -> {
            new LinkedListTabulatedFunction(x, y);
        });
    }

    @Test
    public void testConstructorWithFunctionSinglePointThrowsException() {
        MathFunction source = new SqrFunction();

        assertThrows(IllegalArgumentException.class, () -> {
            new LinkedListTabulatedFunction(source, 0.0, 4.0, 1);
        });
    }

    @Test
    public void testGetXWithNegativeIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> {
            function.getX(-1);
        });
    }

    @Test
    public void testGetXWithIndexEqualToCountThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> {
            function.getX(3);
        });
    }

    @Test
    public void testSetYWithInvalidIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> {
            function.setY(5, 10.0);
        });
    }

    @Test
    public void testFloorIndexOfXLessThanLeftBoundThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> {
            function.floorIndexOfX(0.5);
        });
    }

    @Test
    public void testRemoveWithInvalidIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> {
            function.remove(3);
        });
    }

    @Test
    public void testRemoveWhenOnlyTwoPointsThrowsException() {
        double[] x = {1.0, 2.0};
        double[] y = {1.0, 4.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        assertThrows(IllegalStateException.class, () -> {
            function.remove(0); // попытка удалить при count=2
        });
    }

    @Test
    public void testValidOperationsStillWorkAfterExceptionHandling() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(x, y);

        // Проверяем, что валидные операции все еще работают после добавления проверок
        assertEquals(2.0, function.getX(1), 1e-10);
        assertEquals(4.0, function.getY(1), 1e-10);
        assertEquals(1, function.indexOfX(2.0));
        assertEquals(1, function.floorIndexOfX(2.5));

        // Исправляем ожидаемое значение - при x=2.5 между точками (2,4) и (3,9)
        // Линейная интерполяция: y = 4 + (9-4)/(3-2) * (2.5-2) = 4 + 5 * 0.5 = 6.5
        assertEquals(6.5, function.apply(2.5), 1e-10);
    }

    @Test
    public void testIteratorWhileLoop() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        Iterator<Point> iterator = function.iterator();
        int count = 0;

        // Способ 1: цикл while
        while (iterator.hasNext()) {
            Point point = iterator.next();
            assertEquals(xValues[count], point.x, 0.0001);
            assertEquals(yValues[count], point.y, 0.0001);
            count++;
        }

        assertEquals(3, count);
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    public void testIteratorForEachLoop() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        int count = 0;

        // Способ 2: цикл for-each
        for (Point point : function) {
            assertEquals(xValues[count], point.x, 0.0001);
            assertEquals(yValues[count], point.y, 0.0001);
            count++;
        }

        assertEquals(3, count);
    }

    @Test
    public void testIteratorOnEmptyFunction() {
        // Создаем функцию с минимальным количеством точек (2)
        double[] xValues = {1.0, 2.0};
        double[] yValues = {1.0, 4.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        Iterator<Point> iterator = function.iterator();

        // Должны быть 2 точки
        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }
}
