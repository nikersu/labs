package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompositeFunctionTest {

    @Test
    public void testSimpleComposition() {
        // h(x) = (x²) + 1 = x² + 1
        CompositeFunction composite = new CompositeFunction(
                new SqrFunction(),      // f(x) = x²
                x -> x + 1              // g(x) = x + 1
        );

        assertEquals(1.0, composite.apply(0.0), 0.0001);  // 0² + 1 = 1
        assertEquals(2.0, composite.apply(1.0), 0.0001);  // 1² + 1 = 2
        assertEquals(5.0, composite.apply(2.0), 0.0001);  // 2² + 1 = 5
        assertEquals(10.0, composite.apply(3.0), 0.0001); // 3² + 1 = 10
    }

    @Test
    public void testCompositionWithConstantFunctions() {
        // h(x) = 5 для любого x (постоянная функция)
        CompositeFunction composite = new CompositeFunction(
                new SqrFunction(),          // f(x) = x²
                new ConstantFunction(5.0)   // g(x) = 5
        );

        assertEquals(5.0, composite.apply(0.0), 0.0001);
        assertEquals(5.0, composite.apply(1.0), 0.0001);
        assertEquals(5.0, composite.apply(10.0), 0.0001);
        assertEquals(5.0, composite.apply(-5.0), 0.0001);
    }

    @Test
    public void testCompositionWithZeroFunction() {
        // h(x) = 0 для любого x
        CompositeFunction composite = new CompositeFunction(
                new SqrFunction(),  // f(x) = x²
                new ZeroFunction()  // g(x) = 0
        );

        assertEquals(0.0, composite.apply(0.0), 0.0001);
        assertEquals(0.0, composite.apply(5.0), 0.0001);
        assertEquals(0.0, composite.apply(-3.0), 0.0001);
    }

    @Test
    public void testCompositionOfSameFunction() {
        // h(x) = (x²)² = x⁴
        CompositeFunction composite = new CompositeFunction(
                new SqrFunction(),  // f(x) = x²
                new SqrFunction()   // g(x) = x²
        );

        assertEquals(0.0, composite.apply(0.0), 0.0001);   // 0⁴ = 0
        assertEquals(1.0, composite.apply(1.0), 0.0001);   // 1⁴ = 1
        assertEquals(16.0, composite.apply(2.0), 0.0001);  // 2⁴ = 16
        assertEquals(81.0, composite.apply(3.0), 0.0001);  // 3⁴ = 81
    }

    @Test
    public void testNestedCompositeFunctions() {
        // Создаем сложную композицию: h(x) = ((x + 1)² + 5) * 2

        // Внутренняя композиция: f1(x) = (x + 1)²
        CompositeFunction innerComposite = new CompositeFunction(
                x -> x + 1,             // f(x) = x + 1
                new SqrFunction()        // g(x) = x²
        );

        // Внешняя композиция: h(x) = f1(x) + 5, затем умножить на 2
        CompositeFunction outerComposite = new CompositeFunction(
                innerComposite,          // f1(x) = (x + 1)²
                new CompositeFunction(
                        x -> x + 5,          // f2(x) = x + 5
                        x -> x * 2           // g2(x) = x * 2
                )
        );

        // Проверяем: h(0) = ((0 + 1)² + 5) * 2 = (1 + 5) * 2 = 12
        assertEquals(12.0, outerComposite.apply(0.0), 0.0001);

        // h(1) = ((1 + 1)² + 5) * 2 = (4 + 5) * 2 = 18
        assertEquals(18.0, outerComposite.apply(1.0), 0.0001);

        // h(2) = ((2 + 1)² + 5) * 2 = (9 + 5) * 2 = 28
        assertEquals(28.0, outerComposite.apply(2.0), 0.0001);
    }

    @Test
    public void testCompositionOrder() {
        // Важно: порядок применения функций имеет значение!
        // h1(x) = (x + 2) * 3
        // h2(x) = x + (2 * 3) = x + 6

        CompositeFunction h1 = new CompositeFunction(
                x -> x + 2,     // сначала прибавляем 2
                x -> x * 3       // затем умножаем на 3
        );

        CompositeFunction h2 = new CompositeFunction(
                x -> x * 3,     // сначала умножаем на 3
                x -> x + 2       // затем прибавляем 2
        );

        // Для x = 4:
        // h1(4) = (4 + 2) * 3 = 6 * 3 = 18
        // h2(4) = (4 * 3) + 2 = 12 + 2 = 14
        assertEquals(18.0, h1.apply(4.0), 0.0001);
        assertEquals(14.0, h2.apply(4.0), 0.0001);

        // Разные результаты подтверждают важность порядка!
        assertNotEquals(h1.apply(4.0), h2.apply(4.0), 0.0001);
    }

    @Test
    public void testGetFunctions() {
        MathFunction first = new SqrFunction();
        MathFunction second = new ConstantFunction(5.0);

        CompositeFunction composite = new CompositeFunction(first, second);

        // Проверяем, что геттеры возвращают правильные функции
        assertEquals(first, composite.getFirstFunction());
        assertEquals(second, composite.getSecondFunction());
    }

    @Test
    public void testFieldsAreFinal() throws Exception {
        // Проверяем, что поля действительно final
        var firstField = CompositeFunction.class.getDeclaredField("firstFunction");
        var secondField = CompositeFunction.class.getDeclaredField("secondFunction");

        assertTrue(java.lang.reflect.Modifier.isFinal(firstField.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(secondField.getModifiers()));
    }

    @Test
    public void testComplexMathComposition() {
        // h(x) = sin(x²)
        CompositeFunction sinOfSquare = new CompositeFunction(
                new SqrFunction(),      // f(x) = x²
                Math::sin               // g(x) = sin(x)
        );

        assertEquals(0.0, sinOfSquare.apply(0.0), 0.0001);        // sin(0²) = sin(0) = 0
        assertEquals(Math.sin(1.0), sinOfSquare.apply(1.0), 0.0001); // sin(1²) = sin(1)
        assertEquals(Math.sin(4.0), sinOfSquare.apply(2.0), 0.0001); // sin(2²) = sin(4)
    }
}