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

import java.util.*;

import edu.umd.cs.findbugs.*;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreDataflow;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;

import org.apache.bcel.Repository;

import org.apache.bcel.classfile.*;

import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.InstructionHandle;;

/**
 * RuntimeExceptionCapture
 *
 * @author Brian Goetz
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class RuntimeExceptionCapture extends BytecodeScanningDetector implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("rec.debug");

	private BugReporter bugReporter;
	private Method method;
	private OpcodeStack stack;
	private List<CaughtException> catchList;
	private List<ThrownException> throwList;

	private static class CaughtException {
		public String exceptionClass;
		public int startOffset, endOffset, sourcePC;
		public boolean seen = false;
		public boolean dead = false;

		public CaughtException(String exceptionClass, int startOffset, int endOffset, int sourcePC) {
			this.exceptionClass = exceptionClass;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.sourcePC = sourcePC;
		}
	}

	private static class ThrownException {
		public String exceptionClass;
		public int offset;

		public ThrownException(String exceptionClass, int offset) {
			this.exceptionClass = exceptionClass;
			this.offset = offset;
		}
	}


	public RuntimeExceptionCapture(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitMethod(Method method) {
		this.method = method;
		if (DEBUG) {
			System.out.println("RuntimeExceptionCapture visiting " + method);
		}
		super.visitMethod(method);
	}

	public void visitCode(Code obj) {
		catchList = new ArrayList<CaughtException>();
		throwList = new ArrayList<ThrownException>();
		stack = new OpcodeStack();

		super.visitCode(obj);

		for (Iterator iterator = catchList.iterator(); iterator.hasNext();) {
			CaughtException caughtException = (CaughtException) iterator.next();
			Set<String> thrownSet = new HashSet<String>();
			for (Iterator iterator1 = throwList.iterator(); iterator1.hasNext();) {
				ThrownException thrownException = (ThrownException) iterator1.next();
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
				for (Iterator iterator1 = catchList.iterator(); iterator1.hasNext();) {
					CaughtException otherException = (CaughtException) iterator1.next();
					if (otherException.startOffset == caughtException.startOffset
						&& otherException.endOffset == caughtException.endOffset) {
					   catchClauses++;
					   if (otherException.exceptionClass.equals("java.lang.RuntimeException"))
						rteCaught = true;
					}
				}
				int range = caughtException.endOffset - caughtException.startOffset;
				if (!rteCaught) {
					int priority = LOW_PRIORITY+1;
					if (range > 300) priority--;
					else if (range < 30) priority++;
					if (catchClauses > 1) priority++;
					if (thrownSet.size() > 1) priority--;
					if (caughtException.dead) priority--;
					bugReporter.reportBug(new BugInstance(this, "REC_CATCH_EXCEPTION", 
							priority)
					        .addClassAndMethod(this)
					        .addSourceLine(this, caughtException.sourcePC));
					}
			}
		}
	}

	public void visit(CodeException obj) {
		super.visit(obj);
		int type = obj.getCatchType();
		if (type == 0) return;
		String name = getConstantPool().constantToString(getConstantPool().getConstant(type));

		CaughtException caughtException =
			new CaughtException(name, obj.getStartPC(), obj.getEndPC(), obj.getHandlerPC());
		catchList.add(caughtException);

		try {
			// See if the store that saves the exception object
			// is alive or dead.  We rely on the fact that javac
			// always (?) emits an ASTORE instruction to save
			// the caught exception.
			LiveLocalStoreDataflow dataflow = getClassContext().getLiveLocalStoreDataflow(this.method);
			CFG cfg = getClassContext().getCFG(method);
			Collection<BasicBlock> blockList = cfg.getBlocksContainingInstructionWithOffset(obj.getHandlerPC());
			for (Iterator<BasicBlock> i = blockList.iterator(); i.hasNext(); ) {
				BasicBlock block = i.next();
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
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Error checking for dead exception store: " + e.toString());
		} catch (CFGBuilderException e) {
			bugReporter.logError("Error checking for dead exception store: " + e.toString());
		}
	}

	public void sawOpcode(int seen) {
		try {
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
						throwList.add(new ThrownException(signature, getPC()));
					}
				}
				break;

			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
				String className = getDottedClassConstantOperand();
				try {
					if (!className.startsWith("[")) {
						JavaClass clazz = Repository.lookupClass(className);
						Method[] methods = clazz.getMethods();
						for (int i = 0; i < methods.length; i++) {
							Method method = methods[i];
							if (method.getName().equals(getNameConstantOperand())
							        && method.getSignature().equals(getSigConstantOperand())) {
								ExceptionTable et = method.getExceptionTable();
								if (et != null) {
									String[] names = et.getExceptionNames();
									for (int j = 0; j < names.length; j++)
										throwList.add(new ThrownException(names[j], getPC()));
								}
								break;
							}
						}
					}
				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
				}
				break;
			default:
				break;
			}
		} finally {
			stack.sawOpcode(this, seen);
		}
	}

}

// vim:ts=4
