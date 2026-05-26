package ghIssues;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4007">#4007</a>.
 */
public class Issue4007 {

    private final ByteBuffer buf = ByteBuffer.allocate(16);
    private static final ByteBuffer S_BUF = ByteBuffer.allocate(16);
    private Date[] dates = new Date[] { new Date() };

    @ExpectWarning("EI_EXPOSE_BUF")
    public byte[] getArray() {
        return buf.array();
    }

    @ExpectWarning("MS_EXPOSE_BUF")
    public static byte[] getStaticArray() {
        return S_BUF.array();
    }

    @ExpectWarning("EI_EXPOSE_BUF")
    public ByteBuffer getDuplicate() {
        return buf.duplicate();
    }

    @ExpectWarning("EI_EXPOSE_REP2")
    public void setDates(Date[] input) {
        dates = Arrays.copyOf(input, input.length);
    }

    @ExpectWarning("EI_EXPOSE_REP")
    public Date[] getDatesClone() {
        return dates.clone();
    }

    @NoWarning("EI_EXPOSE_BUF,MS_EXPOSE_BUF,EI_EXPOSE_REP,MS_EXPOSE_REP,EI_EXPOSE_REP2")
    public byte[] getArrayCopy() {
        return Arrays.copyOf(buf.array(), buf.array().length);
    }
}
