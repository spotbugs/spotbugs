/*
 * Contributions to FindBugs
 * Copyright (C) 2011, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.builder;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.IOConsoleOutputStream;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.view.FindBugsConsole;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Footprint;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.engine.ClassDataAnalysisEngine;
import edu.umd.cs.findbugs.classfile.impl.AnalysisCache;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.log.Profiler.Profile;

public class FindBugs2Eclipse extends FindBugs2 {

    private static WeakHashMap<IProject, SoftReference<List<String>>> auxClassPaths =
        new WeakHashMap<IProject, SoftReference<List<String>>>();

    private static WeakHashMap<IProject, SoftReference<Map<ClassDescriptor, Object>>> classAnalysisCache =
        new WeakHashMap<IProject, SoftReference<Map<ClassDescriptor, Object>>>();

    private AnalysisCache analysisCache;
    private final IProject project;

    private final boolean cacheClassData;

    private final Reporter reporter;

    private static IResourceChangeListener resourceListener = new IResourceChangeListener() {
        public void resourceChanged(IResourceChangeEvent event) {
            if(event.getSource() instanceof IProject) {
                cleanClassClache((IProject) event.getSource());
            } else if (event.getResource() instanceof IProject) {
                cleanClassClache((IProject) event.getResource());
            } else if(event.getDelta() != null) {
                final Set<IProject> affectedProjects = new HashSet<IProject>();
                final IResourceDelta delta = event.getDelta();
                try {
                    delta.accept(new IResourceDeltaVisitor() {
                        public boolean visit(IResourceDelta d1) throws CoreException {
                            if(d1 == delta || d1.getFlags() == 0 || d1.getFlags() == IResourceDelta.MARKERS) {
                                return true;
                            }
                            IResource resource = d1.getResource();
                            if(resource instanceof IProject) {
                                affectedProjects.add((IProject) resource);
                                return false;
                            }
                            return true;
                        }
                    });
                } catch (CoreException e) {
                    FindbugsPlugin.getDefault().logException(e, "Error traversing resource delta");
                }
                for (IProject iProject : affectedProjects) {
                    cleanClassClache(iProject);
                }
            }
        }
    };

    public FindBugs2Eclipse(IProject project, boolean cacheClassData, Reporter bugReporter) {
        super();
        this.project = project;
        this.cacheClassData = cacheClassData;
        if(cacheClassData) {
            int eventMask = IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.PRE_CLOSE;
            ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, eventMask);
        }
        reporter = bugReporter;
    }

    @Override
    protected IAnalysisCache createAnalysisCache() throws IOException {
        IAnalysisCache cache = super.createAnalysisCache();
        if(cache instanceof AnalysisCache) {
            analysisCache = (AnalysisCache)cache;
            if(cacheClassData) {
                reuseClassCache();
            }
        }
        return cache;
    }

    @Override
    protected void clearCaches() {
        if(analysisCache != null) {
            postProcessCaches();
        }
        super.clearCaches();
    }

    @Override
    public void dispose() {
        if(analysisCache != null) {
            analysisCache.dispose();
            analysisCache = null;
        }
        super.dispose();
    }

    private void reuseClassCache() {
        SoftReference<Map<ClassDescriptor, Object>> wr = classAnalysisCache.get(project);
        Map<ClassDescriptor, Object> classAnalysis = wr != null? wr.get() : null;
        if(classAnalysis != null) {
            analysisCache.reuseClassAnalysis(ClassData.class, classAnalysis);
            // TODO would be nice to reuse ClassInfoAnalysisEngine: XClass.class,
            // JavaClassAnalysisEngine: JavaClass.class
            // but unfortunately there are side effects for analysis during data generation
            // which we can't have if we would just re-use the data
        }
    }

    private void postProcessCaches() {
        IClassPath classPath = analysisCache.getClassPath();

        Map<ClassDescriptor, Object> classAnalysis = analysisCache.getClassAnalysis(ClassData.class);
        if(classAnalysis == null) {
            return;
        }
        Set<Entry<ClassDescriptor,Object>> entrySet = classAnalysis.entrySet();
        AnalysisData data = new AnalysisData();
        for (Entry<ClassDescriptor, Object> entry : entrySet) {
            data.classCount ++;
            if(!(entry.getValue() instanceof ClassData)) {
                continue;
            }
            ClassData cd = (ClassData) entry.getValue();
            data.byteSize += cd.getData().length;
        }
        Set<Entry<String, ICodeBaseEntry>> entrySet2 = classPath.getApplicationCodebaseEntries().entrySet();
        DescriptorFactory descriptorFactory = DescriptorFactory.instance();
        for (Entry<String, ICodeBaseEntry> entry : entrySet2) {
            String className = entry.getKey();
            if(cacheClassData) {
                if(className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6);
                }
                classAnalysis.remove(descriptorFactory.getClassDescriptor(className));
            }
            data.byteSizeApp += entry.getValue().getNumBytes();
        }
        if(cacheClassData) {
            // create new reference not reachable to anyone except us
            classAnalysis = new HashMap<ClassDescriptor, Object>(classAnalysis);
            classAnalysisCache.put(project, new SoftReference<Map<ClassDescriptor, Object>>(classAnalysis));
        }
        reportExtraData(data);
    }

    @SuppressWarnings("boxing")
    private void reportExtraData(AnalysisData data) {
        SortedBugCollection bugCollection = reporter.getBugCollection();
        if(bugCollection == null) {
            return;
        }
        if (FindBugsConsole.getConsole() == null) {
            return;
        }
        IOConsoleOutputStream out = FindBugsConsole.getConsole().newOutputStream();
        PrintWriter pw = new PrintWriter(out);

        ProjectStats stats = bugCollection.getProjectStats();
        Footprint footprint = new Footprint(stats.getBaseFootprint());
        Profiler profiler = stats.getProfiler();
        Profile profile = profiler.getProfile(ClassDataAnalysisEngine.class);
        long totalClassReadTime = TimeUnit.MILLISECONDS.convert(profile.getTotalTime(), TimeUnit.NANOSECONDS);
        long totalTime = TimeUnit.MILLISECONDS.convert(footprint.getClockTime(), TimeUnit.MILLISECONDS);

        double classReadSpeed = totalClassReadTime > 0? data.byteSize * 1000.0 / totalClassReadTime : 0;
        double classCountSpeed = totalTime > 0? data.classCount * 1000.0 / totalTime : 0;
        double classPart = totalTime > 0? totalClassReadTime * 100.0 / totalTime : 0;
        double appPart = data.byteSize > 0? data.byteSizeApp * 100.0 / data.byteSize : 0;
        double bytesPerClass = data.classCount > 0? ((double) data.byteSize)  / data.classCount : 0;
        long peakMemory = footprint.getPeakMemory() / (1024 * 1024);
        pw.printf("%n");
        pw.printf("Total bugs            : %1$ 20d %n", stats.getTotalBugs());
        pw.printf("Peak memory (MB)      : %1$ 20d %n", peakMemory);
        pw.printf("Total classes         : %1$ 20d %n", data.classCount);
        pw.printf("Total time (msec)     : %1$ 20d %n", totalTime);
        pw.printf("Class read time (msec): %1$ 20d %n", totalClassReadTime);
        pw.printf("Class read time (%%)   : %1$ 20.0f %n", classPart);
        pw.printf("Total bytes read      : %1$ 20d %n", data.byteSize);
        pw.printf("Application bytes     : %1$ 20d %n", data.byteSizeApp);
        pw.printf("Application bytes (%%) : %1$ 20.0f %n", appPart);
        pw.printf("Avg. bytes per class  : %1$ 20.0f %n", bytesPerClass);
        pw.printf("Analysis class/sec    : %1$ 20.0f %n", classCountSpeed);
        pw.printf("Read     bytes/sec    : %1$ 20.0f %n", classReadSpeed);
        pw.printf("            MB/sec    : %1$ 20.1f %n", classReadSpeed / (1024 * 1024));
        pw.flush();

        pw.close();

    }

    static class AnalysisData {
        long byteSize;
        long byteSizeApp;
        long classCount;
    }

    public static void cleanClassClache(IProject project) {
        auxClassPaths.remove(project);
        classAnalysisCache.remove(project);
    }

    static void checkClassPathChanges(List<String> auxClassPath, IProject project) {
        SoftReference<List<String>> wr = auxClassPaths.get(project);
        List<String> oldAuxCp = wr != null ? wr.get() : null;
        if(oldAuxCp != null && !oldAuxCp.equals(auxClassPath)) {
            auxClassPaths.put(project, new SoftReference<List<String>>(new ArrayList<String>(auxClassPath)));
            classAnalysisCache.remove(project);
        } else if(oldAuxCp == null){
            auxClassPaths.put(project, new SoftReference<List<String>>(new ArrayList<String>(auxClassPath)));
        }
    }
}
