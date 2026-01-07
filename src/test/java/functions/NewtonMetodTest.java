package functions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NewtonMetodTest {

    @Test
    void test1() {
        NewtonMetod num = new NewtonMetod(
                x -> x * x - 4,
                x -> 2 * x);

        assertEquals(2.0, num.apply(3), 0.0001);
    }

    @Test
    void test2() {
        NewtonMetod num = new NewtonMetod(
                x -> x * x * x - 8,
                x -> 3 * x * x);

        assertEquals(2.0, num.apply(3), 0.0001);
    }

    @Test
    void test3() {
        NewtonMetod num = new NewtonMetod(
                x -> x * x - 4,
                x -> 2 * x);

        assertEquals(2.0, num.apply(10), 0.0001);
    }

}