package concurrent;

import functions.ConstantFunction;
import functions.LinkedListTabulatedFunction;
import functions.TabulatedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadWriteTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteTaskExecutor.class);
    public static void main(String[] args) {
        logger.info("Starting ReadWriteTaskExecutor");
        TabulatedFunction function = new LinkedListTabulatedFunction(
                new ConstantFunction(-1),
                1,
                1000,
                1000
        );

        Thread reader = new Thread(new ReadTask(function), "Reader");
        Thread writer = new Thread(new WriteTask(function, 0.5), "Writer");

        reader.start();
        writer.start();
        logger.info("Started Reader and Writer threads");
    }
}


