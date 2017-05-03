package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3062023 {
    public enum E {
        A, B, C, D
    }

    @NoWarning("DB_DUPLICATE_SWITCH_CLAUSES")
    public void noWarning(E e) {
        switch (e) {
        case A:
            something(1);
            break;
        case B:
            // Nothing to report
            break;
        case C:
            something(2);
            break;
        case D:
            break;
        default:
            something(10);
            break;
        }
    }

    private void something(int i) {
    }
}
