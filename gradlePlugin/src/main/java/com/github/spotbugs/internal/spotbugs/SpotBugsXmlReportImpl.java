package com.github.spotbugs.internal.spotbugs;

import org.gradle.api.Task;
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport;

import com.github.spotbugs.SpotBugsXmlReport;

public class SpotBugsXmlReportImpl extends TaskGeneratedSingleFileReport implements SpotBugsXmlReport {
  private static final long serialVersionUID = 1L;
  private boolean withMessages;
  
  public SpotBugsXmlReportImpl(String name, Task task) {
      super(name, task);
  }

  @Override
  public boolean isWithMessages() {
      return withMessages;
  }

  @Override
  public void setWithMessages(boolean withMessages) {
      this.withMessages = withMessages;
  }

}