package concurrent;
import functions.LinkedListTabulatedFunction;
import functions.UnitFunction;
import functions.TabulatedFunction;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplyingTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(MultiplyingTaskExecutor.class);
    public static void main(String[] args) {
        logger.info("Starting MultiplyingTaskExecutor");
        TabulatedFunction function = new LinkedListTabulatedFunction(new UnitFunction(), 1, 1000, 1000);
        // список потоков
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            MultiplyingTask task = new MultiplyingTask(function);
            Thread thread = new Thread(task);
            threads.add(thread);
        }
        // запуск
        for (Thread thread : threads) {
            thread.start();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.error("Main thread interrupted", e);
            Thread.currentThread().interrupt();
            return;
        }

        // вывод
        System.out.println(function);
        logger.info("Thread {} was started", Thread.currentThread().getName());
    }
}