package ghIssues;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/603">GitHub issue</a>
 */
public class Issue603 {
    @SuppressWarnings("unused")
    private byte[] bytes;

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setBytesAfterCheck(byte[] bytes) {
        this.bytes = Objects.requireNonNull(bytes);
    }

    public void setBytesAfterCheckWithGuava(byte[] bytes) {
        this.bytes = Preconditions.checkNotNull(bytes);
    }
}
