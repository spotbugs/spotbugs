package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class Issue3999Test extends AbstractIntegrationTest {
  @Test
  void generatedOnClassIsConsidered() {
    performAnalysis("ghIssues/issue3999/GeneratedAnnotationOnClass.class", "ghIssues/issue3999/Generated.class");
    assertBugTypeCount("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED", 0);
  }

  @Test
  void generatedOnMethodIsConsidered() {
    performAnalysis("ghIssues/issue3999/GeneratedAnnotationOnMethod.class", "ghIssues/issue3999/Generated.class");
    assertBugTypeCount("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED", 0);
  }
}
