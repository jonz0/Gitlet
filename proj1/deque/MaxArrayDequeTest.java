package deque;

import org.junit.Test;
import static org.junit.Assert.*;


/** Performs some basic linked list tests. */
public class MaxArrayDequeTest {

    @Test
    public void compareTest() {
        MaxArrayDeque<Integer> a1 = new MaxArrayDeque<Integer>();

        a1.addLast(1);
        a1.addLast(2);
        a1.addLast(3);
        a1.addLast(300);
        a1.removeLast();

        assertEquals(3, (int) a1.max());
    }
}