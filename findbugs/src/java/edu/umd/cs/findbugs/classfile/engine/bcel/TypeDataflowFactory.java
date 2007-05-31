/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.type.ExceptionSetFactory;
import edu.umd.cs.findbugs.ba.type.TypeAnalysis;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce TypeDataflow objects
 * for analyzed methods. 
 * 
 * @author David Hovemeyer
 */
public class TypeDataflowFactory extends AnalysisFactory<TypeDataflow> {
	/**
	 * Constructor.
	 */
	public TypeDataflowFactory() {
		super("type analysis", TypeDataflow.class);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
		MethodGen methodGen = getMethodGen(analysisCache, descriptor);
		if (methodGen == null) {
			throw new MethodUnprofitableException(descriptor);
		}
		CFG cfg = getCFG(analysisCache, descriptor);
		DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);
		ExceptionSetFactory exceptionSetFactory = getExceptionSetFactory(analysisCache, descriptor);
		Method method = getMethod(analysisCache, descriptor);

		TypeAnalysis typeAnalysis = new TypeAnalysis(
				method,
				methodGen,
				cfg,
				dfs,
				AnalysisContext.currentAnalysisContext().getLookupFailureCallback(), exceptionSetFactory);

		if (AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.MODEL_INSTANCEOF)) {
			typeAnalysis.setValueNumberDataflow(getValueNumberDataflow(analysisCache, descriptor));
		}

		// Field store type database.
		// If present, this can give us more accurate type information
		// for values loaded from fields.
		typeAnalysis.setFieldStoreTypeDatabase(AnalysisContext.currentAnalysisContext().getFieldStoreTypeDatabase());

		TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
		typeDataflow.execute();
		if (TypeAnalysis.DEBUG) {
			ClassContext.dumpTypeDataflow(method, cfg, typeDataflow);
		}

		return typeDataflow;
	}
}
