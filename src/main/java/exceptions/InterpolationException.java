package exceptions;

public class InterpolationException extends RuntimeException {
    public InterpolationException() { // конструктор без параметров
        super();
    }
    public InterpolationException(String message) { // конструктор с параметром-сообщением
        super(message);
    }
}
