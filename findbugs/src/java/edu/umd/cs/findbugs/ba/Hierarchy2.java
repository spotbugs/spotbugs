/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.ba.Hierarchy.DEBUG_METHOD_LOOKUP;
import static edu.umd.cs.findbugs.ba.Hierarchy.INSTANCE_METHOD;
import static edu.umd.cs.findbugs.ba.Hierarchy.STATIC_METHOD;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.Util;

/**
 * Facade for class hierarchy queries.
 * These typically access the class hierarchy using
 * the {@link org.apache.bcel.Repository} class.  Callers should generally
 * expect to handle ClassNotFoundException for when referenced
 * classes can't be found.
 *
 * @author William Pugh
 */
public class Hierarchy2 {

	
	public static final ClassDescriptor ObjectDescriptor =ClassDescriptor.createClassDescriptor("java/lang/Object");
	/**
	 * Look up the method referenced by given InvokeInstruction.
	 * This method does <em>not</em> look for implementations in
	 * super or subclasses according to the virtual dispatch rules.
	 *
	 * @param inv     the InvokeInstruction
	 * @param cpg     the ConstantPoolGen used by the class the InvokeInstruction belongs to
	 * @param chooser JavaClassAndMethodChooser to use to pick the method from among the candidates
	 * @return the JavaClassAndMethod, or null if no such method is defined in the class
	 */
	public static XMethod findExactMethod(
			InvokeInstruction inv,
			ConstantPoolGen cpg,
			JavaClassAndMethodChooser chooser) throws ClassNotFoundException {
		String className = inv.getClassName(cpg);
		String methodName = inv.getName(cpg);
		String methodSig = inv.getSignature(cpg);

		XMethod result = findMethod(ClassDescriptor.createClassDescriptorFromDottedClassName(className), methodName, methodSig, inv instanceof INVOKESTATIC);
	
		return thisOrNothing(result, chooser);
		}

	private static  @CheckForNull XMethod thisOrNothing(XMethod m, JavaClassAndMethodChooser chooser) {
		if (chooser.choose(m)) return m;
		return null;
	}
	public static @CheckForNull XMethod findInvocationLeastUpperBound(
			InvokeInstruction inv, ConstantPoolGen cpg, JavaClassAndMethodChooser methodChooser)
			throws ClassNotFoundException {
		
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("Find prototype method for " +
					SignatureConverter.convertMethodSignature(inv,cpg));
		}

		short opcode = inv.getOpcode();

		if (opcode == Constants.INVOKESTATIC) {
			if (methodChooser == INSTANCE_METHOD) return null;
		} else {
			if (methodChooser == STATIC_METHOD) return null;
		}

		// Find the method
		if (opcode == Constants.INVOKESPECIAL) {
			// Non-virtual dispatch
			return  findExactMethod(inv, cpg, methodChooser);
		} else {
			String className = inv.getClassName(cpg);
			String methodName = inv.getName(cpg);
			String methodSig = inv.getSignature(cpg);
			if (DEBUG_METHOD_LOOKUP) {
				System.out.println("[Class name is " + className + "]");
				System.out.println("[Method name is " + methodName + "]");
				System.out.println("[Method signature is " + methodSig + "]");
			}

			if (className.startsWith("[")) {
				// Java 1.5 allows array classes to appear as the class name
				className= "java.lang.Object";
			}

			try {
	            return thisOrNothing(findInvocationLeastUpperBound(getXClassFromDottedClassName(className), methodName, methodSig, 
	            		opcode == Constants.INVOKESTATIC, opcode == Constants.INVOKEINTERFACE), methodChooser);
            } catch (CheckedAnalysisException e) {
	            return null;
            }
			
		}
	}

	public static @CheckForNull XMethod findInvocationLeastUpperBound(
			ClassDescriptor classDesc, String methodName, String methodSig, 
			boolean invokeStatic,
			boolean invokeInterface) {
		try {
	        return findInvocationLeastUpperBound(
	        		getXClass(classDesc), methodName, methodSig, invokeStatic, invokeInterface);
        } catch (Exception e) {
        	return null;
        }
	}

	public static @CheckForNull XMethod findInvocationLeastUpperBound(
			XClass jClass, String methodName, String methodSig, 
			boolean invokeStatic,
			boolean invokeInterface)
			throws ClassNotFoundException {
		XMethod result = findMethod(jClass.getClassDescriptor(), methodName, methodSig, invokeStatic);
		if (result != null) return result;
		if (invokeInterface) 
			for(ClassDescriptor i : jClass.getInterfaceDescriptorList()) {
				result = findInvocationLeastUpperBound(i, methodName, methodSig, invokeStatic, invokeInterface);
				if (result != null) return null;
		}
		else {
			ClassDescriptor sClass = jClass.getSuperclassDescriptor();
			if (sClass != null)
				return findInvocationLeastUpperBound(sClass, methodName, methodSig, invokeStatic, invokeInterface);
		}
		return null;
		
	}

	public static @CheckForNull XMethod findMethod(ClassDescriptor classDescriptor, String methodName, String methodSig, boolean isStatic) {
		try {
	        return getXClass(classDescriptor).findMethod(methodName, methodSig, isStatic);
        } catch (CheckedAnalysisException e) {
	       return null;
        }
	}

	static XClass getXClass(@SlashedClassName String c) throws CheckedAnalysisException {
		return getXClass(ClassDescriptor.createClassDescriptor(c));
	}
	static XClass getXClassFromDottedClassName(@DottedClassName String c) throws CheckedAnalysisException {
		return getXClass(ClassDescriptor.createClassDescriptorFromDottedClassName(c));
	}

	static XClass getXClass(ClassDescriptor c) throws CheckedAnalysisException {
		return Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
	}
	/**
	 * Resolve possible method call targets.
	 * This works for both static and instance method calls.
	 * 
	 * @param invokeInstruction the InvokeInstruction
	 * @param typeFrame         the TypeFrame containing the types of stack values
	 * @param cpg               the ConstantPoolGen
	 * @return Set of methods which might be called
	 * @throws DataflowAnalysisException 
	 * @throws ClassNotFoundException 
	 */
	public static @NonNull Set<XMethod> resolveMethodCallTargets(
			InvokeInstruction invokeInstruction,
			TypeFrame typeFrame,
			ConstantPoolGen cpg) throws DataflowAnalysisException, ClassNotFoundException {

		short opcode = invokeInstruction.getOpcode(); 

		if (opcode == Constants.INVOKESTATIC) {
			return Util.emptyOrNonnullSingleton(findInvocationLeastUpperBound(invokeInstruction, cpg, STATIC_METHOD));
		}

		if (!typeFrame.isValid()) {
			return Collections.emptySet();
		}

		Type receiverType;
		boolean receiverTypeIsExact;

		if (opcode == Constants.INVOKESPECIAL) {
			// invokespecial instructions are dispatched to EXACTLY
			// the class specified by the instruction
			receiverType = ObjectTypeFactory.getInstance(invokeInstruction.getClassName(cpg));
			receiverTypeIsExact = false; // Doesn't actually matter
		} else {
			// For invokevirtual and invokeinterface instructions, we have
			// virtual dispatch.  By taking the receiver type (which may be a
			// subtype of the class specified by the instruction),
			// we may get a more precise set of call targets.
			int instanceStackLocation = typeFrame.getInstanceStackLocation(invokeInstruction, cpg);
			receiverType = typeFrame.getStackValue(instanceStackLocation);
			if (!(receiverType instanceof ReferenceType)) {
				return Collections.emptySet();
			}
			receiverTypeIsExact = typeFrame.isExact(instanceStackLocation);
		}
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("[receiver type is " + receiverType + ", " +
					(receiverTypeIsExact ? "exact]" : " not exact]"));
		}

		return resolveMethodCallTargets((ReferenceType) receiverType, invokeInstruction, cpg, receiverTypeIsExact);
	}
	
	/**
	 * Resolve possible instance method call targets.
	 * Assumes that invokevirtual and invokeinterface methods may
	 * call any subtype of the receiver class.
	 * 
	 * @param receiverType      type of the receiver object
	 * @param invokeInstruction the InvokeInstruction
	 * @param cpg               the ConstantPoolGen
	 * @return Set of methods which might be called
	 * @throws ClassNotFoundException
	 */
	public static Set<XMethod> resolveMethodCallTargets(
			ReferenceType receiverType,
			InvokeInstruction invokeInstruction,
			ConstantPoolGen cpg
			) throws ClassNotFoundException {
		return resolveMethodCallTargets(receiverType, invokeInstruction, cpg, false);
	}
	/**
	 * Resolve possible instance method call targets.
	 * 
	 * @param receiverType        type of the receiver object
	 * @param invokeInstruction   the InvokeInstruction
	 * @param cpg                 the ConstantPoolGen
	 * @param receiverTypeIsExact if true, the receiver type is known exactly,
	 *                            which should allow a precise result
	 * @return Set of methods which might be called
	 * @throws ClassNotFoundException
	 */
	public static Set<XMethod> resolveMethodCallTargets(
			ReferenceType receiverType,
			InvokeInstruction invokeInstruction,
			ConstantPoolGen cpg,
			boolean receiverTypeIsExact
			) throws ClassNotFoundException {
		HashSet<XMethod> result = new HashSet<XMethod>();

		if (invokeInstruction.getOpcode() == Constants.INVOKESTATIC)
			throw new IllegalArgumentException();

		String methodName = invokeInstruction.getName(cpg);
		String methodSig = invokeInstruction.getSignature(cpg);

		// Array method calls aren't virtual.
		// They should just resolve to Object methods.
		if (receiverType instanceof ArrayType)
	        try {
	            return Util.emptyOrNonnullSingleton(getXClass(ObjectDescriptor).findMethod(methodName, methodSig, false));
            } catch (CheckedAnalysisException e) {
	            return Collections.emptySet();
            }

		AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();

		// Get the receiver class.
		String receiverClassName = ((ObjectType) receiverType).getClassName();
		ClassDescriptor receiverDesc = ClassDescriptor.createClassDescriptorFromDottedClassName(receiverClassName);
		XClass xClass;
        try {
	        xClass = getXClass(receiverDesc);
        } catch (CheckedAnalysisException e) {
	      return Collections.emptySet();
        }
		// Figure out the upper bound for the method.
		// This is what will be called if this is not a virtual call site.
		XMethod upperBound = findMethod(receiverDesc, methodName, methodSig, false);
		if (upperBound == null) {
			upperBound = findInvocationLeastUpperBound(xClass, methodName, methodSig, false, false);
		}
		if (upperBound != null) {
			if (DEBUG_METHOD_LOOKUP) {
				System.out.println("Adding upper bound: " + upperBound);
			}
			result.add(upperBound);
		}

		// Is this a virtual call site?
		boolean virtualCall =
			   (invokeInstruction.getOpcode() == Constants.INVOKEVIRTUAL || invokeInstruction.getOpcode() == Constants.INVOKEINTERFACE)
			   && (upperBound == null || !upperBound.isFinal())
			&& !receiverTypeIsExact;

		if (virtualCall) {
			if (!receiverClassName.equals("java.lang.Object")) {

			// This is a true virtual call: assume that any concrete
			// subtype method may be called.
			Set<ClassDescriptor> subTypeSet = analysisContext.getSubtypes2().getSubtypes(receiverDesc);
			for (ClassDescriptor subtype : subTypeSet) {
				XMethod concreteSubtypeMethod = findMethod(subtype, methodName, methodSig, false);
				if (concreteSubtypeMethod != null && (concreteSubtypeMethod.getAccessFlags() & Constants.ACC_ABSTRACT) == 0 ) {
					result.add(concreteSubtypeMethod);
				}
			}
			if (false && subTypeSet.size() > 500)
				new RuntimeException(receiverClassName + " has " + subTypeSet.size() + " subclasses, " + result.size() + " of which implement " + methodName+methodSig + " " + invokeInstruction).printStackTrace(System.out);
			
			}
		}
		return result;
	}
}

// vim:ts=4
