package ghIssues.issue3999;

import ghIssues.issue3396.Generated;
import jsr305.Strict;

@Generated
public class GeneratedAnnotationOnClass {
  @Strict
  Object f;

  public void violation(Object unknown) {
    f = unknown;
  }
}
