package functions;

public class CompositeFunction implements MathFunction {
    private final MathFunction firstFunction;
    private final MathFunction secondFunction;

    public CompositeFunction(MathFunction firstFunction, MathFunction secondFunction) {
        this.firstFunction = firstFunction;
        this.secondFunction = secondFunction;
    }

    @Override
    public double apply(double x) {
        //h(x) = g(f(x)) - сначала firstFunction, затем secondFunction
        double intermediateResult = firstFunction.apply(x);
        return secondFunction.apply(intermediateResult);
    }

    public MathFunction getFirstFunction() {
        return firstFunction;
    }

    public MathFunction getSecondFunction() {
        return secondFunction;
    }
}
