package com.github.spotbugs.internal.spotbugs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.gradle.api.file.FileCollection;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.worker.SingleRequestWorkerProcessBuilder;
import org.gradle.process.internal.worker.WorkerProcessFactory;

public class SpotBugsWorkerManager {
  public SpotBugsResult runWorker(File workingDir, WorkerProcessFactory workerFactory, FileCollection findBugsClasspath, SpotBugsSpec spec) throws IOException, InterruptedException {
      SpotBugsWorker worker = createWorkerProcess(workingDir, workerFactory, findBugsClasspath, spec);
      return worker.runSpotbugs(spec);
  }

  private SpotBugsWorker createWorkerProcess(File workingDir, WorkerProcessFactory workerFactory, FileCollection findBugsClasspath, SpotBugsSpec spec) {
      SingleRequestWorkerProcessBuilder<SpotBugsWorker> builder = workerFactory.singleRequestWorker(SpotBugsWorker.class, SpotBugsExecuter.class);
      builder.setBaseName("Gradle SpotBugs Worker");
      builder.applicationClasspath(findBugsClasspath);
      builder.sharedPackages(Arrays.asList("edu.umd.cs.findbugs"));
      JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
      javaCommand.setWorkingDir(workingDir);
      javaCommand.setMaxHeapSize(spec.getMaxHeapSize());
      return builder.build();
  }
}