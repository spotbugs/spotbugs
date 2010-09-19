import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class SynchronizationTesting {

    /**
     * @param args
     */
    @ExpectWarning("ESync")
    public static void main(String[] args) {
        Object o = new Object();
        synchronized (o) {

        }
    }

}
