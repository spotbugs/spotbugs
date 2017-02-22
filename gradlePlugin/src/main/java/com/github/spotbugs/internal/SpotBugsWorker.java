package com.github.spotbugs.internal;

import java.io.IOException;

public interface SpotBugsWorker {
  SpotBugsResult runSpotBugs(SpotBugsSpec spec) throws IOException, InterruptedException;
}
