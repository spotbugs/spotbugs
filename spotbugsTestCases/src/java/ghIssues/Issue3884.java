package ghIssues;

public class Issue3884 {
    private int count = 0;

    public synchronized void testWithFieldRead() {
        int temp = this.count;
        this.notifyAll();
    }

    public synchronized void testWithPrintln() {
        System.out.println("Log");
        this.notifyAll();
    }

    public synchronized void testWithMutation() {
        this.count++;
        this.notifyAll();
    }

    public synchronized void testNakedNotify() {
        this.notifyAll();
    }
}
