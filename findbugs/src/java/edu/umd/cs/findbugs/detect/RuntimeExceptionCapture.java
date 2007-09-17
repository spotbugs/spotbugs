/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Brian Goetz <briangoetz@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.LiveLocalStoreDataflow;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;
;

/**
 * RuntimeExceptionCapture
 *
 * @author Brian Goetz
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class RuntimeExceptionCapture extends OpcodeStackDetector implements Detector, StatelessDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("rec.debug");

	private BugReporter bugReporter;
	private Method method;
	private List<ExceptionCaught> catchList;
	private List<ExceptionThrown> throwList;

	private BugAccumulator accumulator;
	private static class ExceptionCaught {
		public String exceptionClass;
		public int startOffset, endOffset, sourcePC;
		public boolean seen = false;
		public boolean dead = false;

		public ExceptionCaught(String exceptionClass, int startOffset, int endOffset, int sourcePC) {
			this.exceptionClass = exceptionClass;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.sourcePC = sourcePC;
		}
	}

	private static class ExceptionThrown {
		public @DottedClassName String exceptionClass;
		public int offset;

		public ExceptionThrown(@DottedClassName String exceptionClass, int offset) {
			this.exceptionClass = exceptionClass;
			this.offset = offset;
		}
	}


	public RuntimeExceptionCapture(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		accumulator = new BugAccumulator(bugReporter);
	}



	@Override
		 public void visitMethod(Method method) {
		this.method = method;
		if (DEBUG) {
			System.out.println("RuntimeExceptionCapture visiting " + method);
		}
		super.visitMethod(method);
		accumulator.reportAccumulatedBugs();
	}

	@Override
		 public void visitCode(Code obj) {
		catchList = new ArrayList<ExceptionCaught>();
		throwList = new ArrayList<ExceptionThrown>();

		super.visitCode(obj);

		for (ExceptionCaught caughtException : catchList) {
			Set<String> thrownSet = new HashSet<String>();
			for (ExceptionThrown thrownException : throwList) {
				if (thrownException.offset >= caughtException.startOffset
						&& thrownException.offset < caughtException.endOffset) {
					thrownSet.add(thrownException.exceptionClass);
					if (thrownException.exceptionClass.equals(caughtException.exceptionClass))
						caughtException.seen = true;
				}
			}
			int catchClauses = 0;
			if (caughtException.exceptionClass.equals("java.lang.Exception") && !caughtException.seen) {
				// Now we have a case where Exception is caught, but not thrown
				boolean rteCaught = false;
				for (ExceptionCaught otherException : catchList) {
					if (otherException.startOffset == caughtException.startOffset
							&& otherException.endOffset == caughtException.endOffset) {
						catchClauses++;
						if (otherException.exceptionClass.equals("java.lang.RuntimeException"))
							rteCaught = true;
					}
				}
				int range = caughtException.endOffset - caughtException.startOffset;
				if (!rteCaught) {
					int priority = LOW_PRIORITY + 1;
					if (range > 300) priority--;
					else if (range < 30) priority++;
					if (catchClauses > 1) priority++;
					if (thrownSet.size() > 1) priority--;
					if (caughtException.dead) priority--;
					accumulator.accumulateBug(new BugInstance(this, "REC_CATCH_EXCEPTION",
							priority)
							.addClassAndMethod(this), 
							SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, caughtException.sourcePC));
				}
			}
		}
	}

	@Override
		 public void visit(CodeException obj) {
		try {
		super.visit(obj);
		int type = obj.getCatchType();
		if (type == 0) return;
		String name = getConstantPool().constantToString(getConstantPool().getConstant(type));

		ExceptionCaught caughtException =
			new ExceptionCaught(name, obj.getStartPC(), obj.getEndPC(), obj.getHandlerPC());
		catchList.add(caughtException);

			// See if the store that saves the exception object
			// is alive or dead.  We rely on the fact that javac
			// always (?) emits an ASTORE instruction to save
			// the caught exception.
			LiveLocalStoreDataflow dataflow = getClassContext().getLiveLocalStoreDataflow(this.method);
			CFG cfg = getClassContext().getCFG(method);
			Collection<BasicBlock> blockList = cfg.getBlocksContainingInstructionWithOffset(obj.getHandlerPC());
			for (BasicBlock block : blockList) {
				InstructionHandle first = block.getFirstInstruction();
				if (first != null
						&& first.getPosition() == obj.getHandlerPC()
						&& first.getInstruction() instanceof ASTORE) {
					ASTORE astore = (ASTORE) first.getInstruction();
					BitSet liveStoreSet = dataflow.getFactAtLocation(new Location(first, block));
					if (!liveStoreSet.get(astore.getIndex())) {
						// The ASTORE storing the exception object is dead
						if (DEBUG) {
							System.out.println("Dead exception store at " + first);
						}
						caughtException.dead = true;
						break;
					}
				}
			}
		} catch (MethodUnprofitableException e) {
			Method m = getMethod();
			bugReporter.reportSkippedAnalysis(DescriptorFactory.instance().getMethodDescriptor(getClassName(), getMethodName(), getMethodSig(), m.isStatic()));
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error checking for dead exception store", e);
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error checking for dead exception store", e);
		}
	}

	@Override
		 public void sawOpcode(int seen) {
			switch (seen) {
			case ATHROW:
				if (stack.getStackDepth() > 0) {
					OpcodeStack.Item item = stack.getStackItem(0);
					String signature = item.getSignature();
					if (signature != null && signature.length() > 0) {
						if (signature.startsWith("L"))
							signature = SignatureConverter.convert(signature);
						else
							signature = signature.replace('/', '.');
						throwList.add(new ExceptionThrown(signature, getPC()));
					}
				}
				break;

			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
				String className = getClassConstantOperand();
				if (!className.startsWith("[")) try {		
					XClass c = Global.getAnalysisCache().getClassAnalysis(XClass.class, DescriptorFactory.createClassDescriptor(className));
					XMethod m = Hierarchy2.findInvocationLeastUpperBound(c, getNameConstantOperand(), getSigConstantOperand(),  seen == INVOKESTATIC,  seen == INVOKEINTERFACE);
					if (m == null) break;
					String[] exceptions = m.getThrownExceptions();
					if (exceptions != null) for (String name : exceptions)
						throwList.add(new ExceptionThrown(ClassName.toDottedClassName(name), getPC()));
				} catch (MissingClassException e) {
					bugReporter.reportMissingClass(e.getClassDescriptor());
				} catch (CheckedAnalysisException e) {
					bugReporter.logError("Error looking up " + className, e);
				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
                }
				break;
			default:
				break;
			}
		
	}


	}

// vim:ts=4
