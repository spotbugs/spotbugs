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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.NullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * @author pugh
 */
public class DerefFinder {

	public static boolean DEBUG = SystemProperties.getBoolean("deref.finder.debug");
	public static UsagesRequiringNonNullValues getAnalysis(ClassContext classContext, Method method) {
		XMethod thisMethod = XFactory.createXMethod(classContext.getJavaClass(), method);
		if (DEBUG) System.out.println(thisMethod);
		UsagesRequiringNonNullValues derefs = new UsagesRequiringNonNullValues();
		try {

			CFG cfg = classContext.getCFG(method);

			ValueNumberDataflow vna = classContext.getValueNumberDataflow(method);
			TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
			NullnessAnnotationDatabase db = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase();

			ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase = AnalysisContext
					.currentAnalysisContext().getUnconditionalDerefParamDatabase();
			Iterator<BasicBlock> bbIter = cfg.blockIterator();
			ConstantPoolGen cpg = classContext.getConstantPoolGen();
			ValueNumber valueNumberForThis = null;
			if (!method.isStatic()) {
				ValueNumberFrame frameAtEntry = vna.getStartFact(cfg.getEntry());
				valueNumberForThis = frameAtEntry.getValue(0);
			}

			NullnessAnnotation methodAnnotation = getMethodNullnessAnnotation(classContext, method);

			while (bbIter.hasNext()) {
				BasicBlock basicBlock = bbIter.next();

				if (basicBlock.isNullCheck()) {
					InstructionHandle exceptionThrowerHandle = basicBlock.getExceptionThrower();
					Instruction exceptionThrower = exceptionThrowerHandle.getInstruction();
					ValueNumberFrame vnaFrame = vna.getStartFact(basicBlock);
					if (!vnaFrame.isValid())
						continue;
					ValueNumber valueNumber = vnaFrame.getInstance(exceptionThrower, cpg);

					Location location = new Location(exceptionThrowerHandle, basicBlock);
					if (valueNumberForThis != valueNumber)
						derefs.add(location, valueNumber, PointerUsageRequiringNonNullValue.getPointerDereference());

				}
			}

			for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
				Location location = i.next();
				InstructionHandle handle = location.getHandle();
				Instruction ins = handle.getInstruction();
				ValueNumberFrame valueNumberFrame = vna.getFactAtLocation(location);
				TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
				if (ins instanceof InvokeInstruction) {
					InvokeInstruction inv = (InvokeInstruction) ins;
					XMethod m = XFactory.createXMethod(inv, cpg);
					SignatureParser sigParser = new SignatureParser(m.getSignature());
					int numParams = sigParser.getNumParameters();

					// Check nonnull annotations

					for (int j = 0; j < numParams; j++)
						if (db.parameterMustBeNonNull(m, j)) {
							int slot = sigParser.getSlotsFromTopOfStackForParameter(j);
							ValueNumber valueNumber = valueNumberFrame.getStackValue(slot);
							if (valueNumberForThis != valueNumber) derefs.add(location, valueNumber, PointerUsageRequiringNonNullValue
									.getPassedAsNonNullParameter(m, j));
						}

					// Check actual targets
					try {
						Set<JavaClassAndMethod> targetMethodSet = Hierarchy.resolveMethodCallTargets(inv, typeFrame,
								cpg);
						BitSet unconditionallyDereferencedNullArgSet = null;
						 for (JavaClassAndMethod targetMethod : targetMethodSet) {

							ParameterNullnessProperty property = unconditionalDerefParamDatabase
									.getProperty(targetMethod.toMethodDescriptor());
							if (property == null) {
								unconditionallyDereferencedNullArgSet = null;
								break;
							}
							BitSet foo = property.getAsBitSet();
							if (unconditionallyDereferencedNullArgSet == null)
								unconditionallyDereferencedNullArgSet = foo;
							else
								unconditionallyDereferencedNullArgSet.intersects(foo);
							if (unconditionallyDereferencedNullArgSet.isEmpty())
								break;
						}

						if (unconditionallyDereferencedNullArgSet != null
								&& !unconditionallyDereferencedNullArgSet.isEmpty() && valueNumberFrame.isValid())
							for (int j = unconditionallyDereferencedNullArgSet.nextSetBit(0); j >= 0; j = unconditionallyDereferencedNullArgSet
									.nextSetBit(j + 1)) {
								int slot = sigParser.getSlotsFromTopOfStackForParameter(j);
								ValueNumber valueNumber = valueNumberFrame.getStackValue(slot);
								if (valueNumberForThis != valueNumber)  derefs.add(location, valueNumber, PointerUsageRequiringNonNullValue
										.getPassedAsNonNullParameter(m, j));
							}

					} catch (ClassNotFoundException e) {
						AnalysisContext.reportMissingClass(e);
					}

				} else if (ins instanceof ARETURN && methodAnnotation == NullnessAnnotation.NONNULL) {
					ValueNumber valueNumber = valueNumberFrame.getTopValue();
					if (valueNumberForThis != valueNumber) derefs.add(location, valueNumber, PointerUsageRequiringNonNullValue
							.getReturnFromNonNullMethod(thisMethod));

				} else if (ins instanceof PUTFIELD || ins instanceof PUTSTATIC) {
					FieldInstruction inf = (FieldInstruction) ins;
					XField field = XFactory.createXField(inf, cpg);
					NullnessAnnotation annotation = AnalysisContext.currentAnalysisContext()
							.getNullnessAnnotationDatabase().getResolvedAnnotation(field, false);
					if (annotation == NullnessAnnotation.NONNULL) {
						ValueNumber valueNumber = valueNumberFrame.getTopValue();
						if (valueNumberForThis != valueNumber)  derefs.add(location, valueNumber, PointerUsageRequiringNonNullValue
								.getStoredIntoNonNullField(field));
					}

				}
			}

		} catch (CFGBuilderException e) {
			AnalysisContext.logError("Error generating derefs for " + thisMethod, e);
		} catch (DataflowAnalysisException e) {
			AnalysisContext.logError("Error generating derefs for " + thisMethod, e);
		}
		return derefs;
	}

	public static NullnessAnnotation getMethodNullnessAnnotation(ClassContext classContext, Method method) {

		if (method.getSignature().indexOf(")L") >= 0 || method.getSignature().indexOf(")[") >= 0) {

			XMethod m = XFactory.createXMethod(classContext.getJavaClass(), method);
			return AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase().getResolvedAnnotation(m,
					false);
		}
		return NullnessAnnotation.UNKNOWN_NULLNESS;
	}

}
