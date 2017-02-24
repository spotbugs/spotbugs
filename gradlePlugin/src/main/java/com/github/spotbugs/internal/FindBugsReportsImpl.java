package com.github.spotbugs.internal;

import org.gradle.api.Task;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.reporting.internal.CustomizableHtmlReportImpl;
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport;
import org.gradle.api.reporting.internal.TaskReportContainer;

import com.github.spotbugs.FindBugsXmlReport;
import com.github.spotbugs.internal.spotbugs.FindBugsXmlReportImpl;

public class FindBugsReportsImpl extends TaskReportContainer<SingleFileReport> implements FindBugsReportsInternal {

  public FindBugsReportsImpl(Task task) {
      super(SingleFileReport.class, task);

      add(FindBugsXmlReportImpl.class, "xml", task);
      add(CustomizableHtmlReportImpl.class, "html", task);
      add(TaskGeneratedSingleFileReport.class, "text", task);
      add(TaskGeneratedSingleFileReport.class, "emacs", task);
  }

  public FindBugsXmlReport getXml() {
      return (FindBugsXmlReport) getByName("xml");
  }

  public SingleFileReport getHtml() {
      return getByName("html");
  }

  public SingleFileReport getText() {
      return getByName("text");
  }

  public SingleFileReport getEmacs() {
      return getByName("emacs");
  }

  @Override
  public Boolean getWithMessagesFlag() {
      FindBugsXmlReport report = (FindBugsXmlReport)getEnabled().findByName("xml");
      return report != null ? report.isWithMessages() : Boolean.FALSE;
  }
}