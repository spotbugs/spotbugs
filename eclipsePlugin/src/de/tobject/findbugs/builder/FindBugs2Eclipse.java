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
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;

import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.impl.AnalysisCache;

public class FindBugs2Eclipse extends FindBugs2 {

    private static WeakHashMap<IProject, SoftReference<List<String>>> auxClassPaths =
        new WeakHashMap<IProject, SoftReference<List<String>>>();

    private static WeakHashMap<IProject, SoftReference<Map<ClassDescriptor, Object>>> classAnalysisCache =
        new WeakHashMap<IProject, SoftReference<Map<ClassDescriptor, Object>>>();

    private AnalysisCache analysisCache;
    private final IProject project;

    public FindBugs2Eclipse(IProject project) {
        super();
        this.project = project;
    }

    @Override
    protected IAnalysisCache createAnalysisCache() throws IOException {
        IAnalysisCache cache = super.createAnalysisCache();
        if(cache instanceof AnalysisCache) {
            analysisCache = (AnalysisCache)cache;
            reuseClassCache();
        }
        return cache;
    }

    @Override
    protected void clearCaches() {
        if(analysisCache != null) {
            rememberClassCache();
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

    // TODO should add resource listener and track also "close" operation on projects
    private void rememberClassCache() {
        IClassPath classPath = analysisCache.getClassPath();

        Map<ClassDescriptor, Object> classAnalysis = analysisCache.getClassAnalysis(ClassData.class);
        if(classAnalysis == null) {
            return;
        }
        Set<String> keySet = classPath.getApplicationCodebaseEntries().keySet();
        DescriptorFactory descriptorFactory = DescriptorFactory.instance();
        for (String className : keySet) {
            if(className.endsWith(".class")) {
                className = className.substring(0, className.length() - 6);
            }
            classAnalysis.remove(descriptorFactory.getClassDescriptor(className));
        }
        // create new reference not reachable to anyone except us
        classAnalysis = new HashMap<ClassDescriptor, Object>(classAnalysis);
        classAnalysisCache.put(project, new SoftReference<Map<ClassDescriptor, Object>>(classAnalysis));
    }

    static void cleanBuild(IProject project) {
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
