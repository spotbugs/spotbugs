package infiniteLoop;

/**
 * IL_INFINITE_LOOP must not depend on the operand order of the loop condition.
 * {@code "foo".trim().length()} is 3, so the "== 0" loops exit immediately and the "!= 0" loops never exit.
 */
public class Issue4438 {

    void alwaysFalse() {
        String s = "foo";
        while (s.trim().length() == 0) {
        }
    }

    void alwaysFalseSwapped() {
        String s = "foo";
        while (0 == s.trim().length()) {
        }
    }

    void doWhileAlwaysFalse() {
        String s = "foo";
        do {
        } while (s.trim().length() == 0);
    }

    void doWhileAlwaysFalseSwapped() {
        String s = "foo";
        do {
        } while (0 == s.trim().length());
    }

    void alwaysTrue() {
        String s = "foo";
        while (s.trim().length() != 0) {
        }
    }

    void alwaysTrueSwapped() {
        String s = "foo";
        while (0 != s.trim().length()) {
        }
    }
}
