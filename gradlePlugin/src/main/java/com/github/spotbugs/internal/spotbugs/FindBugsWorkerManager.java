package com.github.spotbugs.internal.spotbugs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.gradle.api.file.FileCollection;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.worker.SingleRequestWorkerProcessBuilder;
import org.gradle.process.internal.worker.WorkerProcessFactory;

public class FindBugsWorkerManager {
  public FindBugsResult runWorker(File workingDir, WorkerProcessFactory workerFactory, FileCollection findBugsClasspath, FindBugsSpec spec) throws IOException, InterruptedException {
      FindBugsWorker worker = createWorkerProcess(workingDir, workerFactory, findBugsClasspath, spec);
      return worker.runFindbugs(spec);
  }

  private FindBugsWorker createWorkerProcess(File workingDir, WorkerProcessFactory workerFactory, FileCollection findBugsClasspath, FindBugsSpec spec) {
      SingleRequestWorkerProcessBuilder<FindBugsWorker> builder = workerFactory.singleRequestWorker(FindBugsWorker.class, FindBugsExecuter.class);
      builder.setBaseName("Gradle FindBugs Worker");
      builder.applicationClasspath(findBugsClasspath);
      builder.sharedPackages(Arrays.asList("edu.umd.cs.findbugs"));
      JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
      javaCommand.setWorkingDir(workingDir);
      javaCommand.setMaxHeapSize(spec.getMaxHeapSize());
      return builder.build();
  }
}