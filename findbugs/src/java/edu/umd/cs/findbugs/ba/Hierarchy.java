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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

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
	protected static final boolean DEBUG_METHOD_LOOKUP =
		SystemProperties.getBoolean("hier.lookup.debug");

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
		if (Subtypes2.ENABLE_SUBTYPES2) {
			try {
				Subtypes2 subtypes2 = Global.getAnalysisCache().getDatabase(Subtypes2.class);
				return subtypes2.isSubtype(t, possibleSupertype);
			} catch (CheckedAnalysisException e) {
				// Should not happen
				IllegalStateException ise = new IllegalStateException("Should not happen");
				ise.initCause(e);
				throw ise;
			}
		} else {
			Map<ReferenceType, Boolean> subtypes = subtypeCache.get(possibleSupertype);
			if (subtypes == null) {
				subtypes = new HashMap<ReferenceType, Boolean>();
				subtypeCache.put(possibleSupertype, subtypes);
			}
			Boolean result = subtypes.get(t);
			if (result == null) {
				result = Boolean.valueOf(t.isAssignmentCompatibleWith(possibleSupertype));
				subtypes.put(t, result);
			}
			return result;
		}
	}

	static Map<ReferenceType, Map<ReferenceType, Boolean>> subtypeCache = new HashMap<ReferenceType, Map<ReferenceType, Boolean>> ();
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
	public static @CheckForNull JavaClassAndMethod findInvocationLeastUpperBound(
			InvokeInstruction inv, ConstantPoolGen cpg)
			throws ClassNotFoundException {
		return findInvocationLeastUpperBound(inv, cpg, ANY_METHOD);
	}

	public static @CheckForNull JavaClassAndMethod findInvocationLeastUpperBound(
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

			JavaClass jClass = Repository.lookupClass(className);
			return findInvocationLeastUpperBound(jClass, methodName, methodSig, methodChooser, opcode == Constants.INVOKEINTERFACE);
			
		}
	}

	public static @CheckForNull JavaClassAndMethod findInvocationLeastUpperBound(
			JavaClass jClass, String methodName, String methodSig, JavaClassAndMethodChooser methodChooser,
			boolean invokeInterface)
			throws ClassNotFoundException {
		JavaClassAndMethod result = findMethod(jClass, methodName, methodSig, methodChooser);
		if (result != null) return result;
		if (invokeInterface) 
			for(JavaClass i : jClass.getInterfaces()) {
				result = findInvocationLeastUpperBound(i, methodName, methodSig, methodChooser, invokeInterface);
				if (result != null) return null;
		}
		else {
			JavaClass sClass = jClass.getSuperClass();
			if (sClass != null)
				return findInvocationLeastUpperBound(sClass, methodName, methodSig, methodChooser, invokeInterface);
		}
		return null;
		
	}
	/**
     * Find the declared exceptions for the method called
     * by given instruction.
     *
     * @param inv the InvokeInstruction
     * @param cpg the ConstantPoolGen used by the class the InvokeInstruction belongs to
     * @return array of ObjectTypes of thrown exceptions, or null
     *         if we can't find the list of declared exceptions
     * @deprecated Use {@link Hierarchy2#findDeclaredExceptions(InvokeInstruction,ConstantPoolGen)} instead
     */
	@Deprecated
    public static ObjectType[] findDeclaredExceptions(InvokeInstruction inv, ConstantPoolGen cpg)
    		throws ClassNotFoundException {
                return Hierarchy2.findDeclaredExceptions(inv, cpg);
            }

	/**
	 * Find a method in given class.
	 *
	 * @param javaClass  the class
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	public static @CheckForNull JavaClassAndMethod findMethod(JavaClass javaClass, String methodName, String methodSig) {
		return findMethod(javaClass, methodName, methodSig, ANY_METHOD);
	}
	public static  @CheckForNull  JavaClassAndMethod findMethod(
			JavaClass javaClass,
			String methodName,
			String methodSig,
			JavaClassAndMethodChooser chooser) {
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("Check " + javaClass.getClassName());
		}
		Method[] methodList = javaClass.getMethods();
		for (Method method : methodList)  if (method.getName().equals(methodName)
				&& method.getSignature().equals(methodSig)) {
			JavaClassAndMethod m = new JavaClassAndMethod(javaClass, method);
			if (chooser.choose(m)) {
				if (DEBUG_METHOD_LOOKUP) {
					System.out.println("\t==> FOUND: " + method);
				}
				return m;
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
	 * @param classDesc  the class descriptor
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @param isStatic    are we looking for a static method?
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	public static  @CheckForNull  XMethod findMethod(
			ClassDescriptor classDesc,
			String methodName,
			String methodSig,
			boolean isStatic) {
		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("Check " + classDesc.getClassName());
		}
		
        try {
        	XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);
	        return xClass.findMethod(methodName, methodSig, isStatic);
        } catch (CheckedAnalysisException e) {
	        AnalysisContext.logError("Error looking for " + classDesc+"."+methodName+methodSig, e);
	        return null;
        }
		
		
	}

	/**
	 * Find a method in given class.
	 *
	 * @param javaClass  the class
	 * @param methodName the name of the method
	 * @param methodSig  the signature of the method
	 * @return the JavaClassAndMethod, or null if no such method exists in the class
	 */
	@Deprecated
	public static  @CheckForNull  JavaClassAndMethod findConcreteMethod(
			JavaClass javaClass,
			String methodName,
			String methodSig) {

		if (DEBUG_METHOD_LOOKUP) {
			System.out.println("Check " + javaClass.getClassName());
		}
		Method[] methodList = javaClass.getMethods();
		for (Method method : methodList)  if (method.getName().equals(methodName)
				&& method.getSignature().equals(methodSig)
				&& accessFlagsAreConcrete(method.getAccessFlags())) {
			JavaClassAndMethod m = new JavaClassAndMethod(javaClass, method);

			return m;

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
	@Deprecated
	public static  @CheckForNull XMethod findXMethod(JavaClass javaClass, String methodName, String methodSig,
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

		public boolean choose(XMethod method) {
	        return true;
        }

	};


	// FIXME: perhaps native methods should be concrete.
	public static boolean accessFlagsAreConcrete(int accessFlags) {
		return (accessFlags & Constants.ACC_ABSTRACT) == 0
			&& (accessFlags & Constants.ACC_NATIVE) == 0;
	}

	/**
	 * JavaClassAndMethodChooser which accepts only concrete (not abstract or native) methods.
	 */
	public static final JavaClassAndMethodChooser CONCRETE_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			Method method = javaClassAndMethod.getMethod();
			int accessFlags = method.getAccessFlags();
			return accessFlagsAreConcrete(accessFlags);
		}
		public boolean choose(XMethod method) {
	        return accessFlagsAreConcrete(method.getAccessFlags());
        }
	};

	/**
	 * JavaClassAndMethodChooser which accepts only static methods.
	 */
	public static final JavaClassAndMethodChooser STATIC_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			return javaClassAndMethod.getMethod().isStatic();
		}
		public boolean choose(XMethod method) {
	        return method.isStatic();
        }
	};

	/**
	 * JavaClassAndMethodChooser which accepts only instance methods.
	 */
	public static final JavaClassAndMethodChooser INSTANCE_METHOD = new JavaClassAndMethodChooser() {
		public boolean choose(JavaClassAndMethod javaClassAndMethod) {
			return !javaClassAndMethod.getMethod().isStatic();
		}

		public boolean choose(XMethod method) {
	        return !method.isStatic();
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
		String receiverClassName = ((ObjectType) receiverType).getClassName();
		JavaClass receiverClass = analysisContext.lookupClass(
				receiverClassName);
		ClassDescriptor receiverDesc = DescriptorFactory.createClassDescriptorFromDottedClassName(receiverClassName);

		// Figure out the upper bound for the method.
		// This is what will be called if this is not a virtual call site.
		JavaClassAndMethod upperBound = findMethod(receiverClass, methodName, methodSig, CONCRETE_METHOD);
		if (upperBound == null) {
			upperBound = findInvocationLeastUpperBound(receiverClass, methodName, methodSig, CONCRETE_METHOD, false);
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
			   (invokeInstruction.getOpcode() == Constants.INVOKEVIRTUAL || invokeInstruction.getOpcode() == Constants.INVOKEINTERFACE)
			   && (upperBound == null || !upperBound.getJavaClass().isFinal() && !upperBound.getMethod().isFinal())
			&& !receiverTypeIsExact;

		if (virtualCall) {
			if (!receiverClassName.equals("java.lang.Object")) {

			// This is a true virtual call: assume that any concrete
			// subtype method may be called.
			Set<ClassDescriptor> subTypeSet = analysisContext.getSubtypes2().getSubtypes(receiverDesc);
			for (ClassDescriptor subtype : subTypeSet) {
				XMethod concreteSubtypeMethod = findMethod(subtype, methodName, methodSig, false);
				if (concreteSubtypeMethod != null && (concreteSubtypeMethod.getAccessFlags() & Constants.ACC_ABSTRACT) == 0 ) {
					result.add(new JavaClassAndMethod(concreteSubtypeMethod));
				}
			}
			if (false && subTypeSet.size() > 500)
				new RuntimeException(receiverClassName + " has " + subTypeSet.size() + " subclasses, " + result.size() + " of which implement " + methodName+methodSig + " " + invokeInstruction).printStackTrace(System.out);
			
			}
		}
		return result;
	}
	
	/**
	 * Return whether or not the given method is concrete.
	 * 
	 * @param xmethod the method
	 * @return true if the method is concrete, false otherwise
	 */
	@Deprecated
	public static boolean isConcrete(XMethod xmethod) {
		int accessFlags = xmethod.getAccessFlags();
		return (accessFlags & Constants.ACC_ABSTRACT) == 0
			&& (accessFlags & Constants.ACC_NATIVE) == 0;
	}

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
	 * @param isStatic  true if field is static, false otherwise
	 * @return an XField object representing the field, or null if no such field could be found
	 */
	public static XField findXField(String className, String fieldName, String fieldSig, boolean isStatic)
			throws ClassNotFoundException {

		return XFactory.createXField(className, fieldName, fieldSig, isStatic);
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
		
		boolean isStatic = (fins.getOpcode() == Constants.GETSTATIC || fins.getOpcode() == Constants.PUTSTATIC);

		XField xfield = findXField(className, fieldName, fieldSig, isStatic);
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

		InnerClassAccess access = AnalysisContext.currentAnalysisContext()
			.getInnerClassAccessMap().getInnerClassAccess(className, methodName);
		return (access != null && access.getMethodSignature().equals(methodSig))
			? access
			: null;
	}


}

// vim:ts=4
