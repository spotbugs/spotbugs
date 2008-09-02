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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Factory methods for creating XMethod objects.
 * 
 * @author David Hovemeyer
 */
public class XFactory {
	public static final boolean DEBUG_UNRESOLVED = SystemProperties.getBoolean("findbugs.xfactory.debugunresolved");

	private Set<ClassDescriptor> reflectiveClasses = new HashSet<ClassDescriptor>();
	private Map<MethodDescriptor, XMethod> methods = new HashMap<MethodDescriptor, XMethod>();

	private Map<FieldDescriptor, XField> fields = new HashMap<FieldDescriptor, XField>();

	private Set<XMethod> calledMethods = new HashSet<XMethod>();
	private Set<XField> emptyArrays = new HashSet<XField>();

	private Set<String> calledMethodSignatures = new HashSet<String>();


	public void canonicalizeAll() {
		DescriptorFactory descriptorFactory = DescriptorFactory.instance();
		for(XMethod m : methods.values())
			if (m instanceof MethodDescriptor) {
				descriptorFactory.canonicalize((MethodDescriptor)m);
			}
		for(XField f : fields.values())
			if (f instanceof FieldDescriptor)
				descriptorFactory.canonicalize((FieldDescriptor)f);
	}
	/**
	 * Constructor.
	 */
	public XFactory() {
	}

	public void intern(XClass c) {
		for (XMethod m : c.getXMethods()) {
			MethodInfo mi = (MethodInfo) m;
			methods.put(mi, mi);
		}
		for (XField f : c.getXFields()) {
			FieldInfo fi = (FieldInfo) f;
			fields.put(fi, fi);
		}
	}

	public Collection<XField> allFields() {
		return fields.values();
	}
	public void addCalledMethod(MethodDescriptor m) {
		assert m.getClassDescriptor().getClassName().indexOf('.') == -1;
		calledMethods.add(createXMethod(m));
	}
	public void addEmptyArrayField(XField f) {
		emptyArrays.add(f);
	}
	public boolean isEmptyArrayField(@CheckForNull XField f) {
		return emptyArrays.contains(f);
	}
	public boolean isCalled(XMethod m) {
		if (m.getName().equals("<clinit>"))
			return true;
		return calledMethods.contains(m);
	}

	public Set<XMethod> getCalledMethods() {
		return calledMethods;
	}

	public Set<ClassDescriptor> getReflectiveClasses() {
		return reflectiveClasses;
	}
	public boolean isReflectiveClass(ClassDescriptor c) {
		return reflectiveClasses.contains(c);
	}
	public boolean addReflectiveClasses(ClassDescriptor c) {
		return reflectiveClasses.add(c);
	}
	public boolean isCalledDirectlyOrIndirectly(XMethod m) {
		if (isCalled(m))
			return true;
		if (m.isStatic() || m.isPrivate() || m.getName().equals("<init>"))
			return false;
		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			XClass clazz = analysisCache.getClassAnalysis(XClass.class, m.getClassDescriptor());
			if (isCalledDirectlyOrIndirectly(clazz.getSuperclassDescriptor(), m))
				return true;
			for (ClassDescriptor i : clazz.getInterfaceDescriptorList())
				if (isCalledDirectlyOrIndirectly(i, m))
					return true;

			return false;
		} catch (edu.umd.cs.findbugs.classfile.MissingClassException e) {
			// AnalysisContext.reportMissingClass(e.getClassNotFoundException());
			return false;
		} catch (MissingClassException e) {
			AnalysisContext.reportMissingClass(e.getClassNotFoundException());
			return false;
		} catch (Exception e) {
			AnalysisContext.logError("Error checking to see if " + m + " is called (" + e.getClass().getCanonicalName() + ")", e);
			return false;
		}
	}

	/**
	 * @param superclassDescriptor
	 * @param m
	 * @return
	 * @throws CheckedAnalysisException
	 */
	private boolean isCalledDirectlyOrIndirectly(@CheckForNull
	ClassDescriptor clazzDescriptor, XMethod m) throws CheckedAnalysisException {
		if (clazzDescriptor == null)
			return false;
		IAnalysisCache analysisCache = Global.getAnalysisCache();
		XClass clazz = analysisCache.getClassAnalysis(XClass.class, clazzDescriptor);
		XMethod m2 = clazz.findMethod(m.getName(), m.getSignature(), m.isStatic());
		if (m2 != null && isCalled(m2))
			return true;
		if (isCalledDirectlyOrIndirectly(clazz.getSuperclassDescriptor(), m))
			return true;
		for (ClassDescriptor i : clazz.getInterfaceDescriptorList())
			if (isCalledDirectlyOrIndirectly(i, m))
				return true;

		return false;

	}

	public boolean nameAndSignatureIsCalled(XMethod m) {
		return calledMethodSignatures.contains(getDetailedSignature(m));
	}

	/**
	 * @param m2
	 * @return
	 */
	private static String getDetailedSignature(XMethod m2) {
		return m2.getName() + m2.getSignature() + m2.isStatic();
	}

	@Deprecated
	public boolean isInterned(XMethod m) {
		return m.isResolved();
	}

	public static String canonicalizeString(String s) {
		return DescriptorFactory.canonicalizeString(s);
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param className
	 *            the class to which the Method belongs
	 * @param method
	 *            the Method
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
	private static XMethod createXMethod(@DottedClassName
	String className, String methodName, String methodSig, int accessFlags) {
		return createXMethod(className, methodName, methodSig, (accessFlags & Constants.ACC_STATIC) != 0);
	}

	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param javaClass
	 *            the class to which the Method belongs
	 * @param method
	 *            the Method
	 * @return an XMethod representing the Method
	 */

	public static XMethod createXMethod(JavaClass javaClass, Method method) {
		if (method == null)
			throw new NullPointerException("method must not be null");
		XMethod xmethod = createXMethod(javaClass.getClassName(), method);
		assert xmethod.isResolved();
		return xmethod;
	}

	public static void assertDottedClassName(@DottedClassName
			String className) {
		assert className.indexOf('/') == -1;
	}
	/**
	 * @param className
	 * @param methodName
	 * @param methodSig
	 * @param isStatic
	 * @return the created XMethod
	 */

	public static XMethod createXMethod(@DottedClassName
	String className, String methodName, String methodSig, boolean isStatic) {
		assertDottedClassName(className);
		MethodDescriptor desc = DescriptorFactory.instance().getMethodDescriptor(ClassName.toSlashedClassName(className),
		        methodName, methodSig, isStatic);
		return createXMethod(desc);
	}

	public static XMethod createXMethod(MethodDescriptor desc) {
		XFactory xFactory = AnalysisContext.currentXFactory();

		XMethod m = xFactory.methods.get(desc);
		if (m != null)
			return m;
		m = xFactory.resolveXMethod(desc);
		if (m instanceof MethodDescriptor)  {
		  xFactory.methods.put((MethodDescriptor) m, m);
		  DescriptorFactory.instance().canonicalize((MethodDescriptor) m);
		} else 
			xFactory.methods.put(desc, m);
		return m;
	}
	
	public static void profile() {
		XFactory xFactory = AnalysisContext.currentXFactory();
		int count = 0;
		for(XMethod m : xFactory.methods.values()) {
			if (m instanceof MethodInfo)
				count++;
		}
		System.out.printf("XFactory cached methods: %d/%d\n", count, xFactory.methods.size());
		DescriptorFactory.instance().profile();
		
	}

	private XMethod resolveXMethod(MethodDescriptor originalDescriptor) {
		MethodDescriptor desc = originalDescriptor;
		try {
			while (true) {
				XMethod m = methods.get(desc);
				if (m != null)
					return m;
				XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc.getClassDescriptor());
				if (xClass == null)
					break;
				ClassDescriptor superClass = xClass.getSuperclassDescriptor();
				if (superClass == null)
					break;
				desc = DescriptorFactory.instance().getMethodDescriptor(superClass.getClassName(), desc.getName(),
				        desc.getSignature(), desc.isStatic());
			}
		} catch (CheckedAnalysisException e) {
			assert true;
		} catch (RuntimeException e) {
			assert true;
		}
		return new UnresolvedXMethod(originalDescriptor);
	}

	public static XMethod createXMethod(MethodAnnotation ma) {
		return createXMethod(ma.getClassName(), ma.getMethodName(), ma.getMethodSignature(), ma.isStatic());
	}

	/**
	 * Create an XField object
	 * 
	 * @param className
	 * @param fieldName
	 * @param fieldSignature
	 * @param isStatic
	 * @return the created XField
	 */
	public static 
	XField createXField(String className, String fieldName, String fieldSignature, boolean isStatic) {
		FieldDescriptor fieldDesc = DescriptorFactory.instance().getFieldDescriptor(ClassName.toSlashedClassName(className),
				fieldName, fieldSignature, isStatic);
		
		return  createXField(fieldDesc);
	}

	public final static boolean DEBUG_CIRCULARITY = SystemProperties.getBoolean("circularity.debug");

	public static XField createXField(FieldInstruction fieldInstruction, ConstantPoolGen cpg) {
		String className = fieldInstruction.getClassName(cpg);
		String fieldName = fieldInstruction.getName(cpg);
		String fieldSig = fieldInstruction.getSignature(cpg);

		int opcode = fieldInstruction.getOpcode();
		return createXField(className, fieldName, fieldSig, opcode == Constants.GETSTATIC || opcode == Constants.PUTSTATIC);
	}

	public static XField createReferencedXField(DismantleBytecode visitor) {
		int seen = visitor.getOpcode();
		if (seen != Opcodes.GETFIELD &&  seen != Opcodes.GETSTATIC && seen != Opcodes.PUTFIELD && seen != Opcodes.PUTSTATIC)
			throw new IllegalArgumentException("Not at a field reference");
		return createXField(visitor.getDottedClassConstantOperand(), visitor.getNameConstantOperand(), visitor
		        .getSigConstantOperand(), visitor.getRefFieldIsStatic());
	}

	public static XMethod createReferencedXMethod(DismantleBytecode visitor) {
		return createXMethod(visitor.getDottedClassConstantOperand(), visitor.getNameConstantOperand(), visitor
		        .getSigConstantOperand(), visitor.getOpcode() == Constants.INVOKESTATIC);
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
	 * @param className
	 *            the name of the Java class containing the field
	 * @param field
	 *            the Field within the JavaClass
	 * @return the created XField
	 */
	public static XField createXField(String className, Field field) {
		String fieldName = field.getName();
		String fieldSig = field.getSignature();

		XField xfield = getExactXField(className, fieldName, fieldSig, field.isStatic());
		assert xfield.isResolved() : "Could not exactly resolve " + xfield;
		return xfield;
	}

	/**
	 * Get an XField object exactly matching given class, name, and signature.
	 * May return an unresolved object (if the class can't be found, or does not
	 * directly declare named field).
	 * 
	 * @param className
	 *            name of class containing the field
	 * @param name
	 *            name of field
	 * @param signature
	 *            field signature
	 * @param isStatic
	 *            field access flags
	 * @return XField exactly matching class name, field name, and field
	 *         signature
	 */
	public static XField getExactXField(@SlashedClassName String className, String name, String signature, boolean isStatic) {
		FieldDescriptor fieldDesc = DescriptorFactory.instance().getFieldDescriptor(ClassName.toSlashedClassName(className),
		        name, signature, isStatic);
		return getExactXField(fieldDesc);
	}

	public static @Nonnull XField getExactXField(@SlashedClassName String className, Field f) {
		FieldDescriptor fd = DescriptorFactory.instance().getFieldDescriptor(className, f);
		return getExactXField(fd);
	}
	public static @Nonnull XField getExactXField(FieldDescriptor desc) {
		XFactory xFactory = AnalysisContext.currentXFactory();

		XField f = xFactory.fields.get(desc);
		if (f == null) return new UnresolvedXField(desc);
		return f;
	}
	public static XField createXField(FieldDescriptor desc) {
		XFactory xFactory = AnalysisContext.currentXFactory();

		XField m = xFactory.fields.get(desc);
		if (m != null)
			return m;
		m = xFactory.resolveXField(desc);
		xFactory.fields.put(desc, m);
		return m;
	}
	private XField resolveXField(FieldDescriptor originalDescriptor) {
		FieldDescriptor desc = originalDescriptor;
		try {
			while (true) {
				XField m = fields.get(desc);
				if (m != null)
					return m;
				XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc.getClassDescriptor());
				if (xClass == null)
					break;
				ClassDescriptor superClass = xClass.getSuperclassDescriptor();
				if (superClass == null)
					break;
				desc = DescriptorFactory.instance().getFieldDescriptor(superClass.getClassName(), desc.getName(),
				        desc.getSignature(), desc.isStatic());
			}
		} catch (CheckedAnalysisException e) {
			assert true;
		}
		return new UnresolvedXField(originalDescriptor);
	}

	/**
	 * Create an XMethod object from an InvokeInstruction.
	 * 
	 * @param invokeInstruction
	 *            the InvokeInstruction
	 * @param cpg
	 *            ConstantPoolGen from the class containing the instruction
	 * @return XMethod representing the method called by the InvokeInstruction
	 */
	public static XMethod createXMethod(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) {
		String className = invokeInstruction.getClassName(cpg);
		String methodName = invokeInstruction.getName(cpg);
		String methodSig = invokeInstruction.getSignature(cpg);

		return createXMethod(className, methodName, methodSig, invokeInstruction.getOpcode() == Constants.INVOKESTATIC);
	}

	/**
	 * Create an XMethod object from the method currently being visited by the
	 * given PreorderVisitor.
	 * 
	 * @param visitor
	 *            the PreorderVisitor
	 * @return the XMethod representing the method currently being visited
	 */
	public static XMethod createXMethod(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Method method = visitor.getMethod();
		XMethod m = createXMethod(javaClass, method);
		return m;
	}

	/**
	 * Create an XField object from the field currently being visited by the
	 * given PreorderVisitor.
	 * 
	 * @param visitor
	 *            the PreorderVisitor
	 * @return the XField representing the method currently being visited
	 */
	public static XField createXField(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Field field = visitor.getField();
		XField f = createXField(javaClass, field);
		return f;
	}

	public static XMethod createXMethod(MethodGen methodGen) {
		String className = methodGen.getClassName();
		String methodName = methodGen.getName();
		String methodSig = methodGen.getSignature();
		int accessFlags = methodGen.getAccessFlags();
		return createXMethod(className, methodName, methodSig, accessFlags);
	}

	public static XMethod createXMethod(JavaClassAndMethod classAndMethod) {
		return createXMethod(classAndMethod.getJavaClass(), classAndMethod.getMethod());
	}

	/**
	 * Get the XClass object providing information about the class named by the
	 * given ClassDescriptor.
	 * 
	 * @param classDescriptor
	 *            a ClassDescriptor
	 * @return an XClass object providing information about the class, or null
	 *         if the class cannot be found
	 */
	public @CheckForNull XClass getXClass(ClassDescriptor classDescriptor) {
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
	 * @param lhs
	 *            an XMethod or XField
	 * @param rhs
	 *            an XMethod or XField
	 * @return comparison of lhs and rhs
	 */
	public static <E extends ClassMember> int compare(E lhs, E rhs) {
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
