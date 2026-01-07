package functions;

import exceptions.ArrayIsNotSortedException;
import exceptions.DifferentLengthOfArraysException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTabulatedFunction implements TabulatedFunction {
    private static final Logger logger = LoggerFactory.getLogger(AbstractTabulatedFunction.class);
    protected abstract int floorIndexOfX(double x);

    protected abstract double extrapolateLeft(double x);

    protected abstract double extrapolateRight(double x);

    protected abstract double interpolate(double x, int floorIndex);

    protected double interpolate(double x, double leftX, double rightX, double leftY, double rightY) {
        return leftY + (rightY - leftY) * (x - leftX) / (rightX - leftX);
    }
    public static void checkLengthIsTheSame(double[] xValues, double[] yValues) {
        if (xValues.length != yValues.length) {
            logger.error("Array length mismatch: xValues={}, yValues={}", xValues.length, yValues.length);
            throw new DifferentLengthOfArraysException("Different array lengths");
        }
    }
    public static void checkSorted(double[] xValues) {
        for (int i = 1; i < xValues.length; i++) {
            if (xValues[i] <= xValues[i - 1]) {
                logger.error("Array not sorted at index {}: {} <= {}", i, xValues[i], xValues[i-1]);
                throw new ArrayIsNotSortedException("The xValues array is not sorted in ascending order");
            }
        }
    }

    @Override
    public double apply(double x) {
        if (x < leftBound()) {
            return extrapolateLeft(x);
        } else if (x > rightBound()) {
            return extrapolateRight(x);
        } else {
            int index = indexOfX(x);
            if (index != -1) {
                return getY(index);
            } else {
                int floorIndex = floorIndexOfX(x);
                return interpolate(x, floorIndex);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" size = ").append(getCount());
        for (Point point : this) {
            sb.append("\n[").append(point.x).append("; ").append(point.y).append("]");
        }
        return sb.toString();
    }
}
