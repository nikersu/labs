package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConstantFunctionTest {

    @Test
    public void testApplyWithPositiveConstant() {
        ConstantFunction func = new ConstantFunction(5.0);
        assertEquals(5.0, func.apply(0.0), 0.0001);
        assertEquals(5.0, func.apply(10.0), 0.0001);
        assertEquals(5.0, func.apply(-5.0), 0.0001);
    }

    @Test
    public void testApplyWithNegativeConstant() {
        ConstantFunction func = new ConstantFunction(-3.5);
        assertEquals(-3.5, func.apply(0.0), 0.0001);
        assertEquals(-3.5, func.apply(1.0), 0.0001);
    }

    @Test
    public void testApplyWithZeroConstant() {
        ConstantFunction func = new ConstantFunction(0.0);
        assertEquals(0.0, func.apply(0.0), 0.0001);
        assertEquals(0.0, func.apply(100.0), 0.0001);
    }

    @Test
    public void testGetConstant() {
        ConstantFunction func = new ConstantFunction(7.0);
        assertEquals(7.0, func.getConstant(), 0.0001);
    }
}