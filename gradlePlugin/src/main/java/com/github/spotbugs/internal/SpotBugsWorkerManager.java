package com.github.spotbugs.internal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.gradle.api.file.FileCollection;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.worker.SingleRequestWorkerProcessBuilder;
import org.gradle.process.internal.worker.WorkerProcessFactory;

public class SpotBugsWorkerManager {
  public SpotBugsResult runWorker(File workingDir, WorkerProcessFactory workerFactory, FileCollection SpotBugsClasspath, SpotBugsSpec spec) throws IOException, InterruptedException {
    SpotBugsWorker worker = createWorkerProcess(workingDir, workerFactory, SpotBugsClasspath, spec);
    return worker.runSpotBugs(spec);
}

private SpotBugsWorker createWorkerProcess(File workingDir, WorkerProcessFactory workerFactory, FileCollection SpotBugsClasspath, SpotBugsSpec spec) {
    SingleRequestWorkerProcessBuilder<SpotBugsWorker> builder = workerFactory.singleRequestWorker(SpotBugsWorker.class, SpotBugsExecuter.class);
    builder.setBaseName("Gradle SpotBugs Worker");
    builder.applicationClasspath(SpotBugsClasspath);
    builder.sharedPackages(Arrays.asList("edu.umd.cs.FindBugs"));
    JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
    javaCommand.setWorkingDir(workingDir);
    javaCommand.setMaxHeapSize(spec.getMaxHeapSize());
    return builder.build();
}

}
