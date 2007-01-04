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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
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
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Factory methods for creating XMethod objects.
 * 
 * @author David Hovemeyer
 */
public  class XFactory {
	
	public XFactory() {};

	private  Map<XMethod,XMethod> methods = new HashMap<XMethod,XMethod>();

	private Set<ClassMember> deprecated = new HashSet<ClassMember>();
	private Set<? extends ClassMember> deprecatedView = Collections.unmodifiableSet(deprecated);
	private  Map<XField,XField> fields = new HashMap<XField,XField>();

	private  Set<XMethod> methodsView = Collections
			.unmodifiableSet(methods.keySet());

	private  Set<XField> fieldsView = Collections
			.unmodifiableSet(fields.keySet());

	private  Set<XMethod> calledMethods = new HashSet<XMethod>();
	private boolean calledMethodsIsInterned = false;

	public void addCalledMethod(XMethod m) {
		if (calledMethods.add(m) && !m.isResolved())
			calledMethodsIsInterned = false;
	}
	
	public boolean isCalled(XMethod m) {
		if (!calledMethodsIsInterned) {
			Set<XMethod> tmp = new HashSet<XMethod>();
			for(XMethod m2 : calledMethods)
				tmp.add(intern(m2));
			calledMethodsIsInterned = true;
		}
		return calledMethods.contains(m);
	}
	
    public boolean isInterned(XMethod m) {
        return methods.containsKey(m);
    }

	public @CheckReturnValue @NonNull XMethod intern(XMethod m) {
		XMethod m2 = methods.get(m);
		if (m2 != null) return m2;
	
		methods.put(m,m);
		return m;
	}

	public @CheckReturnValue @NonNull XField intern(XField f) {
		XField f2 = fields.get(f);
		if (f2 != null) return f2;

		fields.put(f,f);
		return f;
	}
	public  Set<XMethod> getMethods() {
		return methodsView;
	}

	public  Set<XField> getFields() {
		return fieldsView;
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param className the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(String className, Method method) {
		XFactory xFactory = AnalysisContext.currentXFactory();
		String methodName = method.getName();
		String methodSig = method.getSignature();
		int accessFlags = method.getAccessFlags();
		boolean isStatic = method.isStatic();
		XMethod m;
		if (isStatic)
			m = new StaticMethod(className, methodName, methodSig, accessFlags);
		else 
			m = new InstanceMethod(className, methodName, methodSig, accessFlags);
		
		XMethod m2 = xFactory.intern(m);
		// MUSTFIX: Check this
		// assert m2.getAccessFlags() == m.getAccessFlags();
		((AbstractMethod) m2).markAsResolved();
		return m2;
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param javaClass the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(JavaClass javaClass , Method method) {
		return createXMethod(javaClass.getClassName(), method);
	}
	/**
	 * @param className
	 * @param methodName
	 * @param methodSig
	 * @param isStatic
	 * @return the created XMethod
	 */
	public  static XMethod createXMethod(String className, String methodName, String methodSig, boolean isStatic) {
		XMethod m;
		if (isStatic)
			m = new StaticMethod(className, methodName, methodSig, Constants.ACC_STATIC);
		else
			m = new InstanceMethod(className, methodName, methodSig, 0);
		XFactory xFactory = AnalysisContext.currentXFactory();
		m = xFactory.intern(m);
		m = xFactory.resolve(m);
		return m;
	}

	public static XMethod createXMethod(MethodAnnotation ma) {
		return createXMethod(ma.getClassName(), ma.getMethodName(), ma.getMethodSignature(), ma.isStatic());
	}

	static class RecursionDepth {
       private static final int MAX_DEPTH = 40;
     private int depth = 0;
     ArrayList<Object> list = new ArrayList<Object>();
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
		XFactory xFactory =AnalysisContext.currentXFactory();
		XField f;
		
		if (isStatic) {
			int accessFlags = 0;
			if (fieldName.toUpperCase().equals(fieldName))
				accessFlags = Constants.ACC_FINAL;
			f = new StaticField(className, fieldName, fieldSignature, accessFlags);
		}
		else {
			int accessFlags = 0;
			if (fieldName.startsWith("this$")) accessFlags = Constants.ACC_FINAL;
			f = new InstanceField(className, fieldName, fieldSignature, accessFlags);
		}
		f = xFactory.intern(f);
		f = xFactory.resolve(f);
		return f;
	}
	
    public static boolean DEBUG_CIRCULARITY = SystemProperties.getBoolean("circularity.debug");
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
                fail("recursive cycle trying to resolve " + f, null);
                return f;
            }

            XField f2 = f;
            String classname = f.getClassName();
            try {
                JavaClass javaClass = Repository.lookupClass(classname);
                javaClass = javaClass.getSuperClass();
                if (javaClass == null) return f;
                if (f.getClassName().equals(javaClass.getClassName())) return f;
                f2 = createXField(javaClass.getClassName(), f.getName(), f.getSignature(), f.isStatic());
                f2 = intern(f2);
                if (f2.isResolved()) {
                    fields.put(f, f2);
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

    private static void fail(String s, JavaClass jClass) {
        if (DEBUG_CIRCULARITY) {
            System.out.println(s);
            recursionDepth.get().dump();
            if (jClass != null)
                System.out.println(jClass);
            System.exit(1);
        }
        AnalysisContext.logError(s);
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
	            fail("recursive cycle trying to resolve " + m, null);
	            return m;
	        }

	        String classname = m.getClassName();

	        String methodName = m.getName();
            if (classname.charAt(0)=='[' || methodName.equals("<init>") || methodName.equals("<clinit>") || methodName.startsWith("access$")) {
	            ((AbstractMethod)m).markAsResolved();
	            return m;
	        }
	        try {
	            JavaClass javaClass = Repository.lookupClass(classname);
                if (!javaClass.getClassName().equals(classname)) {
                    fail("Looked up " + classname + ", got a class named " + javaClass.getClassName(), javaClass);
                    return m;
                }
                JavaClass superClass = javaClass.getSuperClass();
	            if (superClass == null) return m;
	            String superClassName = superClass.getClassName();
                if (!javaClass.getSuperclassName().equals(superClassName))
                    fail("requested superclass of " + classname + ", expecting to get " + javaClass.getSuperclassName()  
                            + ", instead got " + superClassName, javaClass);
                if (classname.equals("java.security.MessageDigest") && !superClassName.equals("java.security.MessageDigestSpi")
                        || classname.equals("java.security.MessageDigestSpi") && !superClassName.equals("java.lang.Object")) {
                    fail("superclass of  " + classname + " is " + superClassName, javaClass);
                    return m;
                }
                if (classname.equals(superClassName)) return m;
                XMethod m2 = createXMethod(superClassName, methodName, m.getSignature(), m.isStatic());
	            if (m2.isResolved()) {
	                methods.put(m, m2);
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
		int accessFlags = field.getAccessFlags();
		XFactory xFactory = AnalysisContext.currentXFactory();
		XField f;
		if (field.isStatic())
			f = new StaticField(className, fieldName, fieldSig, accessFlags);
		else
			f = new InstanceField(className, fieldName, fieldSig, accessFlags);
		XField f2 = xFactory.intern(f);
		// MUSTFIX: investigate
		// assert f.getAccessFlags() == f2.getAccessFlags();
		((AbstractField) f2).markAsResolved();
		return f2;
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
		return createXMethod(methodGen.getClassName(), methodGen.getMethod());
		
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

}
