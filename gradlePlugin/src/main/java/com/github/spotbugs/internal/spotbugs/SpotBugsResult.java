package com.github.spotbugs.internal.spotbugs;

import java.io.Serializable;

public class SpotBugsResult implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int bugCount;
  private final int missingClassCount;
  private final int errorCount;
  private final Throwable exception;

  public SpotBugsResult(int bugCount, int missingClassCount, int errorCount) {
      this(bugCount, missingClassCount, errorCount, null);
  }

  public SpotBugsResult(int bugCount, int missingClassCount, int errorCount, Throwable exception) {
      this.bugCount = bugCount;
      this.missingClassCount = missingClassCount;
      this.errorCount = errorCount;
      this.exception = exception;
  }

  public int getBugCount() {
      return bugCount;
  }

  public int getMissingClassCount() {
      return missingClassCount;
  }

  public int getErrorCount() {
      return errorCount;
  }

  public Throwable getException() {
      return exception;
  }
}