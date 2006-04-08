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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

/**
 * Facade for class hierarchy queries.
 * These typically access the class hierarchy using
 * the {@link org.apache.bcel.Repository} class.  Callers should generally
 * expect to handle ClassNotFoundException for when referenced
 * classes can't be found.
 *
 * @author David Hovemeyer
 */
public class Hierarchy {
	private static final boolean DEBUG_METHOD_LOOKUP =
		Boolean.getBoolean("hier.lookup.debug");

	/**
	 * Type of java.lang.Exception.
	 */
	public static final ObjectType EXCEPTION_TYPE = ObjectTypeFactory.getInstance("java.lang.Exception");
	/**
	 * Type of java.lang.Error.
	 */
	public static final ObjectType ERROR_TYPE = ObjectTypeFactory.getInstance("java.lang.Error");
	/**
	 * Type of java.lang.RuntimeException.
	 */
	public static final ObjectType RUNTIME_EXCEPTION_TYPE = ObjectTypeFactory.getInstance("java.lang.RuntimeException");

	/**
	 * Determine whether one class (or reference type) is a subtype
	 * of another.
	 *
	 * @param clsName                    the name of the class or reference type
	 * @param possibleSupertypeClassName the name of the possible superclass
	 * @return true if clsName is a subtype of possibleSupertypeClassName,
	 *         false if not
	 */
	public static boolean isSubtype(String clsName, String possibleSupertypeClassName) throws ClassNotFoundException {
		ObjectType cls = ObjectTypeFactory.getInstance(clsName);
		ObjectType superCls = ObjectTypeFactory.getInstance(possibleSupertypeClassName);
		return isSubtype(cls, superCls);
	}

	/**
	 * Determine if one reference type is a subtype of another.
	 *
	 * @param t                 a reference type
	 * @param possibleSupertype the possible supertype
	 * @return true if t is a subtype of possibleSupertype,
	 *         false if not
	 */
	public static boolean isSubtype(ReferenceType t, ReferenceType possibleSupertype) throws ClassNotFoundException {
		return t.isAssignmentCompatibleWith(possibleSupertype);
	}

	/**
	 * Determine if the given ObjectType reference represents
	 * a <em>universal</em> exception handler.  That is,
	 * one that will catch any kind of exception.
	 *
	 * @param catchType the ObjectType of the exception handler
	 * @return true if catchType is null, or if catchType is
	 *         java.lang.Throwable
	 */
	public static boolean isUniversalExceptionHandler(ObjectType catchType) {
		return catchType == null || catchType.equals(Type.THROWABLE);
	}

	/**
	 * Determine if the given ObjectType refers to an unchecked
	 * exception (RuntimeException or Error).
	 */
	public static boolean isUncheckedException(ObjectType type) throws ClassNotFoundException {
		return isSubtype(type, RUNTIME_EXCEPTION_TYPE) || isSubtype(type, ERROR_TYPE);
	}

	/**
	 * Determine if method whose name and signature is specified
	 * is a monitor wait operation.
	 *
	 * @param methodName name of the method
	 * @param methodSig  signature of the method
	 * @return true if the method is a monitor wait, false if not
	 */
	public static boolean isMonitorWait(String methodName, String methodSig) {
		return methodName.equals("wait") &&
		        (methodSig.equals("()V") || methodSig.equals("(J)V") || methodSig.equals("(JI)V"));
	}
	
	/**
	 * Determine if given Instruction is a monitor wait.
	 * 
	 * @param ins the Instruction
	 * @param cpg the ConstantPoolGen for the Instruction
	 *
	 * @return true if the instruction is a monitor wait, false if not
	 */
	public static boolean isMonitorWait(Instruction ins, ConstantPoolGen cpg) {
		if (!(ins instanceof InvokeInstruction))
			return false;
		if (ins.getOpcode() == Constants.INVOKESTATIC)
			return false;
		
		InvokeInstruction inv = (InvokeInstruction) ins;
		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);
		
		return isMonitorWait(methodName, methodSig);
	}

	/**
	 * Determine if method whose name and signature is specified
	 * is a monitor notify operation.
	 *
	 * @param methodName name of the method
	 * @param methodSig  signature of the method
	 * @return true if the method is a monitor notify, false if not
	 */
	public static boolean isMonitorNotify(String methodName, String methodSig) {
		return (methodName.equals("notify") || methodName.equals("notifyAll")) &&
		        methodSig.equals("()V");
	}
	/**
	 * Determine if given Instruction is a monitor wait.
	 * 
	 * @param ins the Instruction
	 * @param cpg the ConstantPoolGen for the Instruction
	 *
	 * @return true if the instruction is a monitor wait, false if not
	 */
	public static boolean isMonitorNotify(Instruction ins, ConstantPoolGen cpg) {
		if (!(ins instanceof InvokeInstruction))
			return false;
		if (ins.getOpcode() == Constants.INVOKESTATIC)
			return false;
		
		InvokeInstruction inv = (InvokeInstruction) ins;
		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);
		
		return isMonitorNotify(methodName, methodSig);
	}

	/**
	 * Look up the method referenced by given InvokeInstruction.
	 * This method does <em>not</em> look for implementations in
	 * super or subclasses according to the virtual dispatch rules.
	 *
	 * @param inv the InvokeInstruction
	 * @param cpg the ConstantPoolGen used by the class the InvokeInstruction belongs to
	 * @return the JavaClassAndMethod, or null if no such method is defined in the class
	 */
	public static JavaClassAndMethod findExactMethod(InvokeInstruction inv, ConstantPoolGen cpg) throws ClassNotFoundException {
		return findExactMethod(inv, cpg, ANY_METHOD);
	}

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
	public static JavaClassAndMethod findExactMethod(
			InvokeInstruction inv,
			ConstantPoolGen cpg,
			JavaClassAndMethodChooser chooser) throws ClassNotFoundException {
		String className = inv.getClassName(cpg);
		String methodName = inv.getName(cpg);
		String methodSig = inv.getSignature(cpg);

		JavaClass jclass = Repository.lookupClass(className);
		return findMethod(jclass, methodName, methodSig, chooser);
	}
	
	/**
	 * Visit all superclass methods which the given method overrides. 
	 * 
	 * @param method  the method
	 * @param chooser chooser which visits each superclass method
	 * @return the chosen method, or null if no method is chosen
	 * @throws ClassNotFoundException
	 */
	public static JavaClassAndMethod visitSuperClassMethods(
			JavaClassAndMethod method, JavaClassAndMethodChooser chooser) throws ClassNotFoundException {
		return findMethod(
				method.getJavaClass().getSuperClasses(),
				method.getMethod().getName(),
				method.getMethod().getSignature(),
				chooser);
	}
	
	/**
	 * Visit all superinterface methods which the given method implements. 
	 * 
	 * @param method  the method
	 * @param chooser chooser which visits each superinterface method
	 * @return the chosen method, or null if no method is chosen
	 * @throws ClassNotFoundException
	 */
	public static JavaClassAndMethod visitSuperInterfaceMethods(
			JavaClassAndMethod method, JavaClassAndMethodChooser chooser) throws ClassNotFoundException {
		return findMethod(
				method.getJavaClass().getAllInterfaces(),
				method.getMethod().getName(),
				method.getMethod().getSignature(),
				chooser);
	}
	
	/**
	 * Find the least upper bound method in the class hierarchy
	 * which could be called by the given InvokeInstruction.
	 * One reason this method is useful is that it indicates 
	 * which declared exceptions are thrown by the called methods.
	 * 
	 * <p/>
	 * <ul>
	 * <li> For  invokespecial, this is simply an
	 * exact lookup.
	 * <li> For invokestatic and invokevirtual, the named class is searched,
	 * followed by superclasses  up to the root of the object
	 * hierarchy (java.lang.Object).  Yes, invokestatic really is declared
	 * to check superclasses.  See VMSpec, 2nd ed, sec. 5.4.3.3.
	 * <li> For invokeinterface, the named class is searched,
	 * followed by all interfaces transitively declared by the class.
	 * (Question: is the order important here? Maybe the VM spec
	 * requires that the actual interface desired is given,
	 * so the extended lookup will not be required. Should check.)
	 * </ul>
	 *
	 * @param inv           the InvokeInstruction
	 * @param cpg           the ConstantPoolGen used by the class the InvokeInstruction belongs to
	 * @return the JavaClassAndMethod, or null if no matching method can be found
	 */
	public static JavaClassAndMethod findInvocationLeastUpperBound(
			InvokeInstruction inv, ConstantPoolGen cpg)
	        throws ClassNotFoundException {
		return findInvocationLeastUpperBound(inv, cpg, ANY_METHOD);
	}

	public static JavaClassAndMethod findInvocationLeastUpperBound(
			InvokeInstruction inv, ConstantPoolGen cpg, JavaClassAndMethodChooser methodChooser)
	        throws ClassNotFoundException {
		JavaClassAndMethod result;
		
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("Find prototype method for " +
					SignatureConverter.convertMethodSignature(inv,cpg));
		}
		
		short opcode = inv.getOpcode();
		
		if (methodChooser != ANY_METHOD) {
			methodChooser = new CompoundMethodChooser(new JavaClassAndMethodChooser[]{
					methodChooser, opcode == Constants.INVOKESTATIC ? STATIC_METHOD : INSTANCE_METHOD
			});
		}

		// Find the method
		if (opcode == Constants.INVOKESPECIAL) {
			// Non-virtual dispatch
			result = findExactMethod(inv, cpg, methodChooser);
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

			if (opcode == Constants.INVOKEVIRTUAL || opcode == Constants.INVOKESTATIC) {
				if (DEBUG_METHOD_LOOKUP) {
					System.out.println("[invokevirtual or invokestatic]");
				}
				// Dispatch where the class hierarchy is searched
				// Check superclasses
				result = findMethod(Repository.lookupClass(className), methodName, methodSig, methodChooser);
				if (result == null) {
					if (DEBUG_METHOD_LOOKUP) {
						System.out.println("[not in class, checking superclasses...]");
					}
					JavaClass[] superClassList = Repository.getSuperClasses(className);
					result = findMethod(superClassList, methodName, methodSig, methodChooser);
				}
			} else {
				// Check superinterfaces
				result = findMethod(Repository.lookupClass(className), methodName, methodSig, methodChooser);
				if (result == null) {
					JavaClass[] interfaceList = Repository.getInterfaces(className);
					result = findMethod(interfaceList, methodName, methodSig, methodChooser);
				}
			}
		}

		return result;
	}

	/**
	 * Find the declared exceptions for the method called
	 * by given instruction.
	 *
	 * @param inv the InvokeInstruction
	 * @param cpg the ConstantPoolGen used by the class the InvokeInstruction belongs to
	 * @return array of ObjectTypes of thrown exceptions, or null
	 *         if we can't find the list of declared exceptions
	 */
	public static ObjectType[] findDeclaredExceptions(InvokeInstruction inv, ConstantPoolGen cpg)
	        throws ClassNotFoundException {
		JavaClassAndMethod method = findInvocationLeastUpperBound(inv, cpg);

		if (method == null)
			return null;

		ExceptionTable exTable = method.getMethod().getExceptionTable();
		if (exTable == null)
			return new ObjectType[0];

		String[] exNameList = exTable.getExceptionNames();
		ObjectType[] result = new ObjectType[exNameList.length];
		for (int i = 0; i < exNameList.length; ++i) {
			result[i] = ObjectTypeFactory.getInstance(exNameList[i]);
		}
		return result;
	}

	/**
	 * Find a method in given class.
	 *
	 * @param javaClass  the class
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	public static JavaClassAndMethod findMethod(JavaClass javaClass, String methodName, String methodSig) {
		return findMethod(javaClass, methodName, methodSig, ANY_METHOD);
	}

	/**
	 * Find a method in given class.
	 *
	 * @param javaClass  the class
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @param chooser    JavaClassAndMethodChooser to use to select a matching method
	 *                   (assuming class, name, and signature already match)
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	public static JavaClassAndMethod findMethod(
			JavaClass javaClass,
			String methodName,
			String methodSig,
			JavaClassAndMethodChooser chooser) {
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("Check " + javaClass.getClassName());
		}
		Method[] methodList = javaClass.getMethods();
		for (Method method : methodList) {
			JavaClassAndMethod javaClassAndMethod = new JavaClassAndMethod(javaClass, method);
			if (method.getName().equals(methodName)
					&& method.getSignature().equals(methodSig)
					&& chooser.choose(javaClassAndMethod)) {
				if (DEBUG_METHOD_LOOKUP) {
					System.out.println("\t==> FOUND: " + method);
				}
				return new JavaClassAndMethod(javaClass, method);
			}
		}
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("\t==> NOT FOUND");
		}
		return null;
	}
	
	/**
	 * Find a method in given class.
	 *
	 * @param javaClass  the class
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @param chooser    the JavaClassAndMethodChooser to use to screen possible candidates
	 * @return the XMethod, or null if no such method exists in the class
	 */
	public static XMethod findXMethod(JavaClass javaClass, String methodName, String methodSig,
			JavaClassAndMethodChooser chooser) {
		JavaClassAndMethod result = findMethod(javaClass, methodName, methodSig, chooser);
		return result == null ? null : XFactory.createXMethod(result.getJavaClass(), result.getMethod());
	}
	
	/**
	 * JavaClassAndMethodChooser which accepts any method.
	 */
	public static final JavaClassAndMethodChooser ANY_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			return true;
		}
	};
	
	/**
	 * JavaClassAndMethodChooser which accepts only concrete (not abstract or native) methods.
	 * FIXME: perhaps native methods should be concrete.
	 */
	public static final JavaClassAndMethodChooser CONCRETE_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			Method method = javaClassAndMethod.getMethod();
			int accessFlags = method.getAccessFlags();
			return (accessFlags & Constants.ACC_ABSTRACT) == 0
				&& (accessFlags & Constants.ACC_NATIVE) == 0;
		}
	};
	
	/**
	 * JavaClassAndMethodChooser which accepts only static methods.
	 */
	public static final JavaClassAndMethodChooser STATIC_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			return javaClassAndMethod.getMethod().isStatic();
		}
	};
	
	/**
	 * JavaClassAndMethodChooser which accepts only instance methods.
	 */
	public static final JavaClassAndMethodChooser INSTANCE_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			return !javaClassAndMethod.getMethod().isStatic();
		}
	};

	/**
	 * Find a method in given list of classes,
	 * searching the classes in order.
	 *
	 * @param classList  list of classes in which to search
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	public static JavaClassAndMethod findMethod(JavaClass[] classList, String methodName, String methodSig) {
		return findMethod(classList, methodName, methodSig, ANY_METHOD);
	}

	/**
	 * Find a method in given list of classes,
	 * searching the classes in order.
	 *
	 * @param classList  list of classes in which to search
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @param chooser    JavaClassAndMethodChooser to select which methods are considered;
	 *                   it must return true for a method to be returned
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	public static JavaClassAndMethod findMethod(JavaClass[] classList, String methodName, String methodSig,
			JavaClassAndMethodChooser chooser) {
		JavaClassAndMethod m = null;

		for (JavaClass cls : classList) {
			if ((m = findMethod(cls, methodName, methodSig, chooser)) != null)
				break;
		}

		return m;
	}

	/**
	 * Find XMethod for method in given list of classes,
	 * searching the classes in order.
	 *
	 * @param classList  list of classes in which to search
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @return the XMethod, or null if no such method exists in the class
	 */
	public static XMethod findXMethod(JavaClass[] classList, String methodName, String methodSig) {
		return findXMethod(classList, methodName, methodSig, ANY_METHOD);
	}

	/**
	 * Find XMethod for method in given list of classes,
	 * searching the classes in order.
	 *
	 * @param classList  list of classes in which to search
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @param chooser    JavaClassAndMethodChooser to select which methods are considered;
	 *                   it must return true for a method to be returned
	 * @return the XMethod, or null if no such method exists in the class
	 */
	public static XMethod findXMethod(JavaClass[] classList, String methodName, String methodSig,
			JavaClassAndMethodChooser chooser) {
		for (JavaClass cls : classList) {
			JavaClassAndMethod m;
			if ((m = findMethod(cls, methodName, methodSig)) != null && chooser.choose(m)) {
				return XFactory.createXMethod(cls, m.getMethod());
			}
		}
		return null;
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
	public static Set<JavaClassAndMethod> resolveMethodCallTargets(
			InvokeInstruction invokeInstruction,
			TypeFrame typeFrame,
			ConstantPoolGen cpg) throws DataflowAnalysisException, ClassNotFoundException {
		
		short opcode = invokeInstruction.getOpcode(); 
		
		if (opcode == Constants.INVOKESTATIC) {
			HashSet<JavaClassAndMethod> result = new HashSet<JavaClassAndMethod>();
			JavaClassAndMethod targetMethod = findInvocationLeastUpperBound(invokeInstruction, cpg, CONCRETE_METHOD);
			if (targetMethod != null) {
				result.add(targetMethod);
			}
			return result;
		}
		
		if (!typeFrame.isValid()) {
			return new HashSet<JavaClassAndMethod>();
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
				return new HashSet<JavaClassAndMethod>();
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
	public static Set<JavaClassAndMethod> resolveMethodCallTargets(
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
	public static Set<JavaClassAndMethod> resolveMethodCallTargets(
			ReferenceType receiverType,
			InvokeInstruction invokeInstruction,
			ConstantPoolGen cpg,
			boolean receiverTypeIsExact
			) throws ClassNotFoundException {
		HashSet<JavaClassAndMethod> result = new HashSet<JavaClassAndMethod>();
		
		if (invokeInstruction.getOpcode() == Constants.INVOKESTATIC)
			throw new IllegalArgumentException();
		
		String methodName = invokeInstruction.getName(cpg);
		String methodSig = invokeInstruction.getSignature(cpg);
		
		// Array method calls aren't virtual.
		// They should just resolve to Object methods.
		if (receiverType instanceof ArrayType) {
			JavaClass javaLangObject = AnalysisContext.currentAnalysisContext().lookupClass("java.lang.Object");
			JavaClassAndMethod classAndMethod = findMethod(javaLangObject, methodName, methodSig, INSTANCE_METHOD);
			if (classAndMethod != null)
				result.add(classAndMethod);
			return result;
		}
		
		AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
		
		// Get the receiver class.
		JavaClass receiverClass = analysisContext.lookupClass(
				((ObjectType) receiverType).getClassName());

		// Figure out the upper bound for the method.
		// This is what will be called if this is not a virtual call site.
		JavaClassAndMethod upperBound = findMethod(receiverClass, methodName, methodSig, CONCRETE_METHOD);
		if (upperBound == null) {
			// Try superclasses
			JavaClass[] superClassList = receiverClass.getSuperClasses();
			upperBound = findMethod(superClassList, methodName, methodSig, CONCRETE_METHOD);
		}
		if (upperBound != null) {
			if (DEBUG_METHOD_LOOKUP) {
				System.out.println("Adding upper bound: " +
						SignatureConverter.convertMethodSignature(upperBound.getJavaClass(), upperBound.getMethod()));
			}
			result.add(upperBound);
		}
		
		// Is this a virtual call site?
		boolean virtualCall =
			   invokeInstruction.getOpcode() != Constants.INVOKESPECIAL
			&& !receiverTypeIsExact;
		
		if (virtualCall) {
			// This is a true virtual call: assume that any concrete
			// subtype method may be called.
			Set<JavaClass> subTypeSet = analysisContext.getSubtypes().getTransitiveSubtypes(receiverClass);
			for (JavaClass subtype : subTypeSet) {
				JavaClassAndMethod concreteSubtypeMethod = findMethod(subtype, methodName, methodSig, CONCRETE_METHOD);
				if (concreteSubtypeMethod != null) {
					result.add(concreteSubtypeMethod);
				}
			}
		}
		
		return result;
	}
//	
//	/**
//	 * Return whether or not the given method is concrete.
//	 * 
//	 * @param xmethod the method
//	 * @return true if the method is concrete, false otherwise
//	 */
//	public static boolean isConcrete(XMethod xmethod) {
//		int accessFlags = xmethod.getAccessFlags();
//		return (accessFlags & Constants.ACC_ABSTRACT) == 0
//			&& (accessFlags & Constants.ACC_NATIVE) == 0;
//	}

	/**
	 * Find a field with given name defined in given class.
	 *
	 * @param className the name of the class
	 * @param fieldName the name of the field
	 * @return the Field, or null if no such field could be found
	 */
	public static Field findField(String className, String fieldName) throws ClassNotFoundException {
		JavaClass jclass = Repository.lookupClass(className);

		while (jclass != null) {
			Field[] fieldList = jclass.getFields();
			for (Field field : fieldList) {
				if (field.getName().equals(fieldName)) {
					return field;
				}
			}

			jclass = jclass.getSuperClass();
		}

		return null;
	}

/*
	public static JavaClass findClassDefiningField(String className, String fieldName, String fieldSig)
		throws ClassNotFoundException {

		JavaClass jclass = Repository.lookupClass(className);

		while (jclass != null) {
			Field[] fieldList = jclass.getFields();
			for (int i = 0; i < fieldList.length; ++i) {
				Field field = fieldList[i];
				if (field.getName().equals(fieldName) && field.getSignature().equals(fieldSig)) {
					return jclass;
				}
			}
	
			jclass = jclass.getSuperClass();
		}

		return null;
	}
*/

	/**
	 * Look up a field with given name and signature in given class,
	 * returning it as an {@link XField XField} object.
	 * If a field can't be found in the immediate class,
	 * its superclass is search, and so forth.
	 *
	 * @param className name of the class through which the field
	 *                  is referenced
	 * @param fieldName name of the field
	 * @param fieldSig  signature of the field
	 * @return an XField object representing the field, or null if no such field could be found
	 */
	public static XField findXField(String className, String fieldName, String fieldSig)
	        throws ClassNotFoundException {

		JavaClass classDefiningField = Repository.lookupClass(className);

		Field field = null;
		loop:
			while (classDefiningField != null) {
				Field[] fieldList = classDefiningField.getFields();
				for (Field aFieldList : fieldList) {
					field = aFieldList;
					if (field.getName().equals(fieldName) && field.getSignature().equals(fieldSig)) {
						break loop;
					}
				}

				classDefiningField = classDefiningField.getSuperClass();
			}

		if (classDefiningField == null)
			return null;
		else {
			String realClassName = classDefiningField.getClassName();
			int accessFlags = field.getAccessFlags();
			return field.isStatic()
			        ? (XField) new StaticField(realClassName, fieldName, fieldSig, accessFlags)
			        : (XField) new InstanceField(realClassName, fieldName, fieldSig, accessFlags);
		}
	}

	/**
	 * Look up the field referenced by given FieldInstruction,
	 * returning it as an {@link XField XField} object.
	 *
	 * @param fins the FieldInstruction
	 * @param cpg  the ConstantPoolGen used by the class containing the instruction
	 * @return an XField object representing the field, or null
	 *         if no such field could be found
	 */
	public static XField findXField(FieldInstruction fins, @NonNull ConstantPoolGen cpg)
	        throws ClassNotFoundException {

		String className = fins.getClassName(cpg);
		String fieldName = fins.getFieldName(cpg);
		String fieldSig = fins.getSignature(cpg);

		XField xfield = findXField(className, fieldName, fieldSig);
		short opcode = fins.getOpcode();
		if (xfield != null &&
		        xfield.isStatic() == (opcode == Constants.GETSTATIC || opcode == Constants.PUTSTATIC))
			return xfield;
		else
			return null;
	}

	/**
	 * Determine whether the given INVOKESTATIC instruction
	 * is an inner-class field accessor method.
	 * @param inv the INVOKESTATIC instruction
	 * @param cpg the ConstantPoolGen for the method
	 * @return true if the instruction is an inner-class field accessor, false if not
	 */
	public static boolean isInnerClassAccess(INVOKESTATIC inv, ConstantPoolGen cpg) {
		String methodName = inv.getName(cpg);
		return methodName.startsWith("access$");
	}

	/**
	 * Get the InnerClassAccess for access method called
	 * by given INVOKESTATIC.
	 * @param inv the INVOKESTATIC instruction
	 * @param cpg the ConstantPoolGen for the method
	 * @return the InnerClassAccess, or null if the instruction is not
	 *    an inner-class access
	 */
	public static InnerClassAccess getInnerClassAccess(INVOKESTATIC inv, ConstantPoolGen cpg)
			throws ClassNotFoundException {

		String className = inv.getClassName(cpg);
		String methodName = inv.getName(cpg);
		String methodSig = inv.getSignature(cpg);

		InnerClassAccess access = InnerClassAccessMap.instance().getInnerClassAccess(className, methodName);
		return (access != null && access.getMethodSignature().equals(methodSig))
			? access
			: null;
	}
}

// vim:ts=4
