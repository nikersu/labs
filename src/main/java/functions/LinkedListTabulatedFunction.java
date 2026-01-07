package functions;

import exceptions.InterpolationException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedListTabulatedFunction extends AbstractTabulatedFunction implements Insertable, Removable, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(LinkedListTabulatedFunction.class);
    private static final long serialVersionUID = 7749510516114039501L;
    
    private static class Node implements Serializable { //вспомогательный класс
        private static final long serialVersionUID = -2500538083607940289L;
        public double x;
        public double y;
        public Node next;
        public Node prev;
        public Node(double x, double y) { //конструктор
            this.x = x;
            this.y = y;
        }
    }
    private Node head;
    protected int count;
    private void addNode(double x, double y) { //метод для добавления узла в конец списка
        Node newNode = new Node(x, y);
        if (head == null) { //если список пустой
            head = newNode;
            head.next = head;
            head.prev = head;
        }
        else {
            Node last = head.prev;
            last.next = newNode;
            newNode.prev = last;
            newNode.next = head;
            head.prev = newNode;
        }
        count ++;
    }
    private Node getNode(int index) { //метод для получения узла по индексу
        if (index < 0 || index >= count) {
            logger.error("Node index out of bounds: {} (count={})", index, count);
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + count);
        }
        Node current;
        if (index <= count / 2) {
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            current = head.prev;
            for (int i = count - 1; i > index; i--) {
                current = current.prev;
            }
        }
        return current;
    }
    public LinkedListTabulatedFunction(double [] xValues, double [] yValues) { //конструктор из массивов
        if (xValues.length < 2) {
            logger.error("Insufficient points for linked list: {}", xValues.length);
            throw new IllegalArgumentException("The table should be at least 2 points long");
        }

        checkLengthIsTheSame(xValues, yValues);
        checkSorted(xValues);

        for (int i = 0; i < xValues.length; i++) {
            addNode(xValues[i], yValues[i]);
        }
    }
    public LinkedListTabulatedFunction(MathFunction source, double xFrom, double xTo, int count) { // конструктор: дискретизация
        if (count < 2) {
            logger.error("Insufficient points requested: {}", count);
            throw new IllegalArgumentException("The number of points must be at least 2");
        }
        if (xFrom > xTo) {
            double temp = xFrom;
            xFrom = xTo;
            xTo = temp;
        }

        if (xFrom == xTo) {
            double y = source.apply(xFrom);
            for (int i = 0; i < count; i++) {
                addNode(xFrom, y);
            }
        }
        else {
            double step = (xTo - xFrom) / (count - 1);
            for (int i = 0; i < count; i++) {
                double x = xFrom + i * step;
                double y = source.apply(x);
                addNode(x, y);
            }
        }
    }
    @Override
    public int getCount() {
        return count;
    }
    @Override
    public double leftBound() {
        if (head == null) {
            logger.error("Attempt to get left bound from empty function");
            throw new IllegalStateException("Function is empty");
        }
        return head.x;
    }

    @Override
    public double rightBound() {
        if (head == null) {
            logger.error("Attempt to get right bound from empty function");
            throw new IllegalStateException("Function is empty");
        }
        return head.prev.x;
    }
    @Override
    public double getX(int index) {
        if (index < 0 || index >= count) {
            logger.error("GetX index out of bounds: {} (count={})", index, count);
            throw new IllegalArgumentException("The index is out of range");
        }
        return getNode(index).x;
    }

    @Override
    public double getY(int index) {
        if (index < 0 || index >= count) {
            logger.error("GetY index out of bounds: {} (count={})", index, count);
            throw new IllegalArgumentException("The index is out of range");
        }
        return getNode(index).y;
    }

    @Override
    public void setY(int index, double value) {
        if (index < 0 || index >= count) {
            logger.error("SetY index out of bounds: {} (count={})", index, count);
            throw new IllegalArgumentException("The index is out of range");
        }
        getNode(index).y = value;
    }

    @Override
    public int indexOfX(double x) {
        Node current = head;
        for (int i = 0; i < count; i++) {
            if (Math.abs(current.x - x) < 1e-12) {
                return i;
            }
            current = current.next;
        }
        return -1;
    }

    @Override
    public int indexOfY(double y) {
        Node current = head;
        for (int i = 0; i < count; i++) {
            if (Math.abs(current.y - y) < 1e-12) {
                return i;
            }
            current = current.next;
        }
        return -1;
    }

    @Override
    protected double extrapolateLeft(double x) { //экстраполяция слева (x<leftBound)
        return interpolate(x, getX(0), getX(1), getY(0), getY(1));
    }

    @Override
    protected double extrapolateRight(double x) { //экстраполяция справа (x>rightBound)
        return interpolate(x, getX(count - 2), getX(count - 1), getY(count - 2), getY(count - 1));
    }

    @Override
    protected double interpolate(double x, int floorIndex) {
        if (floorIndex < 0 || floorIndex >= count - 1) {
            logger.error("Invalid floor index for interpolation: {} (count={})", floorIndex, count);
            throw new InterpolationException("Incorrect index for interpolation");
        }

        double leftX = getX(floorIndex);
        double rightX = getX(floorIndex + 1);

        if (x < leftX || x > rightX) {
            logger.error("X={} outside interpolation interval [{}, {}]", x, leftX, rightX);
            throw new InterpolationException("Point x is outside the interpolation interval");
        }
        // вызов метода интерполяции с четырьмя параметрами
        return interpolate(x, leftX, rightX, getY(floorIndex), getY(floorIndex + 1));
    }

    @Override
    protected int floorIndexOfX(double x) {
        if (x < leftBound()) {
            logger.error("X={} is less than left bound {}", x, leftBound());
            throw new IllegalArgumentException("x is less than the left border");
        }
        if (x < getX(0)) return 0;
        if (x > getX(count - 1))return count;
        int left = 0;
        int right = count - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (getX(mid) == x) {
                return mid;
            } else if (getX(mid) < x){
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return right;
    }

    @Override
    public void insert(double x, double y) {
        // Проверяем, существует ли уже узел с таким x
        int existingIndex = indexOfX(x);
        if (existingIndex != -1) {
            setY(existingIndex, y);
            return;
        }
        // если список пустой, просто добавляем узел
        if (head == null) {
            addNode(x, y);
            return;
        }
        // создаем новый узел
        Node newNode = new Node(x, y);
        // если новый узел должен быть в начале списка
        if (x < head.x) {
            Node last = head.prev;
            // устанавливаем связи для нового узла
            newNode.next = head;
            newNode.prev = last;
            // обновляем связи соседних узлов
            head.prev = newNode;
            last.next = newNode;
            // обновляем голову списка
            head = newNode;
            count++;
            return;
        }
        // если новый узел должен быть в конце списка
        if (x > head.prev.x) {
            Node last = head.prev;
            // устанавливаем связи для нового узла
            newNode.next = head;
            newNode.prev = last;
            // обновляем связи соседних узлов
            last.next = newNode;
            head.prev = newNode;
            count++;
            return;
        }
        // поиск места для вставки в середину списка
        Node current = head;
        for (int i = 0; i < count; i++) {
            if (current.x < x && x < current.next.x) {
                // нашли место для вставки между current и current.next
                newNode.next = current.next;
                newNode.prev = current;
                // обновляем связи соседних узлов
                current.next.prev = newNode;
                current.next = newNode;
                count++;
                return;
            }
            current = current.next;
        }
    }

    @Override
    public void remove(int index) { //метод для удаления узлов
        if (index < 0 || index >= count) {
            logger.error("Remove index out of bounds: {} (count={})", index, count);
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        if (count == 2) {
            logger.error("Cannot remove point - minimum 2 points required");
            throw new IllegalStateException("Cannot remove element - minimum 2 points required");
        }
        if (count == 1) {           //если в списке один узел
            head = null;
            count = 0;
            return;
        }
        Node nodeToRemove = getNode(index);
        if (nodeToRemove == head) { //если удаляем голову
            head = head.next;
        }
        nodeToRemove.prev.next = nodeToRemove.next; //переписываем узлы
        nodeToRemove.next.prev = nodeToRemove.prev;
        count--;
        if (count == 0) { //если список стал пустым
            head = null;
        }
    }

    @Override
    public Iterator<Point> iterator() {
        return new Iterator<Point>() {
            private Node currentNode = head;  // каждый итератор получает свою копию ссылки
            private int elementsReturned = 0; // счетчик для этого конкретного итератора

            @Override
            public boolean hasNext() {
                return elementsReturned < count;
            }

            @Override
            public Point next() {
                if (!hasNext()) {
                    logger.warn("Linked list iterator next() called with no more elements");
                    throw new NoSuchElementException("No more points in tabulated function");
                }

                Point point = new Point(currentNode.x, currentNode.y);
                currentNode = currentNode.next;  // переходим к следующему узлу
                elementsReturned++;              // увеличиваем счетчик этого итератора

                return point;
            }
        };
    }


}
