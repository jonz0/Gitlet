package deque;

public class LinkedListDeque<T> implements Deque<T> {

    /** Constructs a node of type T with first item first and next item next. */
    private class Node {
        public T item;
        public Node next;
        public Node prev;

        public Node(T item) {
            this.item = item;
            this.next = null;
            this.prev = null;
        }
    }

    Node sentinel = new Node(null);
    Node first, last = null;
    int size = 0;

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

        if(first.next == sentinel) {
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
        Node temp_node = sentinel.next;

        if(size == 0) {
            return null;
        }

        while (temp != index) {
            temp += 1;
            temp_node = temp_node.next;
        }

        return temp_node.item;
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
}
