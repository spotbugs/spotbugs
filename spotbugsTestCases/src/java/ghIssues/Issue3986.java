package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3986">#3986</a>.
 */
public class Issue3986 {
    Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @ExpectWarning("IS2_INCONSISTENT_SYNC")
    public synchronized int getHashWithoutNullCheck() {
        return data.hashCode();
    }

    @ExpectWarning("IS2_INCONSISTENT_SYNC")
    public synchronized int getHashWithNullCheck() {
        if (data != null) {
            return data.hashCode();
        }
        return data.hashCode();
    }
}
