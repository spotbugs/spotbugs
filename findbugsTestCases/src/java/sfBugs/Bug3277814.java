package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3277814 {

    @ExpectWarning("NP_GUARANTEED_DEREF")
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
            throw new NullPointerException("NULL");
        }
    }
}
