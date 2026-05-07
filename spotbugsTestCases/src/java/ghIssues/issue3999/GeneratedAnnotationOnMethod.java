package ghIssues.issue3999;

import jsr305.Strict;

public class GeneratedAnnotationOnMethod {
  @Strict
  Object f;

  @Generated
  public void violation(Object unknown) {
    f = unknown;
  }

  public void violation2(Object unknown) {
    f = unknown;
  }
}
