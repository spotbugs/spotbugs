package ghIssues;

public class Issue4419 {
    private volatile Thread waiter;

    public int fill(int limit) {
        Thread blocked = null;
        int filled = 0;
        while (true) {
            if (filled >= limit) {
                break;
            }
            filled++;
            blocked = waiter;
            if (blocked == null) {
                continue;
            }
            waiter = null;
            blocked = null;
        }
        if (blocked != null) {
            blocked.interrupt();
        }
        return filled;
    }
}
