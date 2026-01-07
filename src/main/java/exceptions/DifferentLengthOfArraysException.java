package exceptions;

public class DifferentLengthOfArraysException extends RuntimeException {
    public DifferentLengthOfArraysException() { // конструктор без параметров
        super();
    }
    public DifferentLengthOfArraysException(String message) { // конструктор с параметром-сообщением
        super(message);
    }
}