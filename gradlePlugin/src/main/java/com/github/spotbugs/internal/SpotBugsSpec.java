package com.github.spotbugs.internal;

import java.io.Serializable;
import java.util.List;

import org.gradle.internal.impldep.com.google.common.base.Objects;

public class SpotBugsSpec implements Serializable {
  private static final long serialVersionUID = 1L;
  private List<String> arguments;
  private String maxHeapSize;
  private boolean debugEnabled;

  public SpotBugsSpec(List<String> arguments, String maxHeapSize, boolean debugEnabled) {
      this.debugEnabled = debugEnabled;
      this.maxHeapSize = maxHeapSize;
      this.arguments = arguments;
  }

  public List<String> getArguments() {
      return arguments;
  }

  public String getMaxHeapSize() {
      return maxHeapSize;
  }
  
  public boolean isDebugEnabled() {
      return debugEnabled;
  }
  
  public String toString() {
      return Objects.toStringHelper(this).add("arguments", arguments).add("debugEnabled", debugEnabled).toString();
  }
}