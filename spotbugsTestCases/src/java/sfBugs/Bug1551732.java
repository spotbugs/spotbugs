package sfBugs;

/**
 * Shows false dead lock store warning.
 * 
 * @author <a href="mailto:rlenard@avaya.com">rlenard</a>
 */
public class Bug1551732 extends Thread {
    /**
     * 
     * {@inheritDoc}.
     * 
     * @see java.lang.Thread#run()
     * 
     */
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        // We add 20 milliseconds to take into account possible inaccuracy of
        // sleep calls.
        long target = start + 20;
        while (true) {
            try {
                target += 10;
                final long toSleep = target - System.currentTimeMillis();
                sleep(Math.max(0, toSleep));
            } catch (final InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (target > 100000) {
                    return;
                }
                final long elapsed = System.currentTimeMillis() - start;
                if (elapsed >= 50) {
                    start += 50; // dead store warning here
                }
            }
        }
    }
}
