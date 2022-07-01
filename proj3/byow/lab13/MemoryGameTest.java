package byow.lab13;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryGameTest {

    @Test
    public void testRandomString() {
        MemoryGame m = new MemoryGame(16, 16, 234234);
        String s = m.generateRandomString(5);
        System.out.println(s);
    }

    @Test
    public void testStringDisplay() {
        MemoryGame m = new MemoryGame(32, 32, 234234);
        String s = m.generateRandomString(5);
        m.drawFrame(s);
    }
}
