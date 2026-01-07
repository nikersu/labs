package operations;

import functions.SqrFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import functions.MathFunction;

public class SteppingDifferentialOperatorTest {

    private static final double DELTA = 1e-10;

    @Test
    public void testLeftSteppingDifferentialOperator() {
        LeftSteppingDifferentialOperator operator = new LeftSteppingDifferentialOperator(0.001);
        MathFunction derivative = operator.derive(new SqrFunction());
        assertEquals(4.0, derivative.apply(2.0), 0.01);
        assertEquals(6.0, derivative.apply(3.0), 0.01);
    }

    @Test
    public void testRightSteppingDifferentialOperator() {
        RightSteppingDifferentialOperator operator = new RightSteppingDifferentialOperator(0.001);
        MathFunction derivative = operator.derive(new SqrFunction());
        assertEquals(4.0, derivative.apply(2.0), 0.01);
        assertEquals(6.0, derivative.apply(3.0), 0.01);
    }

    @Test
    public void testStepValidation() {
        // Проверка некорректного шага в конструкторе
        assertThrows(IllegalArgumentException.class, () -> new LeftSteppingDifferentialOperator(0));
        assertThrows(IllegalArgumentException.class, () -> new LeftSteppingDifferentialOperator(-1));
        assertThrows(IllegalArgumentException.class, () -> new LeftSteppingDifferentialOperator(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new LeftSteppingDifferentialOperator(Double.POSITIVE_INFINITY));

        // Проверка некорректного шага в сеттере
        LeftSteppingDifferentialOperator operator = new LeftSteppingDifferentialOperator(0.1);
        assertThrows(IllegalArgumentException.class, () -> operator.setStep(0));
        assertThrows(IllegalArgumentException.class, () -> operator.setStep(-0.5));
        assertThrows(IllegalArgumentException.class, () -> operator.setStep(Double.NaN));
    }

    @Test
    public void testGetSetStep() {
        // Проверка геттера и сеттера
        LeftSteppingDifferentialOperator operator = new LeftSteppingDifferentialOperator(0.1);
        assertEquals(0.1, operator.getStep(), DELTA);

        operator.setStep(0.2);
        assertEquals(0.2, operator.getStep(), DELTA);
    }
    @Test
    public void testDifferentStepSizes() {
        // Проверка с разными размерами шага
        SqrFunction function = new SqrFunction();
        LeftSteppingDifferentialOperator operator1 = new LeftSteppingDifferentialOperator(0.01);
        LeftSteppingDifferentialOperator operator2 = new LeftSteppingDifferentialOperator(0.0001);
        MathFunction derivative1 = operator1.derive(function);
        MathFunction derivative2 = operator2.derive(function);

        // Более мелкий шаг должен давать более точный результат
        double result1 = derivative1.apply(2.0);
        double result2 = derivative2.apply(2.0);
        // Оба результата должны быть близки к 4.0
        assertEquals(4.0, result1, 0.1);
        assertEquals(4.0, result2, 0.01);
    }
}