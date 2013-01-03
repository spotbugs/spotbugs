import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Getter {
    @ExpectWarning(value="IS2_INCONSISTENT_SYNC")
    int x;

    @ExpectWarning(value="UG_SYNC_SET_UNSYNC_GET")
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
