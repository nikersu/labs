package concurrent;

import functions.TabulatedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadTask implements Runnable {
    private final TabulatedFunction function;
    private static final Logger logger = LoggerFactory.getLogger(ReadTask.class);
    public ReadTask(TabulatedFunction function) {
        this.function = function;
        logger.info("ReadTask created for function with {} points", function.getCount());
    }

    @Override
    public void run() {
        logger.info("Thread {} was started", Thread.currentThread().getName());
        for (int i = 0; i < function.getCount(); i++) {
            synchronized (function) {
                double x = function.getX(i);
                double y = function.getY(i);
                System.out.printf("After read: i = %d, x = %f, y = %f%n", i, x, y);
            }
        }
        logger.info("Thread {} was finished", Thread.currentThread().getName());
    }
}

