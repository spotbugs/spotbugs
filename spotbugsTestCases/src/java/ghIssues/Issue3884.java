package ghIssues;

public class Issue3884 {

    private int count = 0;

    /** Reporter's case 1: a field read stored into a local does not change the state. */
    public synchronized void notifyAfterFieldRead() {
        int temp = this.count;
        this.notifyAll();
    }

    /** Same, expressed with an explicit synchronized block. */
    public void blockNotifyAfterFieldRead() {
        synchronized (this) {
            int temp = this.count;
            this.notifyAll();
        }
    }

    /** Control: already detected today, must stay detected. */
    public synchronized void nakedNotify() {
        this.notifyAll();
    }

    /** Control: a real mutation, must stay unreported. */
    public synchronized void notifyAfterMutation() {
        this.count++;
        this.notifyAll();
    }

    /** Control: a mutation expressed as a field assignment, must stay unreported. */
    public synchronized void notifyAfterFieldAssignment() {
        this.count = 1;
        this.notifyAll();
    }
}
