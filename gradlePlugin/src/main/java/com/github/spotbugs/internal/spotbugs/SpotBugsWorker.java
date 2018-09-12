package com.github.spotbugs.internal.spotbugs;

import java.io.IOException;

public interface SpotBugsWorker {
    SpotBugsResult runSpotbugs(SpotBugsSpec spec) throws IOException, InterruptedException;
}