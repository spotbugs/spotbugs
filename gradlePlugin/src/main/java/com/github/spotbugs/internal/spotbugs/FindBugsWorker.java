package com.github.spotbugs.internal.spotbugs;

import java.io.IOException;

public interface FindBugsWorker {
    FindBugsResult runFindbugs(FindBugsSpec spec) throws IOException, InterruptedException;
}