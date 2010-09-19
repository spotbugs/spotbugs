import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Getter {
    int x;

    @ExpectWarning("UG")
    public int getX() {
        return x;
    }

    public synchronized void setX(int x) {
        this.x = x;
    }

    public synchronized int calculate() {
        int t = 0;
        t += x;
        t += x;
        return t;
    }
}
