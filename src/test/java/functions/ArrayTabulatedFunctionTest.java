package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import exceptions.InterpolationException;

public class ArrayTabulatedFunctionTest {

    @Test
    public void testConstructorWithArrays() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {2.0, 4.0, 6.0, 8.0};

        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertEquals(4, function.getCount());
        assertEquals(1.0, function.leftBound(), 0.0001);
        assertEquals(4.0, function.rightBound(), 0.0001);
    }

    @Test
    public void testConstructorWithFunction() {
        MathFunction source = new SqrFunction();
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(source, 0.0, 4.0, 5);

        assertEquals(5, function.getCount());
        assertEquals(0.0, function.getX(0), 0.0001);
        assertEquals(4.0, function.getX(4), 0.0001);
        assertEquals(0.0, function.getY(0), 0.0001);  // 0² = 0
        assertEquals(16.0, function.getY(4), 0.0001); // 4² = 16
    }

    @Test
    public void testConstructorWithReversedBounds() {
        MathFunction source = x -> x + 1;
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(source, 5.0, 1.0, 5);

        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(5.0, function.getX(4), 0.0001);
    }

    @Test
    public void testConstructorWithEqualBounds() {
        MathFunction source = new SqrFunction();
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(source, 3.0, 3.0, 4);

        assertEquals(4, function.getCount());
        assertEquals(3.0, function.getX(0), 0.0001);
        assertEquals(3.0, function.getX(3), 0.0001);
        assertEquals(9.0, function.getY(0), 0.0001); // 3² = 9
        assertEquals(9.0, function.getY(3), 0.0001);
    }

    @Test
    public void testGetXGetY() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

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
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        function.setY(1, 25.0);
        assertEquals(25.0, function.getY(1), 0.0001);
    }

    @Test
    public void testIndexOfX() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertEquals(0, function.indexOfX(1.0));
        assertEquals(2, function.indexOfX(3.0));
        assertEquals(-1, function.indexOfX(1.5));
        assertEquals(-1, function.indexOfX(5.0));
    }

    @Test
    public void testIndexOfY() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertEquals(0, function.indexOfY(1.0));
        assertEquals(2, function.indexOfY(9.0));
        assertEquals(-1, function.indexOfY(5.0));
    }

    @Test
    public void testFloorIndexOfX() {
        double[] xValues = {1.0, 3.0, 5.0, 7.0};
        double[] yValues = {1.0, 9.0, 25.0, 49.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertThrows(IllegalArgumentException.class, () -> function.floorIndexOfX(0.5));
        assertEquals(0, function.floorIndexOfX(1.0));  // Равно первому
        assertEquals(0, function.floorIndexOfX(2.0));  // Между 1 и 3
        assertEquals(1, function.floorIndexOfX(4.0));  // Между 3 и 5
        assertEquals(2, function.floorIndexOfX(6.0));  // Между 5 и 7
        assertEquals(3, function.floorIndexOfX(7.0));  // Равно последнему
        assertEquals(4, function.floorIndexOfX(8.0));  // Больше всех
    }

    @Test
    public void testApplyExactMatch() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertEquals(1.0, function.apply(1.0), 0.0001);
        assertEquals(9.0, function.apply(3.0), 0.0001);
    }

    @Test
    public void testApplyInterpolation() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // Интерполяция между 2 и 3: при x=2.5 должно быть 6.5
        assertEquals(6.5, function.apply(2.5), 0.0001);
    }

    @Test
    public void testApplyExtrapolationLeft() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // Экстраполяция слева: продолжение линии между 1 и 2
        // Линия через (1,1) и (2,4): y = 3x - 2
        // При x=0: y = 3*0 - 2 = -2
        assertEquals(-2.0, function.apply(0.0), 0.0001);
    }

    @Test
    public void testApplyExtrapolationRight() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // Экстраполяция справа: продолжение линии между 3 и 4
        // Линия через (3,9) и (4,16): y = 7x - 12
        // При x=5: y = 7*5 - 12 = 23
        assertEquals(23.0, function.apply(5.0), 0.0001);
    }
    @Test
    public void testIteratorWithWhileLoop() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        Iterator<Point> iterator = function.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Point point = iterator.next();
            assertEquals(xValues[index], point.x, 1e-10);
            assertEquals(yValues[index], point.y, 1e-10);
            index++;
        }
        assertEquals(3, index);
        // проверка, что после окончания элементов бросается исключение
        assertThrows(NoSuchElementException.class, iterator::next);
    }
    @Test
    public void testIteratorWithForEachLoop() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        int index = 0;
        for (Point point : function) {
            assertEquals(xValues[index], point.x, 1e-10);
            assertEquals(yValues[index], point.y, 1e-10);
            index++;
        }
        assertEquals(3, index);
    }
    @Test
    public void testConstructorWithSinglePointThrowsException() {
        double[] x = {1.0};
        double[] y = {2.0};
        assertThrows(IllegalArgumentException.class, () -> new ArrayTabulatedFunction(x, y));
    }

    @Test
    public void testConstructorWithDifferentLengthsThrowsException() {
        double[] x = {1.0, 2.0};
        double[] y = {1.0};

        assertThrows(IllegalArgumentException.class, () -> new ArrayTabulatedFunction(x, y));
    }

    @Test
    public void testConstructorWithFunctionSinglePointThrowsException() {
        MathFunction source = new SqrFunction();

        assertThrows(IllegalArgumentException.class, () -> new ArrayTabulatedFunction(source, 0.0, 4.0, 1));
    }

    @Test
    public void testGetXWithNegativeIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> function.getX(-1));
    }

    @Test
    public void testGetXWithIndexEqualToCountThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> function.getX(3));
    }

    @Test
    public void testSetYWithInvalidIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> function.setY(5, 10.0));
    }

    @Test
    public void testFloorIndexOfXLessThanLeftBoundThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> function.floorIndexOfX(0.5));
    }

    @Test
    public void testInterpolateWithInvalidFloorIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(InterpolationException.class, () -> function.interpolate(2.5, 2));
    }

    @Test
    public void testRemoveWithInvalidIndexThrowsException() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(IllegalArgumentException.class, () -> function.remove(3));
    }

    @Test
    public void testRemoveWhenOnlyTwoPointsThrowsException() {
        double[] x = {1.0, 2.0};
        double[] y = {1.0, 4.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertThrows(IllegalStateException.class, () -> {
            function.remove(0); // попытка удалить при count=2
        });
    }

    @Test
    public void testValidOperationsStillWorkAfterExceptionHandling() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(x, y);

        assertEquals(2.0, function.getX(1), 1e-10);
        assertEquals(4.0, function.getY(1), 1e-10);
        assertEquals(1, function.indexOfX(2.0));
        assertEquals(1, function.floorIndexOfX(2.5));


        assertEquals(6.5, function.apply(2.5), 1e-10);
    }

}