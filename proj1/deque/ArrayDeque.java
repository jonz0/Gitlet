package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T> {
    private T[] items;
    private int size;
    private T front;
    private T back;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        front = null;
        back = null;
    }

    private void resize(int capacity) {
        T[] temp = (T[]) new Object[capacity];
        System.arraycopy(items, 0, temp, 0, size);
        items = temp;
    }

    private void utilizationRule() {
        if (size + 1 == items.length) {
            resize(items.length * 2);
        }
    }

    private void removeUtilization() {
        if (size - 1 < items.length * 0.25 && size - 1 > 100) {
            resize(items.length / 3);
        }
    }

    public void addLast(T n) {
        utilizationRule();

        back = n;
        items[size] = n;
        size += 1;

        if (size == 1) {
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

        removeUtilization();

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

        removeUtilization();

        T[] tempArray = (T[]) new Object[size];
        T tempObj = items[0];
        System.arraycopy(items, 1, tempArray, 0, size - 1);
        items = tempArray;
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

        return tempObj;
    }

    private int length() {
        return items.length;
    }

    public T get(int index) {
        return items[index];
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        for (int i = 0; i < items.length; i++) {
            System.out.print(items[i] + " ");
        }
        System.out.println();
    }

    private Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int index;
        public ArrayDequeIterator() {
            index = 0;
        }

        public boolean hasNext() {
            return index < size();
        }

        public T next() {
            T returned = items[index];
            index += 1;
            return returned;
        }
    }

    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || o.getClass() == this.getClass()) {
            return false;
        }

        ArrayDeque<T> other = (ArrayDeque<T>) o;
        if (other.size() != this.size()) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            if (other.items[i] != this.items[i]) {
                return false;
            }
        }

        return true;
    }
}
