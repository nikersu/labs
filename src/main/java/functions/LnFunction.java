package functions;

public class LnFunction implements MathFunction {

    @Override
    public double apply(double x) {
        if (x <= 0) {
            throw new IllegalArgumentException("Аргумент логарифма должен быть положительным");
        }
        return Math.log(x);
    }
}




