package com.github.spotbugs.internal.spotbugs;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

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

  @Override
public String toString() {
      return new ToStringBuilder(this).append("arguments", arguments).append("debugEnabled", debugEnabled).toString();
  }
}