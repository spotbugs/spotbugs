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

import edu.umd.cs.findbugs.ba.CFG;
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
public class TypeQualifierDataflowFactory {
	private static class DataflowResult {
		TypeQualifierDataflow dataflow;
		CheckedAnalysisException checkedException;
		RuntimeException runtimeException;

		TypeQualifierDataflow get() throws CheckedAnalysisException {
			if (dataflow != null) {
				return dataflow;
			}
			if (checkedException != null) {
				throw checkedException;
			}
			throw runtimeException;
		}
	}

	private HashMap<TypeQualifierValue, DataflowResult> dataflowMap;
	private MethodDescriptor methodDescriptor;

	public TypeQualifierDataflowFactory(MethodDescriptor methodDescriptor) {
		this.methodDescriptor = methodDescriptor;
		this.dataflowMap = new HashMap<TypeQualifierValue, DataflowResult>();
	}

	public TypeQualifierDataflow getDataflow(TypeQualifierValue typeQualifierValue) throws CheckedAnalysisException {
		DataflowResult result = dataflowMap.get(typeQualifierValue);
		if (result == null) {
			result = compute(typeQualifierValue);
			dataflowMap.put(typeQualifierValue, result);
		}
		return result.get();
	}

	private DataflowResult compute(TypeQualifierValue typeQualifierValue) {
		DataflowResult result = new DataflowResult();
		
		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();

			DepthFirstSearch dfs = analysisCache.getMethodAnalysis(DepthFirstSearch.class, methodDescriptor);
			XMethod xmethod = analysisCache.getMethodAnalysis(XMethod.class, methodDescriptor);
			CFG cfg = analysisCache.getMethodAnalysis(CFG.class, methodDescriptor);
			ValueNumberDataflow vnaDataflow = analysisCache.getMethodAnalysis(ValueNumberDataflow.class, methodDescriptor);
			ConstantPoolGen cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());

			TypeQualifierDataflowAnalysis analysis = new TypeQualifierDataflowAnalysis(
					dfs, xmethod, cfg, vnaDataflow, cpg, typeQualifierValue
			);
			TypeQualifierDataflow dataflow = new TypeQualifierDataflow(cfg, analysis);

			dataflow.execute();
			
			result.dataflow = dataflow;
		} catch (CheckedAnalysisException e) {
			result.checkedException = e;
		} catch (RuntimeException e) {
			result.runtimeException = e;
		}
		
		return result;
	}
}
