package operations;

import functions.MathFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SteppingDifferentialOperator implements DifferentialOperator<MathFunction> {
    private static final Logger logger = LoggerFactory.getLogger(SteppingDifferentialOperator.class);
    protected double step;
    public SteppingDifferentialOperator(double step) {
        if (step <= 0 || Double.isNaN(step) || Double.isInfinite(step)) {
            logger.error("Invalid step value in constructor: {}", step);
            throw new IllegalArgumentException("Incorrect differentiation step");
        }
        this.step = step;
    }
    public double getStep() {
        return step;
    }
    public void setStep(double step) {
        if (step <= 0 || Double.isNaN(step) || Double.isInfinite(step)) {
            logger.error("Invalid step value in setter: {}", step);
            throw new IllegalArgumentException("Incorrect differentiation step");
        }
        this.step = step;
    }
}