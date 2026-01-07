package functions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqrFunctionTest {

    @Test
    void apply1() {
        SqrFunction x = new SqrFunction();
        double result = x.apply(3.0);
        assertEquals(9.0, result, 0.0001);
    }

    @Test
    void apply2() {
        SqrFunction x = new SqrFunction();
        double result = x.apply(-10.0);
        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void apply3() {
        SqrFunction x = new SqrFunction();
        double result = x.apply(0.5);
        assertEquals(0.25, result, 0.0001);
    }
}