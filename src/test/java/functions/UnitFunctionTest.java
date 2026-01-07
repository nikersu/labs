package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnitFunctionTest {

    @Test
    public void testApplyAlwaysReturnsOne() {
        UnitFunction unit = new UnitFunction();
        assertEquals(1.0, unit.apply(0.0), 0.0001);
        assertEquals(1.0, unit.apply(1.0), 0.0001);
        assertEquals(1.0, unit.apply(-1.0), 0.0001);
    }

    @Test
    public void testGetConstantReturnsOne() {
        UnitFunction unit = new UnitFunction();
        assertEquals(1.0, unit.getConstant(), 0.0001);
    }

    @Test
    public void testInheritance() {
        UnitFunction unit = new UnitFunction();
        assertTrue(unit instanceof ConstantFunction);
    }
}