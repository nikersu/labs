package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AbstractTabulatedFunctionTest {

    @Test
    public void testToStringLinkedListTabulatedFunction() {
        double[] xValues = {0.0, 0.5, 1.0};
        double[] yValues = {0.0, 0.25, 1.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        
        String expected = "LinkedListTabulatedFunction size = 3\n[0.0; 0.0]\n[0.5; 0.25]\n[1.0; 1.0]";
        assertEquals(expected, function.toString());
    }

    @Test
    public void testToStringArrayTabulatedFunction() {
        double[] xValues = {0.0, 0.5, 1.0};
        double[] yValues = {0.0, 0.25, 1.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        
        String expected = "ArrayTabulatedFunction size = 3\n[0.0; 0.0]\n[0.5; 0.25]\n[1.0; 1.0]";
        assertEquals(expected, function.toString());
    }

    @Test
    public void testToStringWithDifferentValues() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {2.0, 4.0, 6.0, 8.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        
        String expected = "ArrayTabulatedFunction size = 4\n[1.0; 2.0]\n[2.0; 4.0]\n[3.0; 6.0]\n[4.0; 8.0]";
        assertEquals(expected, function.toString());
    }

    @Test
    public void testToStringWithNegativeValues() {
        double[] xValues = {-1.0, 0.0, 1.0};
        double[] yValues = {-2.0, 0.0, 2.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        
        String expected = "LinkedListTabulatedFunction size = 3\n[-1.0; -2.0]\n[0.0; 0.0]\n[1.0; 2.0]";
        assertEquals(expected, function.toString());
    }
}

