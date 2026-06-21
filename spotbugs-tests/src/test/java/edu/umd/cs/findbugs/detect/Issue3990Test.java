package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3990Test extends AbstractIntegrationTest {

    private static final String CLASS = "ghIssues.Issue3990";

    @Test
    void testDirectHardcodedPassword() {
        performAnalysis("ghIssues/Issue3990.class");
        assertBugInMethodAtLine("DMI_CONSTANT_DB_PASSWORD", CLASS, "directHardcodedPassword", 12);
    }

    @Test
    void testPropertiesPassword() {
        performAnalysis("ghIssues/Issue3990.class");
        assertBugInMethodAtLine("DMI_CONSTANT_DB_PASSWORD", CLASS, "propertiesPassword", 18);
    }

    @Test
    void testUrlEmbeddedPassword() {
        performAnalysis("ghIssues/Issue3990.class");
        assertBugInMethodAtLine("DMI_CONSTANT_DB_PASSWORD", CLASS, "urlEmbeddedPassword", 22);
    }

    @Test
    void testLocalVariablePassword() {
        performAnalysis("ghIssues/Issue3990.class");
        assertBugInMethodAtLine("DMI_CONSTANT_DB_PASSWORD", CLASS, "localVariablePassword", 27);
    }

    @Test
    void testEmptyPassword() {
        performAnalysis("ghIssues/Issue3990.class");
        assertBugInMethodAtLine("DMI_EMPTY_DB_PASSWORD", CLASS, "emptyPassword", 32);
    }

    @Test
    void testExtractJdbcPassword() {
        org.junit.jupiter.api.Assertions.assertEquals("secret", DumbMethodInvocations.extractJdbcPassword(
                "jdbc:mysql://localhost/db?user=admin&password=secret"));
        org.junit.jupiter.api.Assertions.assertNull(DumbMethodInvocations.extractJdbcPassword(
                "jdbc:mysql://localhost/db?user=admin"));
    }
}
