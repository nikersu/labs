package concurrent;
import functions.TabulatedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplyingTask implements Runnable{
    private final TabulatedFunction function;
    private static final Logger logger = LoggerFactory.getLogger(MultiplyingTask.class);

    public MultiplyingTask(TabulatedFunction function) {
        this.function = function;
    }
    @Override
    public void run() {
        logger.info("Thread {} was started", Thread.currentThread().getName());
        for (int i = 0; i < function.getCount(); i++) {
            synchronized (function) { //синхронизация доступа
                double currentY = function.getY(i);
                function.setY(i, currentY * 2);
            }
        }
        // вывод
        logger.info("Thread {} was finished", Thread.currentThread().getName());
    }
}
