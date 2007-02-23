/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.ClassContext;
import java.util.*;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;

public class UselessSubclassMethod extends BytecodeScanningDetector implements StatelessDetector {

	public static final int SEEN_NOTHING = 0;
	public static final int SEEN_PARM = 1;
	public static final int SEEN_LAST_PARM = 2;
	public static final int SEEN_INVOKE = 3;
	public static final int SEEN_RETURN = 4;
	public static final int SEEN_INVALID = 5;
	
	private BugReporter bugReporter;
	private String superclassName;
	private int state;
	private int curParm;
	private int curParmOffset;
	private int invokePC;
	private Type[] argTypes;
	private Set<String> interfaceMethods = null;
	
	public UselessSubclassMethod(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	
	@Override
         public void visitClassContext(ClassContext classContext) {
		try {
			JavaClass cls = classContext.getJavaClass();
			superclassName = cls.getSuperclassName();
			JavaClass[] interfaces = null;
			if (cls.isClass() && ((cls.getAccessFlags() & Constants.ACC_ABSTRACT) != 0)) {
				interfaces = cls.getAllInterfaces();
				interfaceMethods = new HashSet<String>();
				for (JavaClass aInterface : interfaces) {
					Method[] infMethods = aInterface.getMethods();
					for (Method meth : infMethods) {
						interfaceMethods.add(meth.getName() + meth.getSignature());
					}
				}
			}
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
		super.visitClassContext(classContext);
	}
	
	@Override
	public void visitAfter(JavaClass obj) {
		interfaceMethods = null;
		super.visitAfter(obj);
	}
	
	@Override
         public void visitMethod(Method obj) {
		if ((interfaceMethods != null) && ((obj.getAccessFlags() & Constants.ACC_ABSTRACT) != 0)) {
			String curDetail = obj.getName() + obj.getSignature();
			for (String infMethodDetail : interfaceMethods) {
				if (curDetail.equals(infMethodDetail))
					bugReporter.reportBug(new BugInstance(this, "USM_USELESS_ABSTRACT_METHOD", LOW_PRIORITY)
							.addClassAndMethod(getClassContext().getJavaClass(), obj));
			}
		}
		super.visitMethod(obj);
	}
	
	@Override
         public void visitCode(Code obj)
	{
		try {
			String methodName = getMethodName();
			
			if (!methodName.equals("<init>")
			&&  !methodName.equals("clone")
			&&  ((getMethod().getAccessFlags() & (Constants.ACC_STATIC|Constants.ACC_SYNTHETIC)) == 0)) {
				
				/* for some reason, access flags doesn't return Synthetic, so do this hocus pocus */
				Attribute[] atts = getMethod().getAttributes();
				for (Attribute att : atts) {
					if (att.getClass().equals(Synthetic.class))
						return;
				}
				
				byte[] codeBytes = obj.getCode();
				if ((codeBytes.length == 0) || (codeBytes[0] != ALOAD_0))
					return;
				
				state = SEEN_NOTHING;
				invokePC = 0;
				super.visitCode(obj);
				if ((state == SEEN_RETURN) && (invokePC != 0)) {
					//Do this check late, as it is potentially expensive
					Method superMethod = findSuperclassMethod(superclassName, getMethod());
					if ((superMethod == null) || accessModifiersAreDifferent(getMethod(), superMethod))
						return;
	
					bugReporter.reportBug( new BugInstance( this, "USM_USELESS_SUBCLASS_METHOD", LOW_PRIORITY )
						.addClassAndMethod(this)
						.addSourceLine(this, invokePC));
				}
			}
		}
		catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}
	
	@Override
         public void sawOpcode(int seen) {
		switch (state) {
			case SEEN_NOTHING:
				if (seen == ALOAD_0) {
					argTypes = Type.getArgumentTypes(this.getMethodSig()); 
					curParm = 0;
					curParmOffset = 1;
					if (argTypes.length > 0)
						state = SEEN_PARM;
					else
						state = SEEN_LAST_PARM;
				} else
					state = SEEN_INVALID;	
			break;
			
			case SEEN_PARM:
				if (curParm >= argTypes.length)
					state = SEEN_INVALID;
				else {
					String signature = argTypes[curParm++].getSignature();
					char typeChar0 = signature.charAt(0);
					if ((typeChar0 == 'L') || (typeChar0 == '[')) {
						checkParm(seen, ALOAD_0, ALOAD, 1);
					}
					else if (typeChar0 == 'D') {
						checkParm(seen, DLOAD_0, DLOAD, 2);
					}
					else if (typeChar0 == 'F') {
						checkParm(seen, FLOAD_0, FLOAD, 1);
					}
					else if (typeChar0 == 'I') {
						checkParm(seen, ILOAD_0, ILOAD, 1);
					}
					else if (typeChar0 == 'J') {
						checkParm(seen, LLOAD_0, LLOAD, 2);
					}
					if ((state != SEEN_INVALID) && (curParm >= argTypes.length))
						state = SEEN_LAST_PARM;
						
				}
			break;
			
			case SEEN_LAST_PARM:
				if ((seen == INVOKENONVIRTUAL) && getMethodName().equals(getNameConstantOperand()) && getMethodSig().equals(getSigConstantOperand())) {
					invokePC = getPC();
					state = SEEN_INVOKE;
				}
				else
					state = SEEN_INVALID;
			break;
			
			case SEEN_INVOKE:
				Type returnType = getMethod().getReturnType();
				char retSigChar0 = returnType.getSignature().charAt(0);
				if ((retSigChar0 == 'V') && (seen == RETURN))
					state = SEEN_RETURN;
				else if (((retSigChar0 == 'L') || (retSigChar0 == '[')) && (seen == ARETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'D') && (seen == DRETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'F') && (seen == FRETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'I' || retSigChar0 == 'S'  || retSigChar0 == 'C'  || retSigChar0 == 'B'  || retSigChar0 == 'Z' ) && (seen == IRETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'J') && (seen == LRETURN))
					state = SEEN_RETURN;
				else
					state = SEEN_INVALID;
			break;
			
			case SEEN_RETURN:
				state = SEEN_INVALID;
			break;
		}
	}
	
	private void checkParm(int seen, int fastOpBase, int slowOp, int parmSize) {
		if ((curParmOffset >= 1) && (curParmOffset <= 3)) {
			if (seen == (fastOpBase + curParmOffset))
				curParmOffset += parmSize;
			else
				state = SEEN_INVALID;
		}
		else if (curParmOffset == 0)
			state = SEEN_INVALID;
		else if ((seen == slowOp) && (getRegisterOperand() == curParmOffset))
			curParmOffset += parmSize;
		else
			state = SEEN_INVALID;
	}
	
	private Method findSuperclassMethod(String superclassName, Method subclassMethod) 
		throws ClassNotFoundException {
		
		String methodName = subclassMethod.getName();
		Type[] subArgs = null;
		JavaClass superClass = Repository.lookupClass(superclassName);
		Method[] methods = superClass.getMethods();
		outer:
		for (Method m : methods) {
			if (m.getName().equals(methodName)) {
				if (subArgs == null)
					subArgs = Type.getArgumentTypes(subclassMethod.getSignature());
				Type[] superArgs = Type.getArgumentTypes(m.getSignature());
				if (subArgs.length == superArgs.length) {
					for (int j = 0; j < subArgs.length; j++) {
						if (!superArgs[j].equals(subArgs[j]))
							continue outer;
					}
					return m;
				}
			}
		}
		
		if(!superclassName.equals("Object")) {
			String superSuperClassName = superClass.getSuperclassName();
			if (superSuperClassName.equals(superclassName)) {
				throw new ClassNotFoundException(
					"superclass of " + superclassName + " is itself");
				}
			return findSuperclassMethod(superClass.getSuperclassName(), subclassMethod);
			}
		
		return null;
	}
	
	private boolean accessModifiersAreDifferent(Method m1, Method m2) {
		int access1 = m1.getAccessFlags() & (Constants.ACC_PRIVATE|Constants.ACC_PROTECTED|Constants.ACC_PUBLIC|Constants.ACC_FINAL);
		int access2 = m2.getAccessFlags() & (Constants.ACC_PRIVATE|Constants.ACC_PROTECTED|Constants.ACC_PUBLIC|Constants.ACC_FINAL);

		return access1 != access2;
	}
}
