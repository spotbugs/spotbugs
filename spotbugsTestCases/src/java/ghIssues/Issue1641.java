package ghIssues;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/1641">#1641</a>.
 */
public class Issue1641 {

    public boolean getCondition() {
        return true;
    }

    public void methodA() {
        boolean condition = getCondition();
        if (condition) {
            methodA();
        } else {
            methodA();
        }
    }
}
