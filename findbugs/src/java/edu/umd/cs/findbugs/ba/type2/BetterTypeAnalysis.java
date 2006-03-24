/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type2;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.FrameDataflowAnalysis;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

public class BetterTypeAnalysis extends FrameDataflowAnalysis<Type, BetterTypeFrame> {
	private static final String JAVA_LANG_THROWABLE_SIGNATURE = "Ljava/lang/Throwable;";

	private MethodGen methodGen;
	private String[] parameterSignatureList;
//	private CFG cfg;
	private TypeRepository typeRepository;
	private TypeMerger typeMerger;
	private RepositoryLookupFailureCallback lookupFailureCallback;

	//@SuppressWarnings("EI2")
	public BetterTypeAnalysis(MethodGen methodGen, String[] parameterSignatureList,
	                          CFG cfg, DepthFirstSearch dfs,
	                          TypeRepository typeRepository, TypeMerger typeMerger,
	                          RepositoryLookupFailureCallback lookupFailureCallback) {
		super(dfs);
		this.methodGen = methodGen;
		this.parameterSignatureList = parameterSignatureList;
//		this.cfg = cfg;
		this.typeRepository = typeRepository;
		this.typeMerger = typeMerger;
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public BetterTypeFrame createFact() {
		return new BetterTypeFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(BetterTypeFrame result) throws DataflowAnalysisException {
		try {
			result.setValid();

			int local = 0;

			// Instance methods have the "this" reference in local slot zero
			if (!methodGen.isStatic()) {
				result.setValue(local++, typeRepository.classTypeFromDottedClassName(methodGen.getClassName()));
			}

			// Fill in parameter types
			for (String signature : parameterSignatureList) {
				Type type = typeRepository.typeFromSignature(signature);

				// Long and double values occupy an extra local slot
				if (type.getTypeCode() == Constants.T_LONG) {
					result.setValue(local++, typeRepository.getLongExtraType());
				} else if (type.getTypeCode() == Constants.T_DOUBLE) {
					result.setValue(local++, typeRepository.getDoubleExtraType());
				}

				// The parameter type
				result.setValue(local++, type);
			}

			// Fill remaining locals with the bottom type.
			for (int i = local; i < methodGen.getMaxLocals(); ++i) {
				result.setValue(i, typeRepository.getBottomType());
			}
		} catch (InvalidSignatureException e) {
			throw new DataflowAnalysisException("Invalid parameter type signature", e);
		}
	}

	@Override
         public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BetterTypeFrame fact)
	        throws DataflowAnalysisException {
		// TODO: implement
	}

	public void meetInto(BetterTypeFrame fact, Edge edge, BetterTypeFrame result)
	        throws DataflowAnalysisException {

		// TODO: implement ACCURATE_EXCEPTIONS

		if (fact.isValid() && edge.getTarget().isExceptionHandler()) {
			BetterTypeFrame tmpFact = null;

			// Exception handler.
			// Clear stack and push exception handler catch type.

			tmpFact = modifyFrame(fact, tmpFact);
			tmpFact.clearStack();

			CodeExceptionGen exceptionGen = edge.getTarget().getExceptionGen();
			org.apache.bcel.generic.ObjectType catchType = exceptionGen.getCatchType();
			if (catchType == null) {
				tmpFact.pushValue(typeRepository.classTypeFromSignature(JAVA_LANG_THROWABLE_SIGNATURE));
			} else {
				tmpFact.pushValue(typeRepository.classTypeFromDottedClassName(catchType.getClassName()));
			}

			fact = tmpFact;
		}

		mergeInto(fact, result);

	}
	
	//@Override
	@Override
         protected void mergeValues(BetterTypeFrame otherFrame, BetterTypeFrame resultFrame, int slot) throws DataflowAnalysisException {
		try {
			Type value = typeMerger.mergeTypes(resultFrame.getValue(slot), otherFrame.getValue(slot));
			resultFrame.setValue(slot, value);
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
			throw new DataflowAnalysisException("Missing class for type analysis", e);
		}
	}
}

// vim:ts=4
