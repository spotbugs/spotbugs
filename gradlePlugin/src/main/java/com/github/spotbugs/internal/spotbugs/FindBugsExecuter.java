package com.github.spotbugs.internal.spotbugs;

import java.io.IOException;
import java.util.List;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.TextUICommandLine;

public class FindBugsExecuter implements FindBugsWorker {
  @Override
  public FindBugsResult runFindbugs(FindBugsSpec spec) throws IOException, InterruptedException {
      final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
          final List<String> args = spec.getArguments();
          String[] strArray = args.toArray(new String[0]);

          Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());
          FindBugs2 findBugs2 = new FindBugs2();
          TextUICommandLine commandLine = new TextUICommandLine();
          FindBugs.processCommandLine(commandLine, strArray, findBugs2);
          findBugs2.execute();

          return createFindbugsResult(findBugs2);
      } finally {
          Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
  }

  FindBugsResult createFindbugsResult(IFindBugsEngine findBugs) {
          int bugCount = findBugs.getBugCount();
          int missingClassCount = findBugs.getMissingClassCount();
          int errorCount = findBugs.getErrorCount();
          return new FindBugsResult(bugCount, missingClassCount, errorCount);
      }
}