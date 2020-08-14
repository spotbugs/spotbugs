package ghIssues;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Issue390 {

    @Test
    public void test() {
        Integer intVal = 1;
        assertTrue(intVal instanceof Integer);
    }
}
