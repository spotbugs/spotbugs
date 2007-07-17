/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessProperty;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

/**
 * Build database of unconditionally dereferenced parameters.
 * 
 * @author David Hovemeyer
 */
public class BuildUnconditionalParamDerefDatabase {
	public static final boolean VERBOSE_DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg.verbose");
	private static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg") || VERBOSE_DEBUG;

	public void visitClassContext(ClassContext classContext) {
		boolean fullAnalysis = AnalysisContext.currentAnalysisContext().getBoolProperty(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES);
		if (!fullAnalysis && !AnalysisContext.currentAnalysisContext().getSubtypes().isApplicationClass(classContext.getJavaClass()))
				return;
		if (VERBOSE_DEBUG) System.out.println("Visiting class " + classContext.getJavaClass().getClassName());

		for(Method m : classContext.getMethodsInCallOrder()) 
			considerMethod(classContext, m);
	}


	private void considerMethod(ClassContext classContext, Method method) {
		boolean hasReferenceParameters = false;
		for (Type argument : method.getArgumentTypes())
			if (argument instanceof ReferenceType) {
				hasReferenceParameters = true;
				referenceParameters++;
			}

		if (hasReferenceParameters && classContext.getMethodGen(method) != null) {
			if (VERBOSE_DEBUG) System.out.println("Check " + method);
			analyzeMethod(classContext, method);
		}
	}

	protected int referenceParameters;
	protected int nonnullReferenceParameters;

	private void analyzeMethod(ClassContext classContext, Method method) {
		try {
			CFG cfg = classContext.getCFG(method);

			ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
			UnconditionalValueDerefDataflow dataflow =
				classContext.getUnconditionalValueDerefDataflow(method);

			SignatureParser parser =  new SignatureParser(method.getSignature());
			int paramLocalOffset = method.isStatic() ? 0 : 1;

			// Build BitSet of params that are unconditionally dereferenced
			BitSet unconditionalDerefSet = new BitSet();
			UnconditionalValueDerefSet entryFact = dataflow.getResultFact(cfg.getEntry());
			Iterator<String> paramIterator = parser.parameterSignatureIterator();
			int i = 0;
			while (paramIterator.hasNext()) {
				String paramSig = paramIterator.next();

				ValueNumber paramVN = vnaDataflow.getAnalysis().getEntryValue(paramLocalOffset);

				if (entryFact.isUnconditionallyDereferenced(paramVN)) {
					unconditionalDerefSet.set(i);
				}
				i++;
				if (paramSig.equals("D") || paramSig.equals("J")) paramLocalOffset += 2;
				else paramLocalOffset += 1;
			}

			// No need to add properties if there are no unconditionally dereferenced params
			if (unconditionalDerefSet.isEmpty()) {
				if (VERBOSE_DEBUG) {
					System.out.println("\tResult is empty");
				}
				return;
			}

			if (VERBOSE_DEBUG) {
				ClassContext.dumpDataflowInformation(method, cfg, vnaDataflow, classContext.getIsNullValueDataflow(method), dataflow,  classContext.getTypeDataflow(method));
			}
			ParameterNullnessProperty property = new ParameterNullnessProperty();
			nonnullReferenceParameters += unconditionalDerefSet.cardinality();
			property.setNonNullParamSet(unconditionalDerefSet);

			XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
			AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase().setProperty(xmethod.getMethodDescriptor(), property);
			if (DEBUG) {
				System.out.println("Unconditional deref: " + xmethod + "=" + property);
			}
		} catch (CheckedAnalysisException e) {
			XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
			   AnalysisContext.currentAnalysisContext().getLookupFailureCallback().logError(
					"Error analyzing " + xmethod + " for unconditional deref training", e);
		}
	}

}
