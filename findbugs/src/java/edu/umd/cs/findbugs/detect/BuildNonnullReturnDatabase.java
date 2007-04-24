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
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
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
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessProperty;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;

/**
 * Build database of methods that return values guaranteed to be nonnull
 * 
 */
public class BuildNonnullReturnDatabase {
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
		if ((method.getReturnType() instanceof ReferenceType)  && classContext.getMethodGen(method) != null) {
			if (VERBOSE_DEBUG) System.out.println("Check " + method);
			analyzeMethod(classContext, method);
		}
	}
	protected int returnsReference;
	protected int returnsNonNull;

	private void analyzeMethod(ClassContext classContext, Method method) {
		returnsReference++;
		try {
			CFG cfg = classContext.getCFG(method);

			IsNullValueDataflow inv = classContext.getIsNullValueDataflow(method);
			boolean guaranteedNonNull = true;
			for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
				Location location = i.next();
				InstructionHandle handle = location.getHandle();
				Instruction ins = handle.getInstruction();

				if (!(ins instanceof ARETURN)) continue;
				IsNullValueFrame frame = inv.getFactAtLocation(location);
				if (!frame.isValid()) continue;
				IsNullValue value = frame.getTopValue();
				if (!value.isDefinitelyNotNull()) {
					guaranteedNonNull = false;
					break;
				}

			}

			XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
			if (guaranteedNonNull) {
				returnsNonNull++;
				AnalysisContext.currentAnalysisContext().getReturnValueNullnessPropertyDatabase().setProperty(xmethod, guaranteedNonNull);
				if (DEBUG) 
					System.out.println("Unconditional deref: " + xmethod + "=" + guaranteedNonNull);

				}

		} catch (CFGBuilderException e) {
			XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);

			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().logError(
					"Error analyzing " + xmethod + " for unconditional deref training", e);
		} catch (DataflowAnalysisException e) {
			XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
			   AnalysisContext.currentAnalysisContext().getLookupFailureCallback().logError(
					"Error analyzing " + xmethod + " for unconditional deref training", e);
		}
	}

}
