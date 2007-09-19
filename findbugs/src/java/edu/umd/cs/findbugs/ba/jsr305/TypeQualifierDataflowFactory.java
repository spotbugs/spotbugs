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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.HashMap;

import org.apache.bcel.generic.ConstantPoolGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Factory to create and cache TypeQualifierDataflow objects
 * for a particular method.
 * 
 * @author David Hovemeyer
 */
public abstract class TypeQualifierDataflowFactory
	<
		AnalysisType extends TypeQualifierDataflowAnalysis,
		DataflowType extends TypeQualifierDataflow<AnalysisType>
	> {
	
	private static class DataflowResult<DataflowType> {
		DataflowType dataflow;
		CheckedAnalysisException checkedException;
		RuntimeException runtimeException;

		DataflowType get() throws CheckedAnalysisException {
			if (dataflow != null) {
				return dataflow;
			}
			if (checkedException != null) {
				throw checkedException;
			}
			throw runtimeException;
		}
	}

	private HashMap<TypeQualifierValue, DataflowResult<DataflowType>> dataflowMap;
	private MethodDescriptor methodDescriptor;

	public TypeQualifierDataflowFactory(MethodDescriptor methodDescriptor) {
		this.methodDescriptor = methodDescriptor;
		this.dataflowMap = new HashMap<TypeQualifierValue, DataflowResult<DataflowType>>();
	}

	public DataflowType getDataflow(TypeQualifierValue typeQualifierValue) throws CheckedAnalysisException {
		DataflowResult<DataflowType> result = dataflowMap.get(typeQualifierValue);
		if (result == null) {
			result = compute(typeQualifierValue);
			dataflowMap.put(typeQualifierValue, result);
		}
		return result.get();
	}

	private DataflowResult<DataflowType> compute(TypeQualifierValue typeQualifierValue) {
		DataflowResult<DataflowType> result = new DataflowResult<DataflowType>();
		
		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			
			DepthFirstSearch dfs = analysisCache.getMethodAnalysis(DepthFirstSearch.class, methodDescriptor);
			XMethod xmethod = AnalysisContext.currentXFactory().createXMethod(methodDescriptor);
			CFG cfg = analysisCache.getMethodAnalysis(CFG.class, methodDescriptor);
			ValueNumberDataflow vnaDataflow = analysisCache.getMethodAnalysis(ValueNumberDataflow.class, methodDescriptor);
			ConstantPoolGen cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());

			DataflowType dataflow = getDataflow(dfs, xmethod, cfg, vnaDataflow, cpg, analysisCache, methodDescriptor, typeQualifierValue);
			
			result.dataflow = dataflow;
		} catch (CheckedAnalysisException e) {
			result.checkedException = e;
		} catch (RuntimeException e) {
			result.runtimeException = e;
		}
		
		return result;
	}

    protected abstract DataflowType getDataflow(
    		DepthFirstSearch dfs,
    		XMethod xmethod,
    		CFG cfg,
    		ValueNumberDataflow vnaDataflow,
            ConstantPoolGen cpg,
            IAnalysisCache analysisCache,
            MethodDescriptor methodDescriptor,
            TypeQualifierValue typeQualifierValue) throws CheckedAnalysisException;
	
}
