package functions;

public class NewtonMetod implements MathFunction{
    MathFunction f, df;

    public NewtonMetod(MathFunction f, MathFunction df) {
        this.f = f; this.df = df;
    }

    @Override
    public double apply(double x) {
        for (int i = 0; i < 100; i++) {
            double xNew = x - f.apply(x) / df.apply(x);
            if (Math.abs(xNew - x) < 1e-6) return xNew;
            x = xNew;
        }
        return x;
    }
}
