package com.github.spotbugs.internal;

import org.gradle.api.Task;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.reporting.internal.CustomizableHtmlReportImpl;
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport;
import org.gradle.api.reporting.internal.TaskReportContainer;

import com.github.spotbugs.SpotBugsXmlReport;
import com.github.spotbugs.internal.spotbugs.SpotBugsXmlReportImpl;

public class SpotBugsReportsImpl extends TaskReportContainer<SingleFileReport> implements SpotBugsReportsInternal {

  public SpotBugsReportsImpl(Task task) {
      super(SingleFileReport.class, task);

      add(SpotBugsXmlReportImpl.class, "xml", task);
      add(CustomizableHtmlReportImpl.class, "html", task);
      add(TaskGeneratedSingleFileReport.class, "text", task);
      add(TaskGeneratedSingleFileReport.class, "emacs", task);
  }

  @Override
public SpotBugsXmlReport getXml() {
      return (SpotBugsXmlReport) getByName("xml");
  }

  @Override
public SingleFileReport getHtml() {
      return getByName("html");
  }

  @Override
public SingleFileReport getText() {
      return getByName("text");
  }

  @Override
public SingleFileReport getEmacs() {
      return getByName("emacs");
  }

  @Override
  public Boolean getWithMessagesFlag() {
      SpotBugsXmlReport report = (SpotBugsXmlReport)getEnabled().findByName("xml");
      return report != null ? report.isWithMessages() : Boolean.FALSE;
  }
}