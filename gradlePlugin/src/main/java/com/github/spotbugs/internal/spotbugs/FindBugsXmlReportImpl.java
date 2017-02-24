package com.github.spotbugs.internal.spotbugs;

import org.gradle.api.Task;
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport;

import com.github.spotbugs.FindBugsXmlReport;

public class FindBugsXmlReportImpl extends TaskGeneratedSingleFileReport implements FindBugsXmlReport {
  private static final long serialVersionUID = 1L;
  private boolean withMessages;
  
  public FindBugsXmlReportImpl(String name, Task task) {
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