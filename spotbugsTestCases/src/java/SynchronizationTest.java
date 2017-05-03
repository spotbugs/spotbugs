// This class is not intended to be thread safe
// Make sure it doesn't generate any warnings
public class SynchronizationTest {

    private int x, y;

    public SynchronizationTest() {
        foo();
        bar();
    }

    private void foo() {
        x = 1;
        y = 2;
    }

    private void bar() {
        x = y + 1;
        y = x + 1;
    }

    public synchronized void baz() {
        x++;
        y++;
    }

    public void incrementX() {
        x++;
    }
}
