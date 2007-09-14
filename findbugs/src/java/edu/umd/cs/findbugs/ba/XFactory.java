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

package edu.umd.cs.findbugs.ba;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.impl.AnalysisCache;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Factory methods for creating XMethod objects.
 * 
 * @author David Hovemeyer
 */
public  class XFactory {
	public static final boolean DEBUG_UNRESOLVED = SystemProperties.getBoolean("findbugs.xfactory.debugunresolved"); 

	/**
	 * XMethod implementation for unresolvable methods.
	 * Returns some kind of reasonable default answer to questions
	 * that can't be answered (e.g., what are the access flags).
	 */
	private static class UnresolvedXMethod extends AbstractMethod implements XMethod {
		protected UnresolvedXMethod(String className, String methodName, String methodSig, int accessFlags) {
			super(className, methodName, methodSig, accessFlags);
			if (DEBUG_UNRESOLVED) {
				System.out.println("Unresolved xmethod: " + this);
			}
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#isReturnTypeReferenceType()
		 */
		public boolean isReturnTypeReferenceType() {
			SignatureParser parser = new SignatureParser(getSignature());
			String returnTypeSig = parser.getReturnTypeSignature();
			return SignatureParser.isReferenceType(returnTypeSig);
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o instanceof XMethod) {
				return XFactory.compare((XMethod)this, (XMethod)o);
			}
			throw new ClassCastException("Don't know how to compare " + this.getClass().getName() + " to " + o.getClass().getName());
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#getAnnotation(edu.umd.cs.findbugs.classfile.ClassDescriptor)
		 */
		public AnnotationValue getAnnotation(ClassDescriptor desc) {
			return null;
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#getAnnotationDescriptors()
		 */
		public Collection<ClassDescriptor> getAnnotationDescriptors() {
			return TigerSubstitutes.emptyList();
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#getAnnotations()
		 */
		public Collection<AnnotationValue> getAnnotations() {
			return TigerSubstitutes.emptyList();
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#getParameterAnnotation(int, edu.umd.cs.findbugs.classfile.ClassDescriptor)
		 */
		public AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc) {
			return null;
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#getParameterAnnotationDescriptors(int)
		 */
		public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param) {
			return TigerSubstitutes.emptyList();
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.XMethod#getParameterAnnotations(int)
		 */
		public Collection<AnnotationValue> getParameterAnnotations(int param) {
			return TigerSubstitutes.emptyList();
		}

		public ElementType getElementType() {
			if (getName().equals("<init>")) return ElementType.CONSTRUCTOR;
			return ElementType.METHOD;
		}
		
		public @CheckForNull AnnotatedObject getContainingScope() {
			try {
		        return (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, getClassDescriptor());
	        } catch (CheckedAnalysisException e) {
		         return null;
	        }
		}

		/* (non-Javadoc)
         * @see edu.umd.cs.findbugs.ba.XMethod#getThrownExceptions()
         */
        public String[] getThrownExceptions() {
	       
	        return  new String[0];
        }

	}

	private Map<MethodDescriptor, XMethod> methods = new HashMap<MethodDescriptor, XMethod>();

	private Set<ClassMember> deprecated = new HashSet<ClassMember>();
	private Set<? extends ClassMember> deprecatedView = Collections.unmodifiableSet(deprecated);
	private  Map<FieldDescriptor,XField> fields = new HashMap<FieldDescriptor, XField>();

//	private  Set<XMethod> methodsView = Collections
//	.unmodifiableSet(methods.keySet());

//	private  Set<XField> fieldsView = Collections
//	.unmodifiableSet(fields.keySet());

	private  Set<XMethod> calledMethods = new HashSet<XMethod>();
	private Set<String> calledMethodSignatures = new HashSet<String>();
	private boolean calledMethodsIsInterned = false;

	/**
	 * Constructor.
	 */
	public XFactory() {
	}

	public void addCalledMethod(XMethod m) {
		if (calledMethods.add(m) && !m.isResolved())
			calledMethodsIsInterned = false;
	}

	
	public boolean isCalled(XMethod m) {
		if (m.getName().equals("<clinit>")) return true;
		updatedCalledMethods();
		return calledMethods.contains(m);
	}

	public boolean isCalledDirectlyOrIndirectly(XMethod m) {
		if (isCalled(m)) return true;
		if (m.isStatic() || m.isPrivate() || m.getName().equals("<init>")) return false;
		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			XClass clazz =  analysisCache.getClassAnalysis(XClass.class, m.getClassDescriptor());
			if (isCalledDirectlyOrIndirectly(clazz.getSuperclassDescriptor(), m)) return true;
			for(ClassDescriptor i : clazz.getInterfaceDescriptorList())
				if (isCalledDirectlyOrIndirectly(i, m)) return true;

			return false;
		} catch (edu.umd.cs.findbugs.classfile.MissingClassException e) {
			// AnalysisContext.reportMissingClass(e.getClassNotFoundException());
			return false;
		} catch (MissingClassException e) {
			AnalysisContext.reportMissingClass(e.getClassNotFoundException());
			return false;
		} catch (Exception e) {
			AnalysisContext.logError("Error checking to see if " + m + " is called (" + e.getClass().getCanonicalName()+")", e);
			return false;
		}
	}
	/**
     * @param superclassDescriptor
     * @param m
     * @return
	 * @throws CheckedAnalysisException 
     */
    private boolean isCalledDirectlyOrIndirectly(@CheckForNull ClassDescriptor clazzDescriptor, XMethod m) throws CheckedAnalysisException {
    	if (clazzDescriptor == null) return false;
    	IAnalysisCache analysisCache = Global.getAnalysisCache();
		XClass clazz =  analysisCache.getClassAnalysis(XClass.class, clazzDescriptor);
		XMethod m2 = clazz.findMethod(m.getName(), m.getSignature(), m.isStatic());
		if (m2 != null && isCalled(m2)) return true;
		if (isCalledDirectlyOrIndirectly(clazz.getSuperclassDescriptor(), m)) return true;
		for(ClassDescriptor i : clazz.getInterfaceDescriptorList())
			if (isCalledDirectlyOrIndirectly(i, m)) return true;
		
		return false;
		
    }

	public boolean nameAndSignatureIsCalled(XMethod m) {
		updatedCalledMethods();
		return calledMethodSignatures.contains(getDetailedSignature(m));
	}

	/**
     * 
     */
    private void updatedCalledMethods() {
	    if (!calledMethodsIsInterned) {
			Set<XMethod> tmp = new HashSet<XMethod>();
			calledMethodSignatures.clear();
			for(XMethod m2 : calledMethods) {
				tmp.add(intern(m2));
				calledMethodSignatures.add(getDetailedSignature(m2));
			}
			calledMethods = tmp;
			
			calledMethodsIsInterned = true;
		}
    }

	/**
     * @param m2
     * @return
     */
    private static String getDetailedSignature(XMethod m2) {
	    return m2.getName()+m2.getSignature()+m2.isStatic();
    }

	public boolean isInterned(XMethod m) {
		return methods.containsKey(m);
	}

	private @CheckReturnValue @NonNull XMethod intern(XMethod m) {
		XMethod m2 = methods.get(m);
		if (m2 != null) return m2;

		methods.put(m.getMethodDescriptor(), m);
		return m;
	}

	private @CheckReturnValue @NonNull XField intern(XField f) {
		XField f2 = fields.get(f);
		if (f2 != null) return f2;

		fields.put(f.getFieldDescriptor(), f);
		return f;
	}
//	public  Set<XMethod> getMethods() {
//	return methodsView;
//	}

//	public  Set<XField> getFields() {
//	return fieldsView;
//	}

	public static String canonicalizeString(String s) {
		return ConstantUtf8.getCachedInstance(s).getBytes();
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param className the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(String className, Method method) {
		String methodName = method.getName();
		String methodSig = method.getSignature();
		int accessFlags = method.getAccessFlags();

		return createXMethod(className, methodName, methodSig, accessFlags);
	}

	/*
	 * Create a new, never-before-seen, XMethod object and intern it.
	 */
	private static XMethod createXMethod(@DottedClassName String className, String methodName, String methodSig, int accessFlags) {
		XFactory xFactory = AnalysisContext.currentXFactory();

		boolean isStatic = (accessFlags & Constants.ACC_STATIC) != 0;
		MethodDescriptor methodDescriptor =
			DescriptorFactory.instance().getMethodDescriptor(ClassName.toSlashedClassName(className), methodName, methodSig, isStatic);

		XMethod xmethod = xFactory.methods.get(methodDescriptor);
		if (xmethod == null) {
			// Find the class
			ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptorForDottedClassName(className);
			XClass xclass = xFactory.getXClass(classDescriptor);
			if (xclass != null) {
				// Find the method within the class
				xmethod = xclass.findMethod(methodName, methodSig, isStatic);
			}

			if (xmethod == null) {
				xmethod = new UnresolvedXMethod(
						className,
						methodName,
						methodSig,
						accessFlags);
			}

			// Intern the method
			xmethod = xFactory.intern(xmethod);
		}

		return xmethod;
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param javaClass the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(JavaClass javaClass , Method method) {
		if (method == null) throw new NullPointerException("method must not be null");
		XMethod xmethod = createXMethod(javaClass.getClassName(), method);
		assert xmethod.isResolved();
		return xmethod;
	}

	/**
	 * @param className
	 * @param methodName
	 * @param methodSig
	 * @param isStatic
	 * @return the created XMethod
	 */
	public  static XMethod createXMethod(@DottedClassName String className, String methodName, String methodSig, boolean isStatic) {
		XMethod m;
		XFactory xFactory = AnalysisContext.currentXFactory();
		m = createXMethod(className, methodName, methodSig, isStatic ? Constants.ACC_STATIC : 0);
		m = xFactory.resolve(m);
		return m;
	}

	public static XMethod createXMethod(MethodAnnotation ma) {
		return createXMethod(ma.getClassName(), ma.getMethodName(), ma.getMethodSignature(), ma.isStatic());
	}

	static class RecursionDepth {
		private static final int MAX_DEPTH = 50;
		private int depth = 0;
		ArrayList<Object> list = new ArrayList<Object>();
		@Override
		public String toString() {
			return list.toString();
		}
		public void dump() {
			System.out.println("Recursive calls" );
			for(Object o : list) 
				System.out.println("  resolve " + o);
		}
		public boolean enter(Object value) {
			if (depth > MAX_DEPTH) 
				return false;
			if (DEBUG_CIRCULARITY) list.add(value);
			depth++;
			return true;
		}
		public void exit() {
			depth--;
			if (DEBUG_CIRCULARITY) list.remove(list.size()-1);
			assert depth >= 0;
		}
	}
	static ThreadLocal<RecursionDepth> recursionDepth = new  ThreadLocal<RecursionDepth>() {
		@Override
		public RecursionDepth initialValue() {
			return new RecursionDepth();
		}
	};

	/**
	 * Create an XField object
	 * 
	 * @param className
	 * @param fieldName
	 * @param fieldSignature
	 * @param isStatic
	 * @return the created XField
	 */
	public static XField createXField(String className, String fieldName, String fieldSignature, boolean isStatic) {
		XFactory xFactory = AnalysisContext.currentXFactory();
		XField f = getExactXField(className, fieldName, fieldSignature, isStatic);
		f = xFactory.resolve(f);
		return f;
	}

	public final static boolean DEBUG_CIRCULARITY = SystemProperties.getBoolean("circularity.debug");
	/**
	 * @param f
	 * @return
	 */
	private @NonNull XField resolve(XField f) {
		if (f.isResolved()) return f;
		if (f.isStatic()) return f;
		if (f.getName().startsWith("this$")) return f;
		try {
			if (!recursionDepth.get().enter(f)) {
				fail("recursive cycle trying to resolve " + f, null, null);
				return f;
			}

			XField f2 = f;
			String classname = f.getClassName();
			try {
				JavaClass superClass = Repository.lookupClass(classname).getSuperClass();
				if (superClass == null) return f;

				if (classname.equals(superClass.getClassName())) return f;
				f2 = createXField(superClass.getClassName(), f.getName(), f.getSignature(), f.isStatic());
				f2 = intern(f2);
				if (f2.isResolved()) {
					fields.put(f.getFieldDescriptor(), f2);
					return f2;	
				}


			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
			return f;
		} finally {
			recursionDepth.get().exit();
		}
	}

	private static void fail(String s, @CheckForNull JavaClass jClass, @CheckForNull JavaClass superClass) {
		AnalysisContext.logError(s);
		if (DEBUG_CIRCULARITY) {
			System.out.println(s);
			recursionDepth.get().dump();

		}
		if (jClass != null)
			System.out.println(jClass);
		if (superClass != null)
			System.out.println(superClass);
		System.exit(1);
	}
	/**
	 * If a method is not marked as resolved, look in superclasses to see if the method can be found there.
	 * Return whatever method is found. 
	 * @param m
	 * @return
	 */
	private @NonNull XMethod resolve(XMethod m) {
		if (m.isResolved()) return m;
		// if (m.isStatic()) return m;
		try {
			if (!recursionDepth.get().enter(m)) {
				fail("recursive cycle trying to resolve " + m, null, null);
				return m;
			}

			String className = m.getClassName();

			String methodName = m.getName();
			if (className.charAt(0)=='[' || methodName.equals("<init>") || methodName.equals("<clinit>") || methodName.startsWith("access$")) {
				((AbstractMethod)m).markAsResolved();
				return m;
			}
			try {
				JavaClass javaClass = Repository.lookupClass(className);
				if (!javaClass.getClassName().equals(className)) {
					fail("Looked up " + className + ", got a class named " + javaClass.getClassName(), javaClass, null);
					return m;
				}
				JavaClass superClass = javaClass.getSuperClass();
				if (superClass == null) return m;
				String superClassName = superClass.getClassName();
				if (!javaClass.getSuperclassName().equals(superClassName))
					fail("requested superclass of " + className + ", expecting to get " + javaClass.getSuperclassName()  
							+ ", instead got " + superClassName, javaClass, superClass);
				if (superClass.getSuperclassName().equals(className)
						|| className.equals(superClassName)) {
					fail("superclass of  " + className + " is " + superClassName, javaClass, superClass);
					return m;
				}
				XMethod m2 = createXMethod(superClassName, methodName, m.getSignature(), m.isStatic());
				if (m2.isResolved()) {
					methods.put(m.getMethodDescriptor(), m2);
					return m2;
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
			// ((AbstractMethod)m).markAsResolved();
			return m;
		} finally {
			recursionDepth.get().exit();
		}

	}

	public static XField createXField(FieldInstruction fieldInstruction, ConstantPoolGen cpg) {
		String className = fieldInstruction.getClassName(cpg);
		String fieldName = fieldInstruction.getName(cpg);
		String fieldSig = fieldInstruction.getSignature(cpg);

		int opcode = fieldInstruction.getOpcode();
		return createXField(className, fieldName, fieldSig, opcode ==  Constants.GETSTATIC
				||  opcode ==  Constants.PUTSTATIC );
	}
	public static XField createReferencedXField(DismantleBytecode visitor) {
		return createXField(visitor.getDottedClassConstantOperand(),
				visitor.getNameConstantOperand(),
				visitor.getSigConstantOperand(),
				visitor.getRefFieldIsStatic());			
	}
	public static XMethod createReferencedXMethod(DismantleBytecode visitor) {
		return createXMethod(visitor.getDottedClassConstantOperand(),
				visitor.getNameConstantOperand(),
				visitor.getSigConstantOperand(),
				visitor.getOpcode() == Constants.INVOKESTATIC);			
	}

	public static XField createXField(FieldAnnotation f) {
		return createXField(f.getClassName(), f.getFieldName(), f.getFieldSignature(), f.isStatic());
	}

	public static XField createXField(JavaClass javaClass, Field field) {
		return createXField(javaClass.getClassName(), field);
	}
	/**
	 * Create an XField object from a BCEL Field.
	 * 
	 * @param className the name of the Java class containing the field
	 * @param field     the Field within the JavaClass
	 * @return the created XField
	 */
	public static XField createXField(String className, Field field) {
		String fieldName = field.getName();
		String fieldSig = field.getSignature();

		XField xfield = getExactXField(className, fieldName, fieldSig, field.isStatic());
		assert xfield.isResolved();
		return xfield;
	}

	/**
	 * Get an XField object exactly matching given class, name,
	 * and signature.  May return an unresolved object
	 * (if the class can't be found, or does not directly
	 * declare named field). 
	 * 
	 * @param className   name of class containing the field
	 * @param name        name of field
	 * @param signature   field signature
	 * @param isStatic field access flags
	 * @return XField exactly matching class name, field name, and field signature
	 */
	public static XField getExactXField(String className, String name, String signature, boolean isStatic) {
		XFactory xFactory = AnalysisContext.currentXFactory();

		FieldDescriptor fieldDesc = DescriptorFactory.instance().getFieldDescriptor(
				ClassName.toSlashedClassName(className),
				name,
				signature,
				isStatic);

		XField xfield = xFactory.fields.get(fieldDesc);
		if (xfield == null) {
			XClass xclass = xFactory.getXClass(fieldDesc.getClassDescriptor());
			if (xclass != null) {
				xfield = xclass.findField(name, signature, isStatic);
				if (xfield != null) {
					xfield = xFactory.intern(xfield);
				}
			}
		}

		if (xfield == null) {
			xfield = FieldInfo.createUnresolvedFieldInfo(ClassName.toSlashedClassName(className), name, signature, isStatic);
			xfield = xFactory.intern(xfield);
		}

		assert xfield != null;
		assert xFactory.fields.containsKey(xfield.getFieldDescriptor());

		return xfield;
	}

	/**
	 * Create an XMethod object from an InvokeInstruction.
	 * 
	 * @param invokeInstruction the InvokeInstruction
	 * @param cpg               ConstantPoolGen from the class containing the instruction
	 * @return XMethod representing the method called by the InvokeInstruction
	 */
	public static XMethod createXMethod(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) {
		String className = invokeInstruction.getClassName(cpg);
		String methodName = invokeInstruction.getName(cpg);
		String methodSig = invokeInstruction.getSignature(cpg);

		return createXMethod(className, methodName, methodSig, invokeInstruction.getOpcode() == Constants.INVOKESTATIC);
	}

	/**
	 * Create an XMethod object from the method currently being visited by
	 * the given PreorderVisitor.
	 * 
	 * @param visitor the PreorderVisitor
	 * @return the XMethod representing the method currently being visited
	 */
	public static XMethod createXMethod(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Method method = visitor.getMethod();
		XMethod m =  createXMethod(javaClass, method);
		return m;
	}

	/**
	 * Create an XField object from the field currently being visited by
	 * the given PreorderVisitor.
	 * 
	 * @param visitor the PreorderVisitor
	 * @return the XField representing the method currently being visited
	 */
	public static XField createXField(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Field field = visitor.getField();
		XField f =  createXField(javaClass, field);
		return f;
	}

	public static XMethod createXMethod(MethodGen methodGen) {
		String className = methodGen.getClassName();
		String methodName = methodGen.getName();
		String methodSig = methodGen.getSignature();
		int accessFlags = methodGen.getAccessFlags();
		return createXMethod(className, methodName, methodSig, accessFlags);


	}

	/**
	 * @param m
	 */
	public void deprecate(ClassMember m) {
		deprecated.add(m);

	}
	public Set<? extends ClassMember> getDeprecated() {
		return deprecatedView;
	}


	public static XMethod createXMethod(JavaClassAndMethod classAndMethod) {
		return createXMethod(classAndMethod.getJavaClass(), classAndMethod.getMethod());
	}

	/**
	 * Get the XClass object providing information about the
	 * class named by the given ClassDescriptor.
	 * 
	 * @param classDescriptor a ClassDescriptor
	 * @return an XClass object providing information about the class,
	 *         or null if the class cannot be found 
	 */
	public XClass getXClass(ClassDescriptor classDescriptor) {
		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			return analysisCache.getClassAnalysis(XClass.class, classDescriptor);
		} catch (CheckedAnalysisException e) {
			return null;
		}
	}

	/**
	 * Compare XMethod or XField object objects.
	 * <em>All methods that implement XMethod or XField should
	 * delegate to this method when implementing compareTo(Object)
	 * if the right-hand object implements XField or XMethod.</em>
	 * 
	 * @param lhs an XMethod or XField
	 * @param rhs an XMethod or XField
	 * @return comparison of lhs and rhs 
	 */
	public static<E extends ClassMember> int compare(E lhs, E rhs) {
		int cmp;

		cmp = lhs.getClassName().compareTo(rhs.getClassName());
		if (cmp != 0) {
			return cmp;
		}

		cmp = lhs.getName().compareTo(rhs.getName());
		if (cmp != 0) {
			return cmp;
		}

		cmp = lhs.getSignature().compareTo(rhs.getSignature());
		if (cmp != 0) {
			return cmp;
		}

		return (lhs.isStatic() ? 1 : 0) - (rhs.isStatic() ? 1 : 0);
	}

}
