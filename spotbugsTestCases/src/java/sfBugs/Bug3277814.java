package sfBugs;

public class Bug3277814 {

    public void test() {
        String var = "";
        int index = 2;
        if (index == -1) {
            var = String.class.getName();
            if (var.length() == 0) {
                var = null;
            }
        } else {
            var = Integer.class.getName();
            if (var.length() == 0) {
                var = null;
            }
        }
        if (var == null) {// FINBUGS reports on this line NP_GUARANTEED_DEREF
            /*
             * There is a statement or branch that if executed guarantees that a
             * value is null at this point, and that value that is guaranteed to
             * be dereferenced (except on forward paths involving runtime
             * exceptions).
             */
            throw new RuntimeException("NULL");
        }
    }

    public void test2() {
        String var = "";
        int index = 2;
        if (index == -1) {
            var = String.class.getName();
            if (var.length() == 0) {
                var = null;
            }
        } else {
            var = Integer.class.getName();
            if (var.length() == 0) {
                var = null;
            }
        }
        if (var == null) {// FINBUGS reports on this line NP_GUARANTEED_DEREF
            /*
             * There is a statement or branch that if executed guarantees that a
             * value is null at this point, and that value that is guaranteed to
             * be dereferenced (except on forward paths involving runtime
             * exceptions).
             */
            throw new NullPointerException("var is null");
        }
    }
}
