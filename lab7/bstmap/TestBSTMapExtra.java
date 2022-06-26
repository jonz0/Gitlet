package bstmap;

import java.util.Set;
import java.util.HashSet;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Tests of optional parts of lab 7. */
public class TestBSTMapExtra {

    /*
    * Sanity test for keySet, only here because it's optional
    */
    @Test
    public void sanityKeySetTest() {
    	BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        HashSet<String> values = new HashSet<String>();
        for (int i = 0; i < 455; i++) {
            b.put("hi" + i, 1);
            values.add("hi" + i);
        }
        assertEquals(455, b.size()); //keys are there
        Set<String> keySet = b.keySet();
        assertTrue(values.containsAll(keySet));
        assertTrue(keySet.containsAll(values));
    }

    /* Remove Test
     *
     * Note for testRemoveRoot:
     *
     * Just checking that c is gone (perhaps incorrectly)
     * assumes that remove is BST-structure preserving.
     *
     * More exhaustive tests could be done to verify
     * implementation of remove, but that would require doing
     * things like checking for inorder vs. preorder swaps,
     * and is unnecessary in this simple BST implementation.
     */
    @Test
    public void testRemoveRoot() {
        BSTMap<String,String> q = new BSTMap<String,String>();
        q.put("c","a");
        q.put("b","a");
        q.put("a","a");
        q.put("d","a");
        q.put("e","a"); // a b c d e
        assertTrue(null != q.remove("c"));
        assertFalse(q.containsKey("c"));
        assertTrue(q.containsKey("a"));
        assertTrue(q.containsKey("b"));
        assertTrue(q.containsKey("d"));
        assertTrue(q.containsKey("e"));
    }

    /* Remove Test 2
     * test the 3 different cases of remove
     */
    @Test
    public void testRemoveThreeCases() {
        BSTMap<String,String> q = new BSTMap<String,String>();
        q.put("c","a");
        q.put("b","a");
        q.put("a","a");
        q.put("d","a");
        q.put("e","a");                         // a b c d e
        assertTrue(null != q.remove("e"));      // a b c d

        assertTrue(q.containsKey("a"));
        assertTrue(q.containsKey("b"));
        assertTrue(q.containsKey("c"));
        assertTrue(q.containsKey("d"));
        assertTrue(null != q.remove("c"));      // a b d
        assertTrue(q.containsKey("a"));
        assertTrue(q.containsKey("b"));
        assertTrue(q.containsKey("d"));
        q.put("f","a");                         // a b d f
        assertTrue(null != q.remove("d"));      // a b f
        assertTrue(q.containsKey("a"));
        assertTrue(q.containsKey("b"));
        assertTrue(q.containsKey("f"));
    }

    /* Remove Test 3
    *  Checks that remove works correctly on root nodes
    *  when the node has only 1 or 0 children on either side. */
    @Test
    public void testRemoveRootEdge() {
        BSTMap rightChild = new BSTMap();
        rightChild.put('A', 1);
        rightChild.put('B', 2);
        Integer result = (Integer) rightChild.remove('A');
        assertTrue(result.equals(new Integer(1)));
        for (int i = 0; i < 10; i++) {
            rightChild.put((char) ('C'+i), 3+i);
        }
        rightChild.put('A', 100);
        assertTrue(((Integer) rightChild.remove('D')).equals(new Integer(4)));
        assertTrue(((Integer) rightChild.remove('G')).equals(new Integer(7)));
        assertTrue(((Integer) rightChild.remove('A')).equals(new Integer(100)));
        assertTrue(rightChild.size()==9);

        BSTMap leftChild = new BSTMap();
        leftChild.put('B', 1);
        leftChild.put('A', 2);
        assertTrue(((Integer) leftChild.remove('B')).equals(1));
        assertEquals(1, leftChild.size());
        assertEquals(null, leftChild.get('B'));

        BSTMap noChild = new BSTMap();
        noChild.put('Z', 15);
        assertTrue(((Integer) noChild.remove('Z')).equals(15));
        assertEquals(0, noChild.size());
        assertEquals(null, noChild.get('Z'));
    }

    @Test
    public void removeSizeTest() {
        BSTMap<String,String> q = new BSTMap<String,String>();
        q.put("c","a");
        q.put("b","a");
        q.put("a","a");
        q.put("d","a");
        q.put("f","a");
        q.put("i","a");
        q.put("h","a");
        q.put("g","a");
        q.put("j","a");
        q.put("k","a");
        assertNotNull(q.remove("c"));
        assertEquals(9, q.size());
        assertNotNull(q.remove("d"));
        assertEquals(8, q.size());
        assertNotNull(q.remove("a"));
        assertEquals(7, q.size());
        assertNotNull(q.remove("j"));
        assertEquals(6, q.size());
    }

    @Test
    public void printTest() {
        BSTMap<Integer,String> q = new BSTMap<Integer,String>();
        q.put(1 ,"a");
        q.put(2 ,"a");
        q.put(3 ,"a");
        q.put(7 ,"a");
        q.put(8 ,"a");
        q.put(9 ,"a");
        q.put(4 ,"a");
        q.put(5 ,"a");
        q.put(6 ,"a");

        q.printInOrder();
    }

    @Test
    public void heightTest() {
        BSTMap<Integer,String> q = new BSTMap<Integer,String>();
        q.put(5 ,"a");
        assertEquals(0, q.height(q.getNode(q.root, 5)));
        q.put(3 ,"a");
        assertEquals(1, q.height(q.getNode(q.root, 5)));
        q.put(7 ,"a");
        assertEquals(1, q.height(q.getNode(q.root, 5)));
        q.put(2 ,"a");
        assertEquals(2, q.height(q.getNode(q.root, 5)));
        q.put(8 ,"a");
        assertEquals(2, q.height(q.getNode(q.root, 5)));

        q.put(4 ,"a");
        assertEquals(2, q.height(q.getNode(q.root, 5)));
        q.put(6 ,"a");
        assertEquals(2, q.height(q.getNode(q.root, 5)));
        q.put(1 ,"a");
        assertEquals(3, q.height(q.getNode(q.root, 5)));
        q.put(100 ,"a");
        q.put(101 ,"a");
        q.put(102 ,"a");
        assertEquals(5, q.height(q.getNode(q.root, 5)));

        q.remove(100 ,"a");
        q.remove(6 ,"a");
        q.remove(102 ,"a");
        q.remove(1 ,"a");
        q.remove(4 ,"a");
        q.remove(101 ,"a");
        assertEquals(2, q.height(q.getNode(q.root, 5)));
    }

    @Test
    public void rebalanceChecker() {
        BSTMap<Integer,String> q = new BSTMap<Integer,String>();
        q.put(5 ,"a");
        q.put(7 ,"a");
        q.put(9 ,"a");
        q.put(11 ,"a");
        q.put(13 ,"a");

        System.out.println(q.getNode(5).right.key);
        System.out.println(q.getNode(7).right.key);
        System.out.println(q.getNode(9).right.key);
    }

}
