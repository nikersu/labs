package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AndThenMethodTest {

    @Test
    public void testSimpleAndThenChain() {
        // f(x) = x + 1, g(x) = x * 2
        // f.andThen(g) = g(f(x)) = (x + 1) * 2
        MathFunction f = x -> x + 1;
        MathFunction g = x -> x * 2;

        MathFunction composite = f.andThen(g);

        assertEquals(6.0, composite.apply(2.0), 0.0001);  // (2 + 1) * 2 = 6
        assertEquals(8.0, composite.apply(3.0), 0.0001);  // (3 + 1) * 2 = 8
    }

    @Test
    public void testCorrectOrderWithAndThen() {
        // f(x) = x + 2, g(x) = x * 3
        // f.andThen(g) = g(f(x)) = (x + 2) * 3
        MathFunction f = x -> x + 2;
        MathFunction g = x -> x * 3;

        MathFunction composite = f.andThen(g);

        assertEquals(12.0, composite.apply(2.0), 0.0001);  // (2 + 2) * 3 = 12
        assertEquals(15.0, composite.apply(3.0), 0.0001);  // (3 + 2) * 3 = 15
    }

    @Test
    public void testThreeFunctionChain() {
        // h(x) = x + 1, g(x) = x * 2, f(x) = x²
        // h.andThen(g).andThen(f) = f(g(h(x))) = ((x + 1) * 2)²

        MathFunction h = x -> x + 1;
        MathFunction g = x -> x * 2;
        MathFunction f = new SqrFunction();

        MathFunction composite = h.andThen(g).andThen(f);

        assertEquals(4.0, composite.apply(0.0), 0.0001);   // ((0 + 1) * 2)² = 4
        assertEquals(16.0, composite.apply(1.0), 0.0001);  // ((1 + 1) * 2)² = 16
        assertEquals(36.0, composite.apply(2.0), 0.0001);  // ((2 + 1) * 2)² = 36
    }

    @Test
    public void testChainWithConstantFunction() {
        // f(x) = x², g(x) = 5 (константа), h(x) = x + 1
        // f.andThen(g).andThen(h) = h(g(f(x))) = h(g(x²)) = h(5) = 6 (всегда)

        MathFunction f = new SqrFunction();
        MathFunction g = new ConstantFunction(5.0);
        MathFunction h = x -> x + 1;

        MathFunction composite = f.andThen(g).andThen(h);

        // Для любого x результат всегда 6.0
        assertEquals(6.0, composite.apply(0.0), 0.0001);
        assertEquals(6.0, composite.apply(1.0), 0.0001);
        assertEquals(6.0, composite.apply(10.0), 0.0001);
        assertEquals(6.0, composite.apply(-5.0), 0.0001);
    }

    @Test
    public void testChainWithZeroAndUnitFunctions() {
        // zero.andThen(unit).andThen(sqr) = sqr(unit(zero(x))) = sqr(unit(0)) = sqr(1) = 1 (всегда)

        MathFunction zero = new ZeroFunction();
        MathFunction unit = new UnitFunction();
        MathFunction sqr = new SqrFunction();

        MathFunction composite = zero.andThen(unit).andThen(sqr);

        // Для любого x результат всегда 1.0
        assertEquals(1.0, composite.apply(0.0), 0.0001);
        assertEquals(1.0, composite.apply(100.0), 0.0001);
        assertEquals(1.0, composite.apply(-50.0), 0.0001);
    }

    @Test
    public void testLongChain() {
        // f1(x) = x + 1
        // f2(x) = x * 2
        // f3(x) = x²
        // f4(x) = x - 5
        // f5(x) = x / 2
        // f1.andThen(f2).andThen(f3).andThen(f4).andThen(f5) = f5(f4(f3(f2(f1(x))))) = ((((x + 1) * 2)²) - 5) / 2

        MathFunction f1 = x -> x + 1;
        MathFunction f2 = x -> x * 2;
        MathFunction f3 = new SqrFunction();
        MathFunction f4 = x -> x - 5;
        MathFunction f5 = x -> x / 2;

        MathFunction composite = f1.andThen(f2).andThen(f3).andThen(f4).andThen(f5);

        // Для x = 1: ((((1 + 1) * 2)²) - 5) / 2 = (((2 * 2)²) - 5) / 2 = ((4²) - 5) / 2 = (16 - 5) / 2 = 11 / 2 = 5.5
        assertEquals(5.5, composite.apply(1.0), 0.0001);

        // Для x = 2: ((((2 + 1) * 2)²) - 5) / 2 = (((3 * 2)²) - 5) / 2 = ((6²) - 5) / 2 = (36 - 5) / 2 = 31 / 2 = 15.5
        assertEquals(15.5, composite.apply(2.0), 0.0001);
    }

    @Test
    public void testChainWithExistingCompositeFunction() {
        // Создаем композитную функцию: inner(x) = (x + 1) * 2
        CompositeFunction inner = new CompositeFunction(
                x -> x + 1,
                x -> x * 2
        );

        // inner.andThen(sqr).andThen(x -> x + 10) = ((x + 1) * 2)² + 10

        MathFunction composite = inner.andThen(new SqrFunction()).andThen(x -> x + 10);

        assertEquals(14.0, composite.apply(0.0), 0.0001);  // ((0 + 1) * 2)² + 10 = 4 + 10 = 14
        assertEquals(26.0, composite.apply(1.0), 0.0001);  // ((1 + 1) * 2)² + 10 = 16 + 10 = 26
    }

    @Test
    public void testImmediateApplication() {
        // Применяем цепочку сразу без сохранения ссылки
        double result = new SqrFunction()
                .andThen(x -> x + 5)
                .andThen(x -> x * 2)
                .apply(3.0);

        // (3² + 5) * 2 = (9 + 5) * 2 = 14 * 2 = 28
        assertEquals(28.0, result, 0.0001);
    }

    @Test
    public void testChainReturnsCompositeFunction() {
        MathFunction f = new SqrFunction();
        MathFunction g = x -> x + 1;

        MathFunction result = f.andThen(g);

        // Проверяем, что результат действительно CompositeFunction
        CompositeFunction composite = assertInstanceOf(CompositeFunction.class, result);

        // Проверяем, что можем получить исходные функции
        assertEquals(f, composite.getFirstFunction());
        assertEquals(g, composite.getSecondFunction());
    }
    @Test
    public void testComplexMathematicalChain() {
        // sin(cos(x² + 1) * 2)
        MathFunction chain = new SqrFunction()
                .andThen(x -> x + 1)
                .andThen(Math::cos)
                .andThen(x -> x * 2)
                .andThen(Math::sin);

        double result = chain.apply(1.0);
        double expected = Math.sin(Math.cos(1.0 + 1 ) * 2);

        assertEquals(expected, result, 0.0001);
    }

    @Test
    public void testReverseOrderExample() {
        // Демонстрация разницы в порядке:
        MathFunction add = x -> x + 2;
        MathFunction multiply = x -> x * 3;

        // add.andThen(multiply) = multiply(add(x)) = (x + 2) * 3
        MathFunction addThenMultiply = add.andThen(multiply);

        // multiply.andThen(add) = add(multiply(x)) = (x * 3) + 2
        MathFunction multiplyThenAdd = multiply.andThen(add);

        assertEquals(12.0, addThenMultiply.apply(2.0), 0.0001);  // (2 + 2) * 3 = 12
        assertEquals(8.0, multiplyThenAdd.apply(2.0), 0.0001);   // (2 * 3) + 2 = 8

        // Разные результаты подтверждают важность порядка!
        assertNotEquals(addThenMultiply.apply(2.0), multiplyThenAdd.apply(2.0), 0.0001);
    }
}