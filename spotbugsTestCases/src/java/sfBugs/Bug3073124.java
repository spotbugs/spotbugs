package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3073124 {

    void methodA() {
    }

    void methodB() {
    }

    void testLoop(String b) {

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

        void testLoop(String b) {
            boolean test = evaluate();
            if (test) {
                testLoop("dfdsfsd");
            } else {
                super.testLoop("dsdsd");
            }

        }

        boolean evaluate() {
            return true;
        }

    }
}
