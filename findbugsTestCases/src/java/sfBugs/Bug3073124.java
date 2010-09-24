package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3073124 {

    void methodA() {
    }
    void methodB() {
    }

    static class Derived extends Bug3073124 {
        boolean condition;

        @ExpectWarning("IL_INFINITE_RECURSIVE_LOOP")
        void methodA() {

            if (condition) {
                methodA();
            }
        }

        // Corrected code
        @NoWarning("IL_INFINITE_RECURSIVE_LOOP")
        void methodB() {

            if (condition) {
                super.methodB();
            }
        }
    }
}
