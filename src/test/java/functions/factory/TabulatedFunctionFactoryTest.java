package functions.factory;

import functions.ArrayTabulatedFunction;
import functions.LinkedListTabulatedFunction;
import functions.TabulatedFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TabulatedFunctionFactoryTest {

    @Test
    public void testArrayTabulatedFunctionFactory() {
        TabulatedFunctionFactory factory = new ArrayTabulatedFunctionFactory();
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction function = factory.create(xValues, yValues);

        assertInstanceOf(ArrayTabulatedFunction.class, function);
    }

    @Test
    public void testLinkedListTabulatedFunctionFactory() {
        TabulatedFunctionFactory factory = new LinkedListTabulatedFunctionFactory();
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction function = factory.create(xValues, yValues);

        assertInstanceOf(LinkedListTabulatedFunction.class, function);
    }

    @Test
    public void testFactoryCreatesValidFunction() {
        TabulatedFunctionFactory arrayFactory = new ArrayTabulatedFunctionFactory();
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction function = arrayFactory.create(xValues, yValues);

        assertEquals(3, function.getCount());
        assertEquals(1.0, function.getX(0), 1e-10);
        assertEquals(4.0, function.getY(0), 1e-10);
    }
}