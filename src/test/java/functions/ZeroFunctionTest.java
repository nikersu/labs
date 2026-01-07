package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ZeroFunctionTest {

    @Test
    public void testApplyAlwaysReturnsZero() {
        ZeroFunction zero = new ZeroFunction();
        assertEquals(0.0, zero.apply(0.0), 0.0001);
        assertEquals(0.0, zero.apply(1.0), 0.0001);
        assertEquals(0.0, zero.apply(-1.0), 0.0001);
    }

    @Test
    public void testGetConstantReturnsZero() {
        ZeroFunction zero = new ZeroFunction();
        assertEquals(0.0, zero.getConstant(), 0.0001);
    }

    @Test
    public void testInheritance() {
        ZeroFunction zero = new ZeroFunction();
        assertTrue(zero instanceof ConstantFunction);
    }
}