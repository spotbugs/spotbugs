/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine.bcel;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StackMapAnalyzer.StackMapAnalysisFactory;
import edu.umd.cs.findbugs.ba.AnnotationRetentionDatabase;
import edu.umd.cs.findbugs.ba.CheckReturnAnnotationDatabase;
import edu.umd.cs.findbugs.ba.InnerClassAccessMap;
import edu.umd.cs.findbugs.ba.JCIPAnnotationDatabase;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.jsr305.DirectlyRelevantTypeQualifiersDatabase;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDatabase;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.ReturnValueNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IDatabaseFactory;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.ReflectionDatabaseFactory;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo.MethodInfoDatabase;

/**
 * Register BCEL-framework analysis engines.
 *
 * <p>
 * <b>NOTE</b>: the database factories will only work with
 * AnalysisCacheToAnalysisContextAdapter, not with LegacyAnalysisContext.
 * However, that's ok since the databases for BCEL-based analyses are only ever
 * accessed through the AnalysisContext.
 * </p>
 *
 * @author David Hovemeyer
 */
public class EngineRegistrar implements IAnalysisEngineRegistrar {
    private static final IClassAnalysisEngine<?>[] classAnalysisEngineList = { new ClassContextClassAnalysisEngine(),
        new JavaClassAnalysisEngine(), new ConstantPoolGenFactory(),
        // new AssignedFieldMapFactory(),
        new AssertionMethodsFactory(), };

    private static final IMethodAnalysisEngine<?>[] methodAnalysisEngineList = { new MethodFactory(), new MethodGenFactory(),
        new CFGFactory(), new UsagesRequiringNonNullValuesFactory(), new ValueNumberDataflowFactory(),
        new IsNullValueDataflowFactory(), new TypeDataflowFactory(), new DepthFirstSearchFactory(),
        new ReverseDepthFirstSearchFactory(), new UnpackedCodeFactory(), new LockDataflowFactory(), new LockCheckerFactory(),
        new ReturnPathDataflowFactory(), new DominatorsAnalysisFactory(), new NonExceptionPostdominatorsAnalysisFactory(),
        new NonImplicitExceptionPostDominatorsAnalysisFactory(), new ExceptionSetFactoryFactory(),
        new ParameterSignatureListFactory(), new ConstantDataflowFactory(), new LoadDataflowFactory(),
        new StoreDataflowFactory(), new LoadedFieldSetFactory(), new LiveLocalStoreDataflowFactory(),
        new BlockTypeAnalysisFactory(), new CallListDataflowFactory(), new UnconditionalValueDerefDataflowFactory(),
        new CompactLocationNumberingFactory(),  new ReturnPathTypeDataflowFactory(),
        new ForwardTypeQualifierDataflowFactoryFactory(), new BackwardTypeQualifierDataflowFactoryFactory(),
        new OpcodeStack.JumpInfoFactory(), new StackMapAnalysisFactory(), new ObligationDataflowFactory(),
        new ValueRangeAnalysisFactory(), new FinallyDuplicatesInfoFactory()};

    private static final IDatabaseFactory<?>[] databaseFactoryList = {
        // new ReflectionDatabaseFactory<Subtypes>(Subtypes.class),
        new ReflectionDatabaseFactory<Subtypes2>(Subtypes2.class),
        new ReflectionDatabaseFactory<InnerClassAccessMap>(InnerClassAccessMap.class),
        new ReflectionDatabaseFactory<CheckReturnAnnotationDatabase>(CheckReturnAnnotationDatabase.class),
        new ReflectionDatabaseFactory<AnnotationRetentionDatabase>(AnnotationRetentionDatabase.class),
        new ReflectionDatabaseFactory<JCIPAnnotationDatabase>(JCIPAnnotationDatabase.class),
        new ReflectionDatabaseFactory<SourceInfoMap>(SourceInfoMap.class),
        new ReflectionDatabaseFactory<FieldStoreTypeDatabase>(FieldStoreTypeDatabase.class),
        new ReflectionDatabaseFactory<ParameterNullnessPropertyDatabase>(ParameterNullnessPropertyDatabase.class),
        new ReflectionDatabaseFactory<ReturnValueNullnessPropertyDatabase>(ReturnValueNullnessPropertyDatabase.class),
        new ReflectionDatabaseFactory<DirectlyRelevantTypeQualifiersDatabase>(DirectlyRelevantTypeQualifiersDatabase.class),
        new ReflectionDatabaseFactory<TypeQualifierDatabase>(TypeQualifierDatabase.class),
        new ReflectionDatabaseFactory<MethodInfoDatabase>(MethodInfoDatabase.class),
    };

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar#
     * registerAnalysisEngines(edu.umd.cs.findbugs.classfile.IAnalysisCache)
     */
    @Override
    public void registerAnalysisEngines(IAnalysisCache analysisCache) {
        for (IClassAnalysisEngine<?> engine : classAnalysisEngineList) {
            engine.registerWith(analysisCache);
        }

        for (IMethodAnalysisEngine<?> engine : methodAnalysisEngineList) {
            engine.registerWith(analysisCache);
        }

        for (IDatabaseFactory<?> databaseFactory : databaseFactoryList) {
            databaseFactory.registerWith(analysisCache);
        }

        // if (!Subtypes.DO_NOT_USE) {
        // new
        // ReflectionDatabaseFactory<Subtypes>(Subtypes.class).registerWith(analysisCache);
        // }
    }

}
