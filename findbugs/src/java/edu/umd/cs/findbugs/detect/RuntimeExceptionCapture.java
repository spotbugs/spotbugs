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
import edu.umd.cs.findbugs.ba.SignatureConverter;
import org.apache.bcel.classfile.*;
import org.apache.bcel.Repository;

/**
 * RuntimeExceptionCapture
 *
 * @author Brian Goetz
 */
public class RuntimeExceptionCapture extends BytecodeScanningDetector implements Detector {

	private BugReporter bugReporter;
	private OpcodeStack stack;
	private List<CaughtException> catchList;
	private List<ThrownException> throwList;

	private static class CaughtException {
		public String exceptionClass;
		public int startOffset, endOffset, sourcePC;
		public boolean seen = false;

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

	public void visitCode(Code obj) {
		catchList = new ArrayList<CaughtException>();
		throwList = new ArrayList<ThrownException>();
		stack = new OpcodeStack();

		super.visitCode(obj);

		for (Iterator iterator = catchList.iterator(); iterator.hasNext();) {
			CaughtException caughtException = (CaughtException) iterator.next();
			for (Iterator iterator1 = throwList.iterator(); iterator1.hasNext();) {
				ThrownException thrownException = (ThrownException) iterator1.next();
				if (thrownException.exceptionClass.equals(caughtException.exceptionClass)
						&& thrownException.offset >= caughtException.startOffset
				        && thrownException.offset < caughtException.endOffset) {
					caughtException.seen = true;
					break;
				}
			}
			if (caughtException.exceptionClass.equals("java.lang.Exception") && !caughtException.seen) {
				// Now we have a case where Exception is caught, but not thrown
				boolean rteCaught = false;
				for (Iterator iterator1 = catchList.iterator(); iterator1.hasNext();) {
					CaughtException otherException = (CaughtException) iterator1.next();
					if (otherException.exceptionClass.equals("java.lang.RuntimeException")
					        && otherException.startOffset == caughtException.startOffset
							&& otherException.endOffset == caughtException.endOffset) {
						rteCaught = true;
						break;
					}
				}
				int range = caughtException.endOffset - caughtException.startOffset;
				if (!rteCaught && range > 80) {
					bugReporter.reportBug(new BugInstance(this, "REC_CATCH_EXCEPTION", 
			range > 300 ? NORMAL_PRIORITY : LOW_PRIORITY)
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
		catchList.add(new CaughtException(name, obj.getStartPC(), obj.getEndPC(), obj.getHandlerPC()));
	}

	public void sawOpcode(int seen) {
		try {
			switch (seen) {
			case ATHROW:
				OpcodeStack.Item item = stack.getStackItem(0);
				String signature = item.getSignature();
				if (signature != null && signature.length() > 0) {
					if (signature.startsWith("L"))
						signature = SignatureConverter.convert(signature);
					else
						signature = signature.replace('/', '.');
					throwList.add(new ThrownException(signature, getPC()));
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
					System.out.println("Class not found: " + className);
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
