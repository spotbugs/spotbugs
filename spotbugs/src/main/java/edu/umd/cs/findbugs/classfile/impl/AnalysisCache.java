/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.impl;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.asm.FBClassReader;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IDatabaseFactory;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.UncheckedAnalysisException;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * Implementation of IAnalysisCache. This object is responsible for registering
 * class and method analysis engines and caching analysis results.
 *
 * @author David Hovemeyer
 */
public class AnalysisCache implements IAnalysisCache {
    /**
     *
     */
    private static final int MAX_JAVACLASS_RESULTS_TO_CACHE = 3000;
    private static final int MAX_FBCLASSREADER_RESULTS_TO_CACHE = 3000;

    private static final int MAX_CONSTANT_POOL_GEN_RESULTS_TO_CACHE = 500;

    /**
     * Maximum number of class analysis results to cache.
     */
    private static final int MAX_CLASS_RESULTS_TO_CACHE = 5000;

    //    private static final boolean ASSERTIONS_ENABLED = SystemProperties.ASSERTIONS_ENABLED;

    // Fields
    private final IClassPath classPath;

    private final BugReporter bugReporter;

    private final Map<Class<?>, IClassAnalysisEngine<?>> classAnalysisEngineMap;

    private final Map<Class<?>, IMethodAnalysisEngine<?>> methodAnalysisEngineMap;

    private final Map<Class<?>, IDatabaseFactory<?>> databaseFactoryMap;

    private final Map<Class<?>, Map<ClassDescriptor, Object>> classAnalysisMap;

    private final Map<Class<?>, Object> databaseMap;

    private final Map<?, ?> analysisLocals = Collections.synchronizedMap(new HashMap<Object, Object>());

    @Override
    public final Map<?, ?> getAnalysisLocals() {
        return analysisLocals;
    }

    static class AbnormalAnalysisResult {
        final CheckedAnalysisException checkedAnalysisException;

        final RuntimeException runtimeException;

        final boolean isNull;

        AbnormalAnalysisResult(CheckedAnalysisException checkedAnalysisException) {
            this.checkedAnalysisException = checkedAnalysisException;
            this.runtimeException = null;
            isNull = false;
        }

        AbnormalAnalysisResult(RuntimeException runtimeException) {
            this.runtimeException = runtimeException;
            this.checkedAnalysisException = null;
            isNull = false;
        }

        AbnormalAnalysisResult() {
            this.isNull = true;
            this.checkedAnalysisException = null;
            this.runtimeException = null;
        }

        public Object returnOrThrow() throws CheckedAnalysisException {
            if (isNull) {
                return null;
            } else if (runtimeException != null) {
                // runtimeException.fillInStackTrace();
                throw runtimeException;
            } else if (checkedAnalysisException != null) {
                // checkedAnalysisException.fillInStackTrace();
                throw checkedAnalysisException;
            }

            throw new IllegalStateException("It has to be something");
        }
    }

    static final AbnormalAnalysisResult NULL_ANALYSIS_RESULT = new AbnormalAnalysisResult();

    @SuppressWarnings("unchecked")
    static <E> E checkedCast(Class<E> analysisClass, Object o) {
        if (SystemProperties.ASSERTIONS_ENABLED) {
            return analysisClass.cast(o);
        }
        return (E) o;
    }

    /**
     * Constructor.
     *
     * @param classPath
     *            the IClassPath to load resources from
     * @param errorLogger
     *            the IErrorLogger
     */
    AnalysisCache(IClassPath classPath, BugReporter errorLogger) {
        this.classPath = classPath;
        this.bugReporter = errorLogger;
        this.classAnalysisEngineMap = new HashMap<Class<?>, IClassAnalysisEngine<?>>();
        this.methodAnalysisEngineMap = new HashMap<Class<?>, IMethodAnalysisEngine<?>>();
        this.databaseFactoryMap = new HashMap<Class<?>, IDatabaseFactory<?>>();
        this.classAnalysisMap = new HashMap<Class<?>, Map<ClassDescriptor, Object>>();
        this.databaseMap = new HashMap<Class<?>, Object>();
    }

    @Override
    public IClassPath getClassPath() {
        return classPath;
    }

    @Override
    public void purgeAllMethodAnalysis() {
        // System.out.println("ZZZ : purging all method analyses");

        try {
            Map<ClassDescriptor, ClassContext> map = getAllClassAnalysis(ClassContext.class);
            Collection<?> allClassContexts = map.values();
            for (Object c : allClassContexts) {
                if (c instanceof ClassContext) {
                    ((ClassContext) c).purgeAllMethodAnalyses();
                }
            }
        } catch (ClassCastException e) {
            AnalysisContext.logError("Unable to purge method analysis", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <E> Map<ClassDescriptor, E> getAllClassAnalysis(Class<E> analysisClass)  {
        Map<ClassDescriptor, Object> descriptorMap
        = findOrCreateDescriptorMap(classAnalysisMap, classAnalysisEngineMap,
                analysisClass);
        return (Map<ClassDescriptor, E>) descriptorMap;
    }

    @Override
    public void purgeClassAnalysis(Class<?> analysisClass) {
        classAnalysisMap.remove(analysisClass);
    }

    /**
     * Cleans up all cached data
     */
    public void dispose(){
        classAnalysisMap.clear();
        classAnalysisEngineMap.clear();
        analysisLocals.clear();
        databaseFactoryMap.clear();
        databaseMap.clear();
        methodAnalysisEngineMap.clear();
    }

    /**
     * @param analysisClass non null analysis type
     * @return map with analysis data for given type, can be null
     */
    public @CheckForNull Map<ClassDescriptor, Object> getClassAnalysis(Class<?> analysisClass) {
        return classAnalysisMap.get(analysisClass);
    }

    /**
     * Adds the data for given analysis type from given map to the cache
     * @param analysisClass non null analysis type
     * @param map non null, pre-filled map with analysis data for given type
     */
    public <E> void reuseClassAnalysis(Class<E> analysisClass, Map<ClassDescriptor, Object> map) {
        Map<ClassDescriptor, Object> myMap = classAnalysisMap.get(analysisClass);
        if (myMap != null) {
            myMap.putAll(map);
        } else {
            myMap = createMap(classAnalysisEngineMap, analysisClass);
            myMap.putAll(map);
            classAnalysisMap.put(analysisClass, myMap);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> E getClassAnalysis(Class<E> analysisClass, @Nonnull ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        requireNonNull(classDescriptor, "classDescriptor is null");
        // Get the descriptor->result map for this analysis class,
        // creating if necessary
        Map<ClassDescriptor, Object> descriptorMap = findOrCreateDescriptorMap(classAnalysisMap,
                classAnalysisEngineMap,
                analysisClass);

        // See if there is a cached result in the descriptor map
        Object analysisResult = descriptorMap.get(classDescriptor);
        if (analysisResult == null) {
            // No cached result - compute (or recompute)

            IAnalysisEngine<ClassDescriptor, E> engine = (IAnalysisEngine<ClassDescriptor, E>) classAnalysisEngineMap
                    .get(analysisClass);
            if (engine == null) {
                throw new IllegalArgumentException("No analysis engine registered to produce " + analysisClass.getName());
            }
            Profiler profiler = getProfiler();
            // Perform the analysis
            try {
                profiler.start(engine.getClass());
                analysisResult = engine.analyze(this, classDescriptor);

                // If engine returned null, we need to construct
                // an AbnormalAnalysisResult object to record that fact.
                // Otherwise we will try to recompute the value in
                // the future.
                if (analysisResult == null) {
                    analysisResult = NULL_ANALYSIS_RESULT;
                }
            } catch (CheckedAnalysisException e) {
                // Exception - make note
                // Andrei: e.getStackTrace() cannot be null, but getter clones
                // the stack...
                // if (e.getStackTrace() == null)
                // e.fillInStackTrace();
                analysisResult = new AbnormalAnalysisResult(e);
            } catch (RuntimeException e) {
                // Exception - make note
                // Andrei: e.getStackTrace() cannot be null, but getter clones
                // the stack...
                // if (e.getStackTrace() == null)
                // e.fillInStackTrace();
                analysisResult = new AbnormalAnalysisResult(e);
            } finally {
                profiler.end(engine.getClass());
            }

            // Save the result
            descriptorMap.put(classDescriptor, analysisResult);
        }

        // Abnormal analysis result?
        if (analysisResult instanceof AbnormalAnalysisResult) {
            return checkedCast(analysisClass, ((AbnormalAnalysisResult) analysisResult).returnOrThrow());
        }

        return checkedCast(analysisClass, analysisResult);
    }

    @Override
    public <E> E probeClassAnalysis(Class<E> analysisClass, @Nonnull ClassDescriptor classDescriptor) {
        Map<ClassDescriptor, Object> descriptorMap = classAnalysisMap.get(analysisClass);
        if (descriptorMap == null) {
            return null;
        }
        return checkedCast(analysisClass, descriptorMap.get(classDescriptor));
    }

    String hex(Object o) {
        return Integer.toHexString(System.identityHashCode(o));
    }

    @Override
    public <E> E getMethodAnalysis(Class<E> analysisClass, @Nonnull MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
        requireNonNull(methodDescriptor, "methodDescriptor is null");
        ClassContext classContext = getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
        Object object = classContext.getMethodAnalysis(analysisClass, methodDescriptor);

        if (object == null) {
            try {
                object = analyzeMethod(classContext, analysisClass, methodDescriptor);
                if (object == null) {
                    object = NULL_ANALYSIS_RESULT;
                }
            } catch (RuntimeException e) {
                object = new AbnormalAnalysisResult(e);
            } catch (CheckedAnalysisException e) {
                object = new AbnormalAnalysisResult(e);
            }

            classContext.putMethodAnalysis(analysisClass, methodDescriptor, object);

        }
        if (Debug.VERIFY_INTEGRITY && object == null) {
            throw new IllegalStateException("AnalysisFactory failed to produce a result object");
        }

        if (object instanceof AbnormalAnalysisResult) {
            return checkedCast(analysisClass, ((AbnormalAnalysisResult) object).returnOrThrow());
        }

        return checkedCast(analysisClass, object);
    }

    /**
     * Analyze a method.
     *
     * @param classContext
     *            ClassContext storing method analysis objects for method's
     *            class
     * @param analysisClass
     *            class the method analysis object should belong to
     * @param methodDescriptor
     *            method descriptor identifying the method to analyze
     * @return the computed analysis object for the method
     * @throws CheckedAnalysisException
     */
    @SuppressWarnings("unchecked")
    private <E> E analyzeMethod(ClassContext classContext, Class<E> analysisClass, MethodDescriptor methodDescriptor)
            throws CheckedAnalysisException {
        IMethodAnalysisEngine<E> engine = (IMethodAnalysisEngine<E>) methodAnalysisEngineMap.get(analysisClass);
        if (engine == null) {
            throw new IllegalArgumentException("No analysis engine registered to produce " + analysisClass.getName());
        }
        Profiler profiler = getProfiler();
        profiler.start(engine.getClass());
        try {
            return engine.analyze(this, methodDescriptor);
        } finally {
            profiler.end(engine.getClass());
        }
    }

    @Override
    public <E> void eagerlyPutMethodAnalysis(Class<E> analysisClass, @Nonnull MethodDescriptor methodDescriptor, E analysisObject) {
        try {
            ClassContext classContext = getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
            assert analysisClass.isInstance(analysisObject);
            classContext.putMethodAnalysis(analysisClass, methodDescriptor, analysisObject);
        } catch (CheckedAnalysisException e) {
            IllegalStateException ise = new IllegalStateException("Unexpected exception adding method analysis to cache");
            ise.initCause(e);
            throw ise;
        }

    }

    @Override
    public void purgeMethodAnalyses(@Nonnull MethodDescriptor methodDescriptor) {
        try {

            ClassContext classContext = getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
            classContext.purgeMethodAnalyses(methodDescriptor);
        } catch (CheckedAnalysisException e) {
            IllegalStateException ise = new IllegalStateException("Unexpected exception purging method analyses from cache");
            ise.initCause(e);
            throw ise;
        }
    }

    /**
     * Find or create a descriptor to analysis object map.
     *
     * @param <DescriptorType>
     *            type of descriptor used as the map's key type (ClassDescriptor
     *            or MethodDescriptor)
     * @param analysisClassToDescriptorMapMap
     *            analysis class to descriptor map map
     * @param engineMap
     *            analysis class to analysis engine map
     * @param analysisClass
     *            the analysis map
     * @return the descriptor to analysis object map
     */
    private static <DescriptorType> Map<DescriptorType, Object> findOrCreateDescriptorMap(
            final Map<Class<?>, Map<DescriptorType, Object>> analysisClassToDescriptorMapMap,
            final Map<Class<?>, ? extends IAnalysisEngine<DescriptorType, ?>> engineMap,
                    final Class<?> analysisClass) {
        Map<DescriptorType, Object> descriptorMap = analysisClassToDescriptorMapMap.get(analysisClass);
        if (descriptorMap == null) {
            descriptorMap = createMap(engineMap, analysisClass);
            analysisClassToDescriptorMapMap.put(analysisClass, descriptorMap);
        }
        return descriptorMap;
    }

    private static <DescriptorType> Map<DescriptorType, Object> createMap(
            final Map<Class<?>, ? extends IAnalysisEngine<DescriptorType, ?>> engineMap,
                    final Class<?> analysisClass) {
        Map<DescriptorType, Object> descriptorMap;
        // Create a MapCache that allows the analysis engine to
        // decide that analysis results should be retained indefinitely.
        IAnalysisEngine<DescriptorType, ?> engine = engineMap.get(analysisClass);
        if (analysisClass.equals(JavaClass.class)) {
            descriptorMap = new MapCache<DescriptorType, Object>(MAX_JAVACLASS_RESULTS_TO_CACHE);
        } else if (analysisClass.equals(FBClassReader.class)) {
            descriptorMap = new MapCache<DescriptorType, Object>(MAX_FBCLASSREADER_RESULTS_TO_CACHE);
        } else if (analysisClass.equals(ConstantPoolGen.class)) {
            descriptorMap = new MapCache<DescriptorType, Object>(MAX_CONSTANT_POOL_GEN_RESULTS_TO_CACHE);
        } else if (analysisClass.equals(ClassContext.class)) {
            descriptorMap = new MapCache<DescriptorType, Object>(10);
        } else if (engine instanceof IClassAnalysisEngine && ((IClassAnalysisEngine<?>) engine).canRecompute()) {
            descriptorMap = new MapCache<DescriptorType, Object>(MAX_CLASS_RESULTS_TO_CACHE);
        } else {
            descriptorMap = new HashMap<DescriptorType, Object>();
        }
        return descriptorMap;
    }

    @Override
    public <E> void registerClassAnalysisEngine(Class<E> analysisResultType, IClassAnalysisEngine<E> classAnalysisEngine) {
        classAnalysisEngineMap.put(analysisResultType, classAnalysisEngine);
    }

    @Override
    public <E> void registerMethodAnalysisEngine(Class<E> analysisResultType, IMethodAnalysisEngine<E> methodAnalysisEngine) {
        methodAnalysisEngineMap.put(analysisResultType, methodAnalysisEngine);
    }

    @Override
    public <E> void registerDatabaseFactory(Class<E> databaseClass, IDatabaseFactory<E> databaseFactory) {
        databaseFactoryMap.put(databaseClass, databaseFactory);
    }

    @Override
    public <E> E getDatabase(Class<E> databaseClass) {
        return getDatabase(databaseClass, false);
    }
    @Override
    public @CheckForNull <E> E getOptionalDatabase(Class<E> databaseClass) {
        return getDatabase(databaseClass, true);
    }
    public <E> E getDatabase(Class<E> databaseClass, boolean optional) {
        Object database = databaseMap.get(databaseClass);

        if (database == null) {
            try {
                // Find the database factory
                IDatabaseFactory<?> databaseFactory = databaseFactoryMap.get(databaseClass);
                if (databaseFactory == null) {
                    if (optional) {
                        return null;
                    }
                    throw new IllegalArgumentException("No database factory registered for " + databaseClass.getName());
                }

                // Create the database
                database = databaseFactory.createDatabase();
            } catch (CheckedAnalysisException e) {
                // Error - record the analysis error
                database = new AbnormalAnalysisResult(e);
            }
            // FIXME: should catch and re-throw RuntimeExceptions?
            databaseMap.put(databaseClass, database);
        }

        if (database instanceof AbnormalAnalysisResult) {
            throw new UncheckedAnalysisException("Error instantiating " + databaseClass.getName() + " database",
                    ((AbnormalAnalysisResult) database).checkedAnalysisException);
        }
        return databaseClass.cast(database);
    }

    @Override
    public <E> void eagerlyPutDatabase(Class<E> databaseClass, E database) {
        databaseMap.put(databaseClass, database);
    }

    @Override
    public IErrorLogger getErrorLogger() {
        return bugReporter;
    }

    @Override
    public Profiler getProfiler() {
        return bugReporter.getProjectStats().getProfiler();
    }
}
