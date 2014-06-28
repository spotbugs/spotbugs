/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2004-2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.reporter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.console.IOConsoleOutputStream;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugs2Eclipse;
import de.tobject.findbugs.util.ConfigurableXmlOutputStream;
import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.Footprint;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * The <code>Reporter</code> is a class that is called by the FindBugs engine in
 * order to record and report bugs that have been found. This implementation
 * displays the bugs found as tasks in the task view.
 *
 * @author Peter Friese
 * @author David Hovemeyer
 * @version 1.0
 * @since 28.07.2003
 */
public class Reporter extends AbstractBugReporter implements FindBugsProgress {

    /** Controls debugging for the reporter */
    public static boolean DEBUG;

    private final IJavaProject project;

    private final IProgressMonitor monitor;

    /** Persistent store of reported warnings. */
    private final SortedBugCollection bugCollection;

    private int pass = -1;

    private int bugCount;

    private IOConsoleOutputStream stream;

    /**
     * Constructor.
     *
     * @param project
     *            the project whose classes are being analyzed for bugs
     * @param monitor
     *            progress monitor
     */
    public Reporter(IJavaProject project, Project findBugsProject, IProgressMonitor monitor) {
        super();
        if (DEBUG) {
            printToStream("Eclipse FindBugs plugin REPORTER debugging enabled");
        }
        this.monitor = monitor;
        this.project = project;
        // TODO we do not need to sort bugs, so we can optimize performance and
        // use just a list here
        this.bugCollection = new SortedBugCollection(getProjectStats(), findBugsProject);
    }

    public void setReportingStream(IOConsoleOutputStream stream) {
        this.stream = stream;
    }

    boolean isStreamReportingEnabled() {
        return stream != null && !stream.isClosed();
    }

    void printToStream(String message) {
        if (DEBUG) {
            FindbugsPlugin.log(message);
            System.out.println(message);
        }
        if (isStreamReportingEnabled()) {
            try {
                stream.write(message);
                stream.write("\n");
            } catch (IOException e) {
                FindbugsPlugin.getDefault().logException(e, "Print to console failed");
            }
        }
    }

    @Override
    protected void doReportBug(BugInstance bug) {
        synchronized (bugCollection) {
            if (bugCollection.add(bug)) {
                notifyObservers(bug);
                bugCount++;
            } else if (DEBUG) {
                System.out.println("Duplicated bug added: " + bug);
            }
        }

    }

    @Override
    public void reportQueuedErrors() {
        // Report unique errors in order of their sequence
        List<Error> errorList = new ArrayList<Error>(getQueuedErrors());
        if (errorList.size() > 0) {
            Collections.sort(errorList, new Comparator<Error>() {
                @Override
                public int compare(Error o1, Error o2) {
                    return o1.getSequence() - o2.getSequence();
                }
            });

            MultiStatus status = new MultiStatus(FindbugsPlugin.PLUGIN_ID, IStatus.ERROR,
                    "The following errors occurred during FindBugs analysis:", null);

            for (Error error : errorList) {
                status.add(FindbugsPlugin.createErrorStatus(error.getMessage(), error.getCause()));
            }
            FindbugsPlugin.getDefault().getLog().log(status);
        }

        Set<String> missingClasses = getMissingClasses();
        if (missingClasses.size() > 0) {
            FindBugs2Eclipse.cleanClassClache(project.getProject());
            MultiStatus status = new MultiStatus(FindbugsPlugin.PLUGIN_ID, IStatus.WARNING,
                    "The following classes needed for FindBugs analysis on project " + project.getElementName()
                            + " were missing:", null);
            for (String missingClass : missingClasses) {
                status.add(new Status(IStatus.WARNING, FindbugsPlugin.PLUGIN_ID, missingClass));
            }
            FindbugsPlugin.getDefault().getLog().log(status);
        }
    }

    @Override
    public void finish() {
        if (DEBUG) {
            System.out.println("Finish: Found " + bugCount + " bugs."); //$NON-NLS-1$//$NON-NLS-2$
        }
        Cloud cloud = bugCollection.getCloud();
        if (cloud != null) {
            cloud.bugsPopulated();
        }
        reportResultsToConsole();
    }

    /**
     * If there is a FB console opened, report results and statistics to it.
     */
    private void reportResultsToConsole() {
        if (!isStreamReportingEnabled()) {
            return;
        }
        printToStream("Finished, found: " + bugCount + " bugs");
        ConfigurableXmlOutputStream xmlStream = new ConfigurableXmlOutputStream(stream, true);
        ProjectStats stats = bugCollection.getProjectStats();

        printToStream("\nFootprint: " + new Footprint(stats.getBaseFootprint()).toString());

        Profiler profiler = stats.getProfiler();
        PrintStream printStream;
        try {
            printStream = new PrintStream(stream, false, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            // can never happen with UTF-8
            return;
        }

        printToStream("\nTotal time:");
        profiler.report(new Profiler.TotalTimeComparator(profiler), new Profiler.FilterByTime(10000000), printStream);

        printToStream("\nTotal calls:");
        int numClasses = stats.getNumClasses();
        if(numClasses > 0) {
            profiler.report(new Profiler.TotalCallsComparator(profiler), new Profiler.FilterByCalls(numClasses),
                    printStream);

            printToStream("\nTime per call:");
            profiler.report(new Profiler.TimePerCallComparator(profiler),
                    new Profiler.FilterByTimePerCall(10000000 / numClasses), printStream);
        }
        try {
            xmlStream.finish();
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "Print to console failed");
        }
    }

    /**
     * Returns the list of bugs found in this project. If the list has not been
     * initialized yet, this will be done before returning.
     *
     * @return The collection that hold the bugs found in this project.
     */
    @Override
    public SortedBugCollection getBugCollection() {
        return bugCollection;
    }

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
        String className = classDescriptor.getDottedClassName();

//        if (DEBUG) {
//            System.out.println("Observing class: " + className); //$NON-NLS-1$
//        }

        if (monitor.isCanceled()) {
            // causes break in FindBugs main loop
            Thread.currentThread().interrupt();
        }

        int work = (pass * 99) + 1;


        // Update progress monitor
        if (pass <= 0) {
            monitor.setTaskName("Prescanning... (found " + bugCount + ", checking " + className + ")");
        } else {
            monitor.setTaskName("Checking... (pass #" + pass + ") (found " + bugCount + ", checking " + className + ")");
        }
        monitor.worked(work);
        // printToStream("observeClass: " + className);
    }

    @Override
    public void reportAnalysisError(AnalysisError error) {
        // nothing to do, see reportQueuedErrors()
    }

    @Override
    public void reportMissingClass(String missingClass) {
        // nothing to do, see reportQueuedErrors()
    }

    public BugReporter getRealBugReporter() {
        return this;
    }

    @Override
    public void finishArchive() {
        // printToStream("archive finished");
    }

    @Override
    public void finishClass() {
        // noop
    }

    @Override
    public void finishPerClassAnalysis() {
        // printToStream("finish per class analysis");
    }

    @Override
    public void reportNumberOfArchives(int numArchives) {
        printToStream("\nStarting FindBugs analysis on file(s) from: " + project.getElementName());
        List<String> classpathEntryList = new ArrayList<String>(bugCollection.getProject().getAuxClasspathEntryList());
        printToStream("\nResolved auxiliary classpath (" + classpathEntryList.size() + " entries):");
        for (String path : classpathEntryList) {
            printToStream("\t " + path);
        }
        printToStream("\nArchives to analyze: " + numArchives);
    }

    @Override
    public void startAnalysis(int numClasses) {
        pass++;
        printToStream("Start pass: " + pass + " on " + numClasses + " classes");
    }

    @Override
    public void predictPassCount(int[] classesPerPass) {
        int expectedWork = 0;
        for (int i = 0; i < classesPerPass.length; i++) {
            int count = classesPerPass[i];
            expectedWork += ((i * 99) + 1) * count;
        }
        if (!(monitor instanceof SubProgressMonitor)) {
            monitor.beginTask("Performing bug checking...", expectedWork);
        }
    }

    @Override
    public void startArchive(String name) {
        if (DEBUG) {
            printToStream("start archive: " + name);
        }
    }


}
