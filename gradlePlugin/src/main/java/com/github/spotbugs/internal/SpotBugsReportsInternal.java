package com.github.spotbugs.internal;

import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import com.github.spotbugs.reporting.SpotBugsReports;

public interface SpotBugsReportsInternal extends SpotBugsReports {
  @Internal
  SingleFileReport getFirstEnabled();

  @Input
  @Optional
  Boolean getWithMessagesFlag();
}