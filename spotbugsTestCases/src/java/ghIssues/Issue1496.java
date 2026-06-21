package ghIssues;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/1496">#1496</a>.
 */
public class Issue1496 {

    public boolean compareClassNames(final Object obj) {
        return obj.getClass().getName().equals(obj.getClass().getName());
    }

    public boolean equalsStyle(final Object obj) {
        return getClass().getName().equals(obj.getClass().getName());
    }
}
