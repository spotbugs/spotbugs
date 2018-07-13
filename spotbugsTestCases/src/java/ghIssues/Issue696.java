package ghIssues;

import java.math.BigDecimal;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/696">GitHub issue</a>
 */
public class Issue696 {
    @SuppressWarnings("unused")
    public BigDecimal create() {
        BigDecimal bd = new BigDecimal(getDoubleValue());
        return bd;
    }

    private static double getDoubleValue() {
        return 2.5d;
    }
}
