package bstmap;

import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

    private class Node {
        private K key;
        private V value;
        private Node left;
        private Node right;
        private int size;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            size = 1;
        }
    }

    private Node root;

    public BSTMap() {
        this.clear();
    }

    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean containsKey(K key) {
        return !(getNode(root, key) == null);
    }

    @Override
    public V get(K key) {
        return getHelper(root, key);
    }

    public V getHelper(Node n, K key) {
        if (n == null) {
            return null;
        }

        int compare = key.compareTo(n.key);

        if (compare < 0) {
            return getHelper(n.left, key);
        } else if (compare > 0){
            return getHelper(n.right, key);
        }
        return n.value;
    }

    public Node getNode(Node n, K key) {
        if (n == null) {
            return null;
        }

        int compare = key.compareTo(n.key);

        if (compare < 0) {
            return getNode(n.left, key);
        } else if (compare > 0){
            return getNode(n.right, key);
        }
        return n;
    }

    @Override
    public int size() {
        return size(root);
    }

    private int size(Node node) {
        if (node == null) {
            return 0;
        }
        return node.size + size(node.left) + size(node.right);
    }


    @Override
    public void put(K key, V value) {
        root = putHelper(root, key, value);
    }

    public Node putHelper(Node n, K key, V value) {
        if (n == null) {
            return new Node(key, value);
        }

        int compare = key.compareTo(n.key);

        if (compare < 0) {
            n.left = putHelper(n.left, key, value);
        } else if (compare > 0){
            n.right = putHelper(n.right, key, value);
        } else {
//            n.key = key;
//            n.value = value;
            n = new Node(key, value);
        }
        return n;
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        keyAggregation(root, keys);
        return keys;
    }

    void keyAggregation(Node n, Set<K> set) {
        if (n != null) {
            keyAggregation(n.left, set);
            set.add(n.key);
            keyAggregation(n.right, set);
        }
    }

    @Override
    public V remove(K key) {
        V value = get(key);
        root = removeHelper(root, key);
        return value;
    }

    @Override
    public V remove(K key, V value) {
        if (!containsKey(key)) {
            throw new InputMismatchException();
        } else {
            root = removeHelper(root, key);
            return value;
        }
    }

    public Node removeHelper(Node n, K key) {
        if (n == null) {
            return null;
        }


        int compare = key.compareTo(n.key);
        if (compare < 0) {
            n.left = removeHelper(n.left, key);
            return n;
        } else if (compare > 0) {
            n.right = removeHelper(n.right, key);
            return n;
        } else {
            if (n.left == null && n.right == null) {
                n = null;
                return n;
            } else if (n.left == null) {
                n = n.right;
                return n;
            } else if (n.right == null) {
                n = n.left;
                return n;
            } else {
                Node successor = min(n.right);
                n.key = successor.key;
                n.value = successor.value;
                n.right = removeHelper(n.right, successor.key);
                return root;
            }
        }
    }

    public Node min(Node n) {
        if (n.left == null) {
            return n;
        }
        return min(n.left);
    }

    public Iterator<K> iterator() {
        return null;
    }

    @Override
    public void printInOrder() {
        printInOrder(root);
    }

    public void printInOrder(Node n) {
        if (n == null) {
            return;
        }
        printInOrder(n.left);
        System.out.print("[" + n.key + ", " + n.value + "]" + " ");
        printInOrder(n.right);
    }
}
