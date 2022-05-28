package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.checkerframework.checker.units.qual.A;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    @Test
    public void testThreeAddThreeRemove() {
    AListNoResizing<Integer> working = new AListNoResizing<Integer>();
    BuggyAList<Integer> buggy = new BuggyAList<Integer>();

    for (int i = 0; i < 3; i++) {
        working.addLast(i);
        buggy.addLast(i);
    }

        assertEquals(working.size(), buggy.size());
        assertEquals(working.removeLast(), buggy.removeLast());
        assertEquals(working.removeLast(), buggy.removeLast());
        assertEquals(working.removeLast(), buggy.removeLast());
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> B = new BuggyAList<Integer>();

        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                B.addLast(randVal);
            } else if (operationNumber == 1) {
                // size
                int l_size = L.size();
                int b_size = B.size();
                assertEquals(L.size(), B.size());
            } else if (operationNumber == 2) {
                if (L.size() > 0) {
                    int l_last = L.getLast();
                    int b_last = B.getLast();
                    assertEquals(l_last, b_last);
                }
            } else if (operationNumber == 3) {
                if (L.size() > 0) {
                    int l_removed = L.removeLast();
                    int b_removed = B.removeLast();
                    assertEquals(l_removed, b_removed);
                }
            }
        }
    }
}
