package exceptions;

public class ArrayIsNotSortedException extends RuntimeException {
    public ArrayIsNotSortedException() { // конструктор без параметров
        super();
    }
    public ArrayIsNotSortedException(String message) { // конструктор с параметром-сообщением
        super(message);
    }
}
