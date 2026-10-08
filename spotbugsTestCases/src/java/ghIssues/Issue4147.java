package ghIssues;

public class Issue4147 {

    Object x;

    int directForm() {
        if (x == null) {
            System.out.println("x is null");
        }
        if (x == null) {
            throw new NullPointerException();
        } else {
            return x.hashCode();
        }
    }

    int yodaForm() {
        if (null == x) {
            System.out.println("x is null");
        }
        if (null == x) {
            throw new NullPointerException();
        } else {
            return x.hashCode();
        }
    }

    int dereferencedWhenNull(boolean clear) {
        x = new Object();
        if (clear) {
            x = null;
        }
        return x.hashCode();
    }
}
