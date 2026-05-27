package ghIssues;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4007">#4007</a>.
 */
public class Issue4007 {
    private final ByteBuffer buf = ByteBuffer.allocate(16);
    private static final ByteBuffer S_BUF = ByteBuffer.allocate(16);
    private Date[] dates = new Date[] {new Date()};

    public byte[] getArray() {
        return buf.array();
    }

    public static byte[] getStaticArray() {
        return S_BUF.array();
    }

    public ByteBuffer getDuplicate() {
        return buf.duplicate();
    }

    public void setDates(Date[] input) {
        dates = Arrays.copyOf(input, input.length);
    }

    public Date[] getDatesClone() {
        return dates.clone();
    }
}
