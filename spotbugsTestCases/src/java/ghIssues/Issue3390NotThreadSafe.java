package ghIssues;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class Issue3390NotThreadSafe implements Runnable {
    private int counter;

    public int getCount() {
        return counter;
    }

    @Override
    public void run() {
        counter++;
    }
}
