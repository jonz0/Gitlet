package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T> {

    /** Constructs a node of type T with first item first and next item next. */
    private class Node {
        T item;
        Node next;
        Node prev;

        public Node(T item) {
            this.item = item;
            this.next = null;
            this.prev = null;
        }
    }

    Node sentinel;
    Node first, last;
    public int size;

    public LinkedListDeque() {
        sentinel = new Node(null);
        first = null;
        last = null;
        size = 0;
    }

    public void addLast(T item) {
        Node newNode = new Node(item);
        newNode.next = sentinel;

        if (sentinel.next == null){
            // if the linked list is empty
            sentinel.next = sentinel.prev = newNode;
            first = last = newNode;
            size += 1;
            newNode.prev = sentinel;
        } else {
            // if the linked list contains items
            newNode.prev = last;
            last.next = newNode;
            last = newNode;
            size += 1;
        }
    }

    public void addFirst(T item) {
        Node newNode = new Node(item);
        newNode.prev = sentinel;

        if (sentinel.next == null){
            // if the linked list is empty
            sentinel.next = sentinel.prev = newNode;
            first = last = newNode;
            size += 1;
            newNode.next = sentinel;
        } else {
            // if the linked list contains items
            newNode.next = sentinel.next;
            sentinel.next = newNode;
            first = newNode;
            size += 1;
        }
    }

    public T removeFirst() {
        if (first == null) {
            return null;
        }

        T temp = first.item;
        size -= 1;

        if (first.next == sentinel) {
            sentinel.next = sentinel.prev = null;
            first = last = null;
        } else {
            first = sentinel.next.next;
            first.prev = sentinel;
            sentinel.next = first;
        }
        return temp;
    }

    public T removeLast() {
        if (first == null) {
            return null;
        }

        T temp = last.item;
        size -= 1;

        if (first.next == sentinel) {
            sentinel.next = sentinel.prev = null;
            first = last = null;
        } else {
            last = last.prev;
            last.next = sentinel;
            sentinel.prev = last;
        }

        return temp;
    }

    public T get(int index) {
        int temp = 0;
        Node tempNode = sentinel.next;

        if (size == 0) {
            return null;
        }

        while (temp != index) {
            temp += 1;
            tempNode = tempNode.next;
        }

        return tempNode.item;
    }

    public T getRecursive(int index) {
        if (index > size) {
            return null;
        }

        Node i = sentinel;

        if (index == 0) {
            return i.next.item;
        } else {
            LinkedListDeque<T> copy = new LinkedListDeque<T>();
            copy.sentinel.next = this.sentinel.next;
            copy.size = this.size;
            copy.first = this.first;
            copy.last = this.last;
            copy.removeFirst();
            return copy.getRecursive(index - 1);
        }
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        Node printed = sentinel.next;

        while (printed != sentinel) {
            System.out.print(printed.item + " ");
            printed = printed.next;
        }

        System.out.println();
    }

    public Iterator<T> iterator() {
        return new LinkedListIterator();
    }

    private class LinkedListIterator implements Iterator<T> {
        private int index;
        public LinkedListIterator() {
            index = 0;
        }

        public boolean hasNext() {
            return index < size();
        }

        public T next() {
            T returned = get(index);
            index += 1;
            return returned;
        }
    }

    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || o.getClass() == this.getClass()) {
            return false;
        }

        LinkedListDeque<T> other = (LinkedListDeque<T>) o;
        if (other.size() != this.size()) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            if (other.get(i) != this.get(i)) {
                return false;
            }
        }

        return true;
    }
}
