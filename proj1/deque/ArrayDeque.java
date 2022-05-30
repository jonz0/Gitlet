package deque;

public class ArrayDeque<T> implements Deque<T>{
    private T[] items;
    private int size;
    private T front = null;
    private T back = null;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
    }

    public void resize(int capacity) {
        T[] temp = (T[]) new Object[capacity];
        System.arraycopy(items, 0, temp, 0, size);
        items = temp;
    }

    public void utilizationRule() {
        if (size == items.length) {
            resize(items.length * 2);
        }
    }

    public void addLast(T n) {
        utilizationRule();

        back = n;
        items[size] = n;
        size += 1;

        if(size == 1) {
            front = n;
        }
    }

    public void addFirst(T n) {
        utilizationRule();

        size += 1;
        T[] temp = (T[]) new Object[size];
        System.arraycopy(items, 0, temp, 1, size);
        temp[0] = n;
        items = temp;
        front = n;

        if (size == 1) {
            back = n;
        }

    }

    @Override
    public T removeLast() {

        if (size == 0) {
            return null;
        }

        utilizationRule();

        T temp = items[size - 1];
        items[size - 1] = null;
        size -= 1;

        if (size == 0) {
            front = null;
            back = null;
        } else if (size == 1) {
            front = items[0];
            back = items[0];
        } else {
            back = items[size - 1];
        }

        return temp;
    }

    public T removeFirst() {
        if (size == 0) {
            return null;
        }

        utilizationRule();

        T[] temp_array = (T[]) new Object[size];
        T temp_obj = items[0];
        System.arraycopy(items, 1, temp_array, 0, size - 1);
        items = temp_array;
        size -= 1;

        if (size == 0) {
            front = null;
            back = null;
        } else if (size == 1) {
            front = items[0];
            back = items[0];
        } else {
            front = items[0];
        }

        return temp_obj;
    }

    public int length() {
        return items.length;
    }

    public T get(int index) { return items[index]; }

    public int size() { return size; }

    public void printDeque() {
        for (int i = 0; i < items.length; i++) {
            System.out.print(items[i] + " ");
        }
        System.out.println();
    }

    public T front() { return front; }

    public T back() { return back; }
}