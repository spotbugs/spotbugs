/*
 * Contributions to SpotBugs
 * Copyright (C) 2018, Brian Riehman
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

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IDatabaseFactory;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.log.Profiler;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class NoopAnalysisCache implements IAnalysisCache {
    @Override
    public <E> void registerClassAnalysisEngine(Class<E> analysisResultType,
            IClassAnalysisEngine<E> classAnalysisEngine) {

    }

    @Override
    public <E> void registerMethodAnalysisEngine(Class<E> analysisResultType,
            IMethodAnalysisEngine<E> methodAnalysisEngine) {

    }

    @Override
    public <E> E getClassAnalysis(Class<E> analysisClass,
            @Nonnull ClassDescriptor classDescriptor)
            throws CheckedAnalysisException {
        return null;
    }

    @Override
    public <E> E probeClassAnalysis(Class<E> analysisClass,
            @Nonnull ClassDescriptor classDescriptor) {
        return null;
    }

    @Override
    public <E> E getMethodAnalysis(Class<E> analysisClass,
            @Nonnull MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
        return null;
    }

    @Override
    public <E> void eagerlyPutMethodAnalysis(Class<E> analysisClass,
            @Nonnull MethodDescriptor methodDescriptor, E analysisObject) {

    }

    @Override
    public void purgeMethodAnalyses(@Nonnull MethodDescriptor methodDescriptor) {

    }

    @Override
    public void purgeAllMethodAnalysis() {

    }

    @Override
    public void purgeClassAnalysis(Class<?> analysisClass) {

    }

    @Override
    public <E> void registerDatabaseFactory(Class<E> databaseClass,
            IDatabaseFactory<E> databaseFactory) {

    }

    @Override
    public <E> E getDatabase(Class<E> databaseClass) {
        return null;
    }

    @CheckForNull
    @Override
    public <E> E getOptionalDatabase(Class<E> databaseClass) {
        return null;
    }

    @Override
    public <E> void eagerlyPutDatabase(Class<E> databaseClass, E database) {

    }

    @Override
    public IClassPath getClassPath() {
        return null;
    }

    @Override
    public IErrorLogger getErrorLogger() {
        return null;
    }

    @Override
    public Map<?, ?> getAnalysisLocals() {
        return null;
    }

    @Override
    public Profiler getProfiler() {
        return new Profiler();
    }
}
