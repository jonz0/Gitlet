package deque;

public class LinkedListDeque<T> {

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

    public boolean isEmpty() {
        if (first == null) {
            return true;
        }
        return false;
    }
}
