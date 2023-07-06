/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.visitclass;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationDefault;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantModule;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPackage;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.EnclosingMethod;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.MethodParameters;
import org.apache.bcel.classfile.ParameterAnnotationEntry;
import org.apache.bcel.classfile.ParameterAnnotations;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.FieldOrMethodDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * <p>Interface to make the use of a visitor pattern programming style possible.
 * I.e. a class that implements this interface can traverse the contents of a
 * Java class just by calling the `accept' method which all classes have.
 * </p>
 * <p>Implemented by wish of <A HREF="http://www.inf.fu-berlin.de/~bokowski">Boris
 * Bokowski</A>.</p>
 * <p>
 * If don't like it, blame him. If you do like it thank me 8-)
 * </p>
 * @author <A HREF="http://www.inf.fu-berlin.de/~dahm">M. Dahm</A>
 * @version 970819
 */
public class PreorderVisitor extends BetterVisitor {

    // Available when visiting a class
    private ConstantPool constantPool;

    private JavaClass thisClass;

    private ClassInfo thisClassInfo;

    private MethodInfo thisMethodInfo;

    private FieldInfo thisFieldInfo;

    private String className = "none";

    private String dottedClassName = "none";

    private String packageName = "none";

    private String sourceFile = "none";

    private String superclassName = "none";

    private String dottedSuperclassName = "none";

    // Available when visiting a method
    private boolean visitingMethod = false;

    private String methodSig = "none";

    private String dottedMethodSig = "none";

    private Method method = null;

    private String methodName = "none";

    private String fullyQualifiedMethodName = "none";

    // Available when visiting a field
    private Field field;

    private boolean visitingField = false;

    private String fullyQualifiedFieldName = "none";

    private String fieldName = "none";

    private String fieldSig = "none";

    private String dottedFieldSig = "none";

    private boolean fieldIsStatic;

    // Available when visiting a Code
    private Code code;

    protected String getStringFromIndex(int i) {
        ConstantUtf8 name = (ConstantUtf8) constantPool.getConstant(i);
        return name.getBytes();
    }

    protected int asUnsignedByte(byte b) {
        return 0xff & b;
    }

    /**
     * Return the current Code attribute; assuming one is being visited
     *
     * @return current code attribute
     */
    public Code getCode() {
        if (code == null) {
            throw new IllegalStateException("Not visiting Code");
        }
        return code;
    }

    public Set<String> getSurroundingCaughtExceptions(int pc) {
        return getSurroundingCaughtExceptions(pc, Integer.MAX_VALUE);
    }

    public Set<String> getSurroundingCaughtExceptions(int pc, int maxTryBlockSize) {
        return getSurroundingCaughtExceptionTypes(pc, maxTryBlockSize).stream()
                .map(ex -> "C" + ex)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getSurroundingCaughtExceptionTypes(int pc, int maxTryBlockSize) {
        HashSet<Integer> result = new HashSet<>();
        if (code == null) {
            throw new IllegalStateException("Not visiting Code");
        }
        int size = maxTryBlockSize;
        if (code.getExceptionTable() == null) {
            return result;
        }
        for (CodeException catchBlock : code.getExceptionTable()) {
            int startPC = catchBlock.getStartPC();
            int endPC = catchBlock.getEndPC();
            if (pc >= startPC && pc <= endPC) {
                int thisSize = endPC - startPC;
                if (size > thisSize) {
                    result.clear();
                    size = thisSize;
                    result.add(catchBlock.getCatchType());
                } else if (size == thisSize) {
                    result.add(catchBlock.getCatchType());
                }
            }
        }
        return result;
    }

    /**
     * Get lines of code in try block that surround pc
     *
     * @param pc
     * @return number of lines of code in try block
     */
    public int getSizeOfSurroundingTryBlock(int pc) {
        return getSizeOfSurroundingTryBlock(null, pc);
    }

    /**
     * Get lines of code in try block that surround pc
     *
     * @param pc
     * @return number of lines of code in try block
     */
    public int getSizeOfSurroundingTryBlock(String vmNameOfExceptionClass, int pc) {
        if (code == null) {
            throw new IllegalStateException("Not visiting Code");
        }
        return Util.getSizeOfSurroundingTryBlock(constantPool, code, vmNameOfExceptionClass, pc);
    }

    public CodeException getSurroundingTryBlock(int pc) {
        return getSurroundingTryBlock(null, pc);
    }

    public CodeException getSurroundingTryBlock(String vmNameOfExceptionClass, int pc) {
        if (code == null) {
            throw new IllegalStateException("Not visiting Code");
        }
        return Util.getSurroundingTryBlock(constantPool, code, vmNameOfExceptionClass, pc);
    }

    // Attributes
    @Override
    public void visitCode(Code obj) {
        code = obj;
        super.visitCode(obj);
        CodeException[] exceptions = obj.getExceptionTable();
        for (CodeException exception : exceptions) {
            exception.accept(this);
        }
        Attribute[] attributes = obj.getAttributes();
        for (Attribute attribute : attributes) {
            attribute.accept(this);
        }
        visitAfter(obj);
        code = null;
    }

    /**
     * Called after visiting a code attribute
     *
     * @param obj
     *            Code that was just visited
     */
    public void visitAfter(Code obj) {
    }

    // Constants
    @Override
    public void visitConstantPool(ConstantPool obj) {
        super.visitConstantPool(obj);
        Constant[] constant_pool = obj.getConstantPool();
        for (int i = 1; i < constant_pool.length; i++) {
            constant_pool[i].accept(this);
            byte tag = constant_pool[i].getTag();
            if ((tag == Const.CONSTANT_Double) || (tag == Const.CONSTANT_Long)) {
                i++;
            }
        }
    }

    private void doVisitField(Field field) {
        if (visitingField) {
            throw new IllegalStateException("visitField called when already visiting a field");
        }
        visitingField = true;
        this.field = field;
        try {
            fieldName = fieldSig = dottedFieldSig = fullyQualifiedFieldName = null;
            thisFieldInfo = (FieldInfo) thisClassInfo.findField(getFieldName(), getFieldSig(), field.isStatic());
            assert thisFieldInfo != null : "Can't get field info for " + getFullyQualifiedFieldName();
            fieldIsStatic = field.isStatic();
            field.accept(this);
            Attribute[] attributes = field.getAttributes();
            for (Attribute attribute : attributes) {
                attribute.accept(this);
            }
        } finally {
            visitingField = false;
            this.field = null;
            this.thisFieldInfo = null;
        }
    }

    public void doVisitMethod(Method method) {
        if (visitingMethod) {
            throw new IllegalStateException("doVisitMethod called when already visiting a method");
        }
        visitingMethod = true;
        try {
            this.method = method;
            methodName = methodSig = dottedMethodSig = fullyQualifiedMethodName = null;
            thisMethodInfo = (MethodInfo) thisClassInfo.findMethod(getMethodName(), getMethodSig(), method.isStatic());
            assert thisMethodInfo != null : "Can't get method info for " + getFullyQualifiedMethodName();
            this.method.accept(this);
            Attribute[] attributes = method.getAttributes();
            for (Attribute attribute : attributes) {
                attribute.accept(this);
            }
        } finally {
            visitingMethod = false;
            this.method = null;
            this.thisMethodInfo = null;
        }
    }

    public boolean amVisitingMainMethod() {
        if (!visitingMethod) {
            throw new IllegalStateException("Not visiting a method");
        }
        return method.isStatic()
                && "main".equals(getMethodName())
                && "([Ljava/lang/String;)V".equals(getMethodSig());

    }

    // Extra classes (i.e. leaves in this context)
    @Override
    public void visitInnerClasses(InnerClasses obj) {
        super.visitInnerClasses(obj);
        InnerClass[] inner_classes = obj.getInnerClasses();
        for (InnerClass inner_class : inner_classes) {
            inner_class.accept(this);
        }
    }

    public void visitAfter(JavaClass obj) {
    }

    public boolean shouldVisit(JavaClass obj) {
        return true;
    }

    boolean visitMethodsInCallOrder;


    protected boolean isVisitMethodsInCallOrder() {
        return visitMethodsInCallOrder;
    }

    protected void setVisitMethodsInCallOrder(boolean visitMethodsInCallOrder) {
        this.visitMethodsInCallOrder = visitMethodsInCallOrder;
    }

    protected Iterable<Method> getMethodVisitOrder(JavaClass obj) {
        return Arrays.asList(obj.getMethods());
    }

    // General classes
    @Override
    public void visitJavaClass(JavaClass obj) {
        setupVisitorForClass(obj);
        if (shouldVisit(obj)) {
            constantPool.accept(this);
            Field[] fields = obj.getFields();
            Attribute[] attributes = obj.getAttributes();
            for (Field field : fields) {
                doVisitField(field);
            }
            boolean didInCallOrder = false;

            if (visitMethodsInCallOrder) {
                try {
                    IAnalysisCache analysisCache = Global.getAnalysisCache();

                    ClassDescriptor c = DescriptorFactory.createClassDescriptor(obj);

                    ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, c);
                    didInCallOrder = true;
                    for (Method m : classContext.getMethodsInCallOrder()) {
                        doVisitMethod(m);
                    }

                } catch (CheckedAnalysisException e) {
                    AnalysisContext.logError("Error trying to visit methods in order", e);
                }
            }
            if (!didInCallOrder) {
                for (Method m : getMethodVisitOrder(obj)) {
                    doVisitMethod(m);
                }
            }
            for (Attribute attribute : attributes) {
                attribute.accept(this);
            }
            visitAfter(obj);
        }
    }

    public void setupVisitorForClass(JavaClass obj) {
        constantPool = obj.getConstantPool();
        thisClass = obj;
        ConstantClass c = (ConstantClass) constantPool.getConstant(obj.getClassNameIndex());
        className = getStringFromIndex(c.getNameIndex());
        dottedClassName = className.replace('/', '.');
        packageName = obj.getPackageName();
        sourceFile = obj.getSourceFileName();
        dottedSuperclassName = obj.getSuperclassName();
        superclassName = dottedSuperclassName.replace('.', '/');

        ClassDescriptor cDesc = DescriptorFactory.createClassDescriptor(className);
        if (!FindBugs.isNoAnalysis()) {
            try {
                thisClassInfo = (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class, cDesc);
            } catch (CheckedAnalysisException e) {
                throw new AssertionError("Can't find ClassInfo for " + cDesc);
            }
        }

        super.visitJavaClass(obj);
    }

    @Override
    public void visitLineNumberTable(LineNumberTable obj) {
        super.visitLineNumberTable(obj);
        LineNumber[] line_number_table = obj.getLineNumberTable();
        for (LineNumber aLine_number_table : line_number_table) {
            aLine_number_table.accept(this);
        }
    }

    @Override
    public void visitLocalVariableTable(LocalVariableTable obj) {
        super.visitLocalVariableTable(obj);
        LocalVariable[] local_variable_table = obj.getLocalVariableTable();
        for (LocalVariable aLocal_variable_table : local_variable_table) {
            aLocal_variable_table.accept(this);
        }
    }

    // Accessors

    public XClass getXClass() {
        if (thisClassInfo == null) {
            throw new AssertionError("XClass information not set");
        }
        return thisClassInfo;
    }

    public ClassDescriptor getClassDescriptor() {
        return thisClassInfo;
    }

    public XMethod getXMethod() {
        return thisMethodInfo;
    }

    public MethodDescriptor getMethodDescriptor() {
        return thisMethodInfo;
    }

    public XField getXField() {
        return thisFieldInfo;
    }

    public FieldDescriptor getFieldDescriptor() {
        return thisFieldInfo;
    }

    /** Get the constant pool for the current or most recently visited class */
    public ConstantPool getConstantPool() {
        return constantPool;
    }

    /**
     * Get the slash-formatted class name for the current or most recently
     * visited class
     */
    public @SlashedClassName String getClassName() {
        return className;
    }

    /** Get the dotted class name for the current or most recently visited class */
    public @DottedClassName String getDottedClassName() {
        return dottedClassName;
    }

    /**
     * Get the (slash-formatted?) package name for the current or most recently
     * visited class
     */
    public String getPackageName() {
        return packageName;
    }

    /** Get the source file name for the current or most recently visited class */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Get the slash-formatted superclass name for the current or most recently
     * visited class
     */
    public @SlashedClassName String getSuperclassName() {
        return superclassName;
    }

    /**
     * Get the dotted superclass name for the current or most recently visited
     * class
     */
    public @DottedClassName String getDottedSuperclassName() {
        return dottedSuperclassName;
    }

    /** Get the JavaClass object for the current or most recently visited class */
    public JavaClass getThisClass() {
        return thisClass;
    }

    /** If currently visiting a method, get the method's fully qualified name */
    public String getFullyQualifiedMethodName() {
        if (!visitingMethod) {
            throw new IllegalStateException("getFullyQualifiedMethodName called while not visiting method");
        }
        if (fullyQualifiedMethodName == null) {
            getMethodName();
            getDottedMethodSig();
            StringBuilder ref = new StringBuilder(5 + dottedClassName.length() + methodName.length() + dottedMethodSig.length());

            ref.append(dottedClassName).append(".").append(methodName).append(" : ").append(dottedMethodSig);
            fullyQualifiedMethodName = ref.toString();
        }
        return fullyQualifiedMethodName;
    }

    /**
     * is the visitor currently visiting a method?
     */
    public boolean visitingMethod() {
        return visitingMethod;
    }

    /**
     * is the visitor currently visiting a field?
     */
    public boolean visitingField() {
        return visitingField;
    }

    /** If currently visiting a field, get the field's Field object */
    public Field getField() {
        if (!visitingField) {
            throw new IllegalStateException("getField called while not visiting field");
        }
        return field;
    }

    /** If currently visiting a method, get the method's Method object */
    public Method getMethod() {
        if (!visitingMethod) {
            throw new IllegalStateException("getMethod called while not visiting method");
        }
        return method;
    }

    /** If currently visiting a method, get the method's name */
    public String getMethodName() {
        if (!visitingMethod) {
            throw new IllegalStateException("getMethodName called while not visiting method");
        }
        if (methodName == null) {
            methodName = getStringFromIndex(method.getNameIndex());
        }

        return methodName;
    }

    static Pattern argumentSignature = Pattern.compile("\\[*([BCDFIJSZ]|L[^;]*;)");

    public static int getNumberArguments(String signature) {
        int count = 0;
        int pos = 1;
        boolean inArray = false;

        while (true) {
            switch (signature.charAt(pos++)) {
            case ')':
                return count;
            case '[':
                if (!inArray) {
                    count++;
                }
                inArray = true;
                break;
            case 'L':
                if (!inArray) {
                    count++;
                }
                while (signature.charAt(pos) != ';') {
                    pos++;
                }
                pos++;
                inArray = false;
                break;
            default:
                if (!inArray) {
                    count++;
                }
                inArray = false;
                break;
            }
        }

    }

    /**
     * Returns true if given constant pool probably has a reference to any of supplied methods
     * Useful to exclude from analysis uninteresting classes
     * @param cp constant pool
     * @param methods methods collection
     * @return true if method is found
     */
    public static boolean hasInterestingMethod(ConstantPool cp, Collection<MethodDescriptor> methods) {
        for (Constant c : cp.getConstantPool()) {
            if (c instanceof ConstantMethodref || c instanceof ConstantInterfaceMethodref) {
                ConstantCP desc = (ConstantCP) c;
                ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(desc.getNameAndTypeIndex());
                String className = cp.getConstantString(desc.getClassIndex(), Const.CONSTANT_Class);
                String name = ((ConstantUtf8) cp.getConstant(nameAndType.getNameIndex())).getBytes();
                String signature = ((ConstantUtf8) cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                // We don't know whether method is static thus cannot use equals
                int hash = FieldOrMethodDescriptor.getNameSigHashCode(name, signature);
                for (MethodDescriptor method : methods) {
                    if (method.getNameSigHashCode() == hash
                            && (method.getSlashedClassName().isEmpty() || method.getSlashedClassName().equals(className))
                            && method.getName().equals(name) && method.getSignature().equals(signature)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasInterestingClass(ConstantPool cp, Collection<String> classes) {
        for (Constant c : cp.getConstantPool()) {
            if (c instanceof ConstantClass) {
                String className = ((ConstantUtf8) cp.getConstant(((ConstantClass) c).getNameIndex())).getBytes();
                if (classes.contains(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getNumberMethodArguments() {
        return getNumberArguments(getMethodSig());
    }

    /**
     * If currently visiting a method, get the method's slash-formatted
     * signature
     */
    public String getMethodSig() {
        if (!visitingMethod) {
            throw new IllegalStateException("getMethodSig called while not visiting method");
        }
        if (methodSig == null) {
            methodSig = getStringFromIndex(method.getSignatureIndex());
        }
        return methodSig;
    }

    /** If currently visiting a method, get the method's dotted method signature */
    public String getDottedMethodSig() {
        if (!visitingMethod) {
            throw new IllegalStateException("getDottedMethodSig called while not visiting method");
        }
        if (dottedMethodSig == null) {
            dottedMethodSig = getMethodSig().replace('/', '.');
        }
        return dottedMethodSig;
    }

    /** If currently visiting a field, get the field's name */
    public String getFieldName() {
        if (!visitingField) {
            throw new IllegalStateException("getFieldName called while not visiting field");
        }
        if (fieldName == null) {
            fieldName = getStringFromIndex(field.getNameIndex());
        }

        return fieldName;
    }

    /** If currently visiting a field, get the field's slash-formatted signature */
    public String getFieldSig() {
        if (!visitingField) {
            throw new IllegalStateException("getFieldSig called while not visiting field");
        }
        if (fieldSig == null) {
            fieldSig = getStringFromIndex(field.getSignatureIndex());
        }
        return fieldSig;
    }

    /** If currently visiting a field, return whether or not the field is static */
    public boolean getFieldIsStatic() {
        if (!visitingField) {
            throw new IllegalStateException("getFieldIsStatic called while not visiting field");
        }
        return fieldIsStatic;
    }

    /** If currently visiting a field, get the field's fully qualified name */
    public String getFullyQualifiedFieldName() {
        if (!visitingField) {
            throw new IllegalStateException("getFullyQualifiedFieldName called while not visiting field");
        }
        if (fullyQualifiedFieldName == null) {
            fullyQualifiedFieldName = getDottedClassName() + "." + getFieldName() + " : " + getFieldSig();
        }
        return fullyQualifiedFieldName;
    }

    /** If currently visiting a field, get the field's dot-formatted signature */
    @Deprecated
    public String getDottedFieldSig() {
        if (!visitingField) {
            throw new IllegalStateException("getDottedFieldSig called while not visiting field");
        }
        if (dottedFieldSig == null) {
            dottedFieldSig = fieldSig.replace('/', '.');
        }
        return dottedFieldSig;
    }

    @Override
    public String toString() {
        if (visitingMethod) {
            return this.getClass().getSimpleName() + " analyzing " + getClassName() + "." + getMethodName() + getMethodSig();
        } else if (visitingField) {
            return this.getClass().getSimpleName() + " analyzing " + getClassName() + "." + getFieldName();
        }
        return this.getClass().getSimpleName() + " analyzing " + getClassName();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.bcel.classfile.Visitor#visitAnnotation(org.apache.bcel.classfile
     * .Annotations)
     */
    @Override
    public void visitAnnotation(Annotations arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.bcel.classfile.Visitor#visitAnnotationDefault(org.apache.bcel
     * .classfile.AnnotationDefault)
     */
    @Override
    public void visitAnnotationDefault(AnnotationDefault arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.bcel.classfile.Visitor#visitAnnotationEntry(org.apache.bcel
     * .classfile.AnnotationEntry)
     */
    @Override
    public void visitAnnotationEntry(AnnotationEntry arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.bcel.classfile.Visitor#visitEnclosingMethod(org.apache.bcel
     * .classfile.EnclosingMethod)
     */
    @Override
    public void visitEnclosingMethod(EnclosingMethod arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.bcel.classfile.Visitor#visitParameterAnnotation(org.apache
     * .bcel.classfile.ParameterAnnotations)
     */
    @Override
    public void visitParameterAnnotation(ParameterAnnotations arg0) {
    }

    @Override
    public void visitStackMap(StackMap arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitStackMapEntry(StackMapEntry arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitConstantInvokeDynamic(ConstantInvokeDynamic obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitBootstrapMethods(BootstrapMethods obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitMethodParameters(MethodParameters obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitConstantMethodType(ConstantMethodType obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitConstantMethodHandle(ConstantMethodHandle obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitParameterAnnotationEntry(ParameterAnnotationEntry obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitConstantModule(ConstantModule obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitConstantPackage(ConstantPackage obj) {
        // TODO Auto-generated method stub

    }
}
