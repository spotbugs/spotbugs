package ghIssues;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Issue390 {

    // Should not trigger the lint.
    public void not_test() {
        Integer intVal = 1;
        assertTrue(intVal instanceof Integer);
    }

    @Test
    public void test() {
        Integer intVal = 1;
        assertTrue(intVal instanceof Integer);
    }
}
