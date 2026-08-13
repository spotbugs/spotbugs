package ghIssues;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Reproducer for issue #4007: {@code MS_EXPOSE_BUF}/{@code EI_EXPOSE_BUF} was not reported when a
 * getter returns the backing array of an internal {@link java.nio.Buffer} via {@code array()}.
 *
 * <p>Calling {@code array()} on a buffer exposes its mutable backing array, so the same
 * encapsulation concern as {@code duplicate()} / {@code wrap()} applies.
 */
public class Issue4007 {
    private final ByteBuffer buf = ByteBuffer.allocate(16);

    private static final ByteBuffer S_BUF = ByteBuffer.allocate(16);

    private char[] backing;

    // Instance backing array exposure: must be flagged as EI_EXPOSE_BUF.
    public byte[] getArray() {
        return buf.array();
    }

    // Static backing array exposure: must be flagged as MS_EXPOSE_BUF.
    public static byte[] getStaticArray() {
        return S_BUF.array();
    }

    // Cross-type setter: a buffer parameter's backing array is stored into an array field.
    // Exercises the array() setter-capture path (bufferParamArrays / getPotentialCapture).
    public void setBacking(CharBuffer cb) {
        backing = cb.array();
    }
}
