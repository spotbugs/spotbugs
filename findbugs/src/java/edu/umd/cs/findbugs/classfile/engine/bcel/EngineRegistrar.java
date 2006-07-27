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

import edu.umd.cs.findbugs.ba.AnnotationRetentionDatabase;
import edu.umd.cs.findbugs.ba.CheckReturnAnnotationDatabase;
import edu.umd.cs.findbugs.ba.InnerClassAccessMap;
import edu.umd.cs.findbugs.ba.JCIPAnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IDatabaseFactory;
import edu.umd.cs.findbugs.classfile.ReflectionDatabaseFactory;

/**
 * Register BCEL-framework analysis engines.
 * 
 * <p>
 * <b>NOTE</b>: the database factories will only work with
 * AnalysisCacheToAnalysisContextAdapter,
 * not with LegacyAnalysisContext.
 * However, that's ok since the databases for BCEL-based
 * analyses are only ever accessed through the
 * AnalysisContext.
 * </p>
 * 
 * @author David Hovemeyer
 */
public class EngineRegistrar implements IAnalysisEngineRegistrar {
	private static final IClassAnalysisEngine[] classAnalysisEngineList = {
		new ClassContextClassAnalysisEngine(),
		new JavaClassAnalysisEngine(),
	};
	
	private static final IDatabaseFactory<?>[] databaseFactoryList = {
		new ReflectionDatabaseFactory<Subtypes>(Subtypes.class),
		new ReflectionDatabaseFactory<InnerClassAccessMap>(InnerClassAccessMap.class),
		new ReflectionDatabaseFactory<CheckReturnAnnotationDatabase>(CheckReturnAnnotationDatabase.class),
		new ReflectionDatabaseFactory<AnnotationRetentionDatabase>(AnnotationRetentionDatabase.class),
		new ReflectionDatabaseFactory<JCIPAnnotationDatabase>(JCIPAnnotationDatabase.class),
		new ReflectionDatabaseFactory<NullnessAnnotationDatabase>(NullnessAnnotationDatabase.class),
		new ReflectionDatabaseFactory<SourceFinder>(SourceFinder.class),
		new ReflectionDatabaseFactory<SourceInfoMap>(SourceInfoMap.class),
		new ReflectionDatabaseFactory<FieldStoreTypeDatabase>(FieldStoreTypeDatabase.class),
		new ReflectionDatabaseFactory<ParameterNullnessPropertyDatabase>(ParameterNullnessPropertyDatabase.class),
	};

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar#registerAnalysisEngines(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerAnalysisEngines(IAnalysisCache analysisCache) {
		for (IClassAnalysisEngine engine : classAnalysisEngineList) {
			engine.registerWith(analysisCache);
		}
		
		for (IDatabaseFactory<?> databaseFactory : databaseFactoryList) {
			databaseFactory.registerWith(analysisCache);
		}
	}

}
