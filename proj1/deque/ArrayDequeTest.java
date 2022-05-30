package deque;

import org.junit.Test;
import static org.junit.Assert.*;
import edu.princeton.cs.algs4.Stopwatch;


public class ArrayDequeTest {

    @Test
    public void simpleTest() {
        ArrayDeque<Integer> a1 = new ArrayDeque<Integer>();
        ArrayDeque<Integer> a2 = new ArrayDeque<Integer>();
        a1.addLast(3);
        a1.addLast(2);
        a1.addLast(1);
        a2.addLast(3);
        a2.addLast(2);
        a2.addLast(1);

        assertEquals(a1.size(), a2.size());
        assertEquals(a1.removeLast(), a2.removeLast());
    }

    @Test
    public void manyObjectsTest() {
        ArrayDeque<Integer> a1 = new ArrayDeque<Integer>();
        ArrayDeque<Integer> a2 = new ArrayDeque<Integer>();

        for (int i = 0; i < 1025; i++) {
            a1.addLast(i);
            a2.addLast(i);
        }

        System.out.println("a1 size: " + a1.size() + "\na2 size: " + a2.size());
        System.out.println("a1 length: " + a1.length() + "\na2 length: " + a2.length());

        assertEquals(a1.size(), a2.size());
        assertEquals(a1.removeLast(), a2.removeLast());
    }

    @Test
    public void timingTest() {
        ArrayDeque<Integer> a1 = new ArrayDeque<Integer>();
        ArrayDeque<Integer> a2 = new ArrayDeque<Integer>();
        double[] times = new double[2];

        Stopwatch s1 = new Stopwatch();
        for (int i = 0; i < 1000; i++) {
            a1.addLast(i);
        }
        times[0] = s1.elapsedTime();

        Stopwatch s2 = new Stopwatch();
        for (int i = 0; i < 10000000; i++) {
            a2.addLast(i);
        }
        times[1] = s2.elapsedTime();


        System.out.println("s1 time: " + times[0] + " ... s2 time: " + times[1]);
    }

    @Test
    public void circularTest() {
        ArrayDeque<Integer> a1 = new ArrayDeque<Integer>();

        for (int i = 0; i < 125; i++) {
            a1.addLast(i);
        }

        a1.removeLast();
        a1.removeLast();
        a1.removeFirst();
        a1.removeFirst();
        a1.removeFirst();

        assertEquals(3, (int) a1.get(0));
        assertEquals(122, (int) a1.back());
    }

    @Test
    public void circularTestNull() {
        ArrayDeque<Integer> a1 = new ArrayDeque<Integer>();

        for (int i = 0; i < 1; i++) {
            a1.addLast(i);
        }

        a1.removeLast();

        assertNull(a1.front());
        assertNull(a1.back());
    }

    @Test
    public void circularTestFirst() {
        ArrayDeque<Integer> a1 = new ArrayDeque<Integer>();

        for (int i = 0; i < 2; i++) {
            a1.addLast(i);
        }

        a1.removeLast();

        assertEquals(0, (int) a1.front());
        assertEquals(0, (int) a1.back());
    }
}
