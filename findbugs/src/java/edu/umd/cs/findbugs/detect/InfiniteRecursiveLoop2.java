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

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.heap.FieldSet;
import edu.umd.cs.findbugs.ba.heap.LoadDataflow;
import edu.umd.cs.findbugs.ba.heap.StoreDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Signal an infinite loop if either:
 * we see a call to the same method with the same parameters, or
 * we see a call to the same (dynamically dispatched method), and there
 * has been no transfer of control.
 * 
 * <p>This does the same thing as InfiniteRecursiveLoop, but uses CFG-based
 * analysis for greater precision.</p>
 * 
 * @author Bill Pugh
 * @author David Hovemeyer	
 */
public @Deprecated class InfiniteRecursiveLoop2 implements Detector {
	private static final boolean DEBUG = SystemProperties.getBoolean("irl.debug");
	private static final String IRL_METHOD = SystemProperties.getProperty("irl.method");
	
	private BugReporter bugReporter;
	
	public InfiniteRecursiveLoop2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();
		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			if (IRL_METHOD != null && !method.getName().equals(IRL_METHOD))
				continue;

			try {
				if (DEBUG) {
					System.out.println("Checking method " +
							SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method));
				}
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error checking for infinite recursive loop in " +
						SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method), e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Error checking for infinite recursive loop in " +
						SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method), e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);

		// Look for recursive calls which either
		//   - postdominate the CFG entry, or
		//   - pass all of the parameters as arguments
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext(); ) {
			BasicBlock basicBlock = i.next();

			// Check if it's a method invocation.
			if (!basicBlock.isExceptionThrower())
				continue;
			InstructionHandle thrower = basicBlock.getExceptionThrower();
			Instruction ins = thrower.getInstruction();
			if (!(ins instanceof InvokeInstruction))
				continue;

			// Recursive call?
			if (isRecursiveCall((InvokeInstruction) ins, classContext, method)) {
				checkRecursiveCall(classContext, method, cfg, basicBlock, thrower, (InvokeInstruction) ins);
			}
			
			// Call to add(Object)?
			if (isCallToAdd((InvokeInstruction) ins, classContext.getConstantPoolGen())) {
				if (DEBUG) { System.out.println("Checking call to add..."); }
				checkCallToAdd(classContext, method, basicBlock, thrower);
			}
		}
	}

	private boolean isRecursiveCall(InvokeInstruction instruction, ClassContext classContext, Method method) {
		if ((instruction.getOpcode() == Constants.INVOKESTATIC) != method.isStatic())
			return false;
		
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		if (!instruction.getClassName(cpg).equals(classContext.getJavaClass().getClassName())
				|| !instruction.getName(cpg).equals(method.getName())
				|| !instruction.getSignature(cpg).equals(method.getSignature()))
			return false;
		
		return true;
	}

	private void checkRecursiveCall(
			ClassContext classContext,
			Method method,
			CFG cfg,
			BasicBlock basicBlock,
			InstructionHandle thrower,
			InvokeInstruction ins) throws DataflowAnalysisException, CFGBuilderException {

		if (DEBUG) {
			System.out.println("Checking recursive call in " +
					SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method));
		}
		
		PostDominatorsAnalysis postDominators =
			classContext.getNonImplicitExceptionDominatorsAnalysis(method);
		
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg.getEntry());
		
		// Get blocks which postdominate the method entry
		BitSet entryPostDominators = postDominators.getAllDominatorsOf(cfg.getEntry());

		// How many arguments need to be checked to find out whether
		// the parameters are passed to recursive calls verbatim?
		int numArgsToCheck = new SignatureParser(method.getSignature()).getNumParameters();
		if (!method.isStatic())
			++numArgsToCheck;

		boolean report = false;

		// Check to see if this block postdominates the method entry,
		// and the called method is known exactly.
		report = entryPostDominators.get(basicBlock.getId())
				&& targetMethodKnownExactly(classContext, method, basicBlock, ins);
		
		if (!report) {
			// See if 
			// (1) all parameters are passed unconditionally as arguments
			// (2) no fields which might have been read have been written to
			//     (meaning a different path could be taken in the called method)
			report = allParamsPassedAsArgs(classContext, vnaDataflow, vnaFrameAtEntry, numArgsToCheck, basicBlock, ins)
					&& !checkedStateHasBeenModified(classContext, method, basicBlock);
		}
		
		if (report) {
			JavaClass javaClass = classContext.getJavaClass();
			String sourceFile = javaClass.getSourceFileName();
			BugInstance warning = new BugInstance("IL_INFINITE_RECURSIVE_LOOP", HIGH_PRIORITY)
					.addClassAndMethod(javaClass, method)
					.addSourceLine(classContext, method, thrower);
			bugReporter.reportBug(warning);
		}
	}

	private boolean targetMethodKnownExactly(ClassContext classContext, Method method, BasicBlock basicBlock, InvokeInstruction ins)
			throws DataflowAnalysisException, CFGBuilderException {
		
		// Ways in which we can be confident that the called method
		// is the same as the calling method:
		// 1. invocation is nonvirtual: invokestatic or invokespecial
		// 2. method is private
		// 3. method or class is final
		// 4. receiver instance is the same as "this"
		// 5. receiver type is known exactly, and is the same as the class
		//    containing the calling method
		
		if (ins.getOpcode() == Constants.INVOKESTATIC || ins.getOpcode() == Constants.INVOKESPECIAL)
			return true;
		
		if (method.isPrivate())
			return true;
		
		if (method.isFinal() || classContext.getJavaClass().isFinal())
			return true;
		
		// See if caller and callee are the same.
		// Technically, the call could still dispatch to a subclass,
		// but that's pretty unlikely.
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		ValueNumber caller = vnaDataflow.getAnalysis().getThisValue();
		ValueNumberFrame frameAtCall = vnaDataflow.getStartFact(basicBlock);
		ValueNumber callee = frameAtCall.getInstance(ins, classContext.getConstantPoolGen());
		if (caller.equals(callee)) {
			return true;
		}
		
		TypeFrame typeFrame = classContext.getTypeDataflow(method).getStartFact(basicBlock); 
		int receiverStackSlot = typeFrame.getInstanceSlot(ins, classContext.getConstantPoolGen());
		
		if (!typeFrame.isExact(receiverStackSlot))
			return false;
		
		Type receiverType = typeFrame.getValue(receiverStackSlot);
		if (!(receiverType instanceof ObjectType))
			return false;
		
		return (((ObjectType) receiverType).getClassName().equals(
				classContext.getJavaClass().getClassName()));
	}

	private boolean allParamsPassedAsArgs(
			ClassContext classContext,
			ValueNumberDataflow vnaDataflow,
			ValueNumberFrame vnaFrameAtEntry,
			int numArgsToCheck,
			BasicBlock basicBlock,
			InvokeInstruction ins) throws DataflowAnalysisException {

		boolean allParamsPassedAsArgs = false;

		ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(basicBlock);
		if (vnaFrame.isValid() && vnaFrame.getStackDepth() >= numArgsToCheck) {
			allParamsPassedAsArgs = true;
			
		checkArgsLoop:
			for (int arg = 0; arg < numArgsToCheck; ++arg) {
				ValueNumber paramVal = vnaFrameAtEntry.getValue(arg);
				ValueNumber argVal = vnaFrame.getOperand(ins, classContext.getConstantPoolGen(), arg); 
				
				if (DEBUG) {
					System.out.println("param="+paramVal.getNumber()+", arg=" + argVal.getNumber());
				}
				
				if (!paramVal.equals(argVal)) {
					allParamsPassedAsArgs = false;
					break checkArgsLoop;
				}
			}
		}
		
		return allParamsPassedAsArgs;
	}

	private boolean checkedStateHasBeenModified(ClassContext classContext, Method method, BasicBlock basicBlock)
			throws CFGBuilderException, DataflowAnalysisException {

		LoadDataflow loadDataflow = classContext.getLoadDataflow(method);
		FieldSet loadSet = loadDataflow.getStartFact(basicBlock);
		
		StoreDataflow storeDataflow = classContext.getStoreDataflow(method);
		FieldSet storeSet = storeDataflow.getStartFact(basicBlock);

		if (DEBUG) {
			System.out.println("Checking state: loads=" + loadSet + ", stores=" + storeSet);
		}
		
		if (loadSet.isEmpty() || storeSet.isEmpty())
			return false;
		
		// Bottom means "any field is potentially loaded or stored"
		if (loadSet.isBottom() || storeSet.isBottom())
			return true;
		
		// Top generally means dead code, so we should repress the warning
		if (loadSet.isTop() || storeSet.isTop())
			return true;

		return loadSet.isIntersectionNonEmpty(storeSet);
	}
	
	private boolean isCallToAdd(InvokeInstruction ins, ConstantPoolGen cpg) {
		return ins.getOpcode() != Constants.INVOKESTATIC 
			&& ins.getName(cpg).equals("add")
			&& ins.getSignature(cpg).equals("(Ljava/lang/Object;)Z");
	}

	private void checkCallToAdd(
			ClassContext classContext,
			Method method,
			BasicBlock basicBlock,
			InstructionHandle thrower) throws DataflowAnalysisException, CFGBuilderException {
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(basicBlock);

		if (vnaFrame.isValid() && vnaFrame.getStackDepth() >= 2) {
			ValueNumber top = vnaFrame.getStackValue(0);
			ValueNumber next = vnaFrame.getStackValue(1);
			if (DEBUG) {
				System.out.println("top=" + top.getNumber() + ", next=" + next.getNumber());
			}
			if (top.equals(next)) {
				JavaClass javaClass = classContext.getJavaClass();
				String sourceFile = javaClass.getSourceFileName();
				BugInstance warning = new BugInstance("IL_CONTAINER_ADDED_TO_ITSELF", NORMAL_PRIORITY)
					.addClassAndMethod(javaClass, method)
					.addSourceLine(classContext, method, thrower);
				bugReporter.reportBug(warning);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
	}

}
