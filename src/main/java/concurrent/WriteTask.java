package concurrent;

import functions.TabulatedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteTask implements Runnable {
    private final TabulatedFunction function;
    private final double value;
    private static final Logger logger = LoggerFactory.getLogger(WriteTask.class);

    public WriteTask(TabulatedFunction function, double value) {
        this.function = function;
        this.value = value;
        logger.info("WriteTask created with value {}", value);
    }

    @Override
    public void run() {
        logger.info("Thread {} started writing", function.getCount());
        for (int i = 0; i < function.getCount(); i++) {
            synchronized (function) {
                function.setY(i, value);
                System.out.printf("Writing for index %d complete%n", i);
            }
        }
        logger.info("Thread {} finished writing", Thread.currentThread().getName());
    }
}

