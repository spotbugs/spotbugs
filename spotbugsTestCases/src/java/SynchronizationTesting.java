import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class SynchronizationTesting {

    /**
     * @param args
     */
    @ExpectWarning(value = "ESync_EMPTY_SYNC", confidence=Confidence.MEDIUM)
    public static void main(String[] args) {
        Object o = new Object();
        synchronized (o) {

        }
    }

}
