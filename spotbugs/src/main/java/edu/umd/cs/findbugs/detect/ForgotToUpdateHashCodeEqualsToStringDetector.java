package edu.umd.cs.findbugs.detect;

import java.util.*;
import java.util.function.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.generic.*;
import org.objectweb.asm.*;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.classfile.*;
import edu.umd.cs.findbugs.visitclass.*;

/**
 * <p>HET methods: hashCode(), equals(), toString().
 * This detector detects incomplete HET methods for preventing forgetting to update when member variables are updated.</p>
 * 
 * <p>it means an alternative to @EqualsAndHashCode or @ToString of Lombok.</p>
 * 
 * <p>Lombok is used for auto-generation of HET methods to automatically support to update members.
 * If Spotbugs detects incomplete HET methods, programmers can notice that he forgot to update.</p>
 * 
 * <h2>6 BugPatterns of this detector.</h2> 
 * <ul>
 *      <li>{@link ForgotToUpdateHashCodeEqualsToStringDetector#HE_MEMBER_DOESNT_APPEAR_IN_HASHCODE}
 *      class defines member variable doesn't appear in its hashCode().</li>
 *      <li>{@link ForgotToUpdateHashCodeEqualsToStringDetector#HE_MEMBER_DOESNT_APPEAR_IN_EQUALS}
 *      class defines member variable doesn't appear in its equals().</li>
 *      <li>{@link ForgotToUpdateHashCodeEqualsToStringDetector#USELESS_STRING_MEMBER_DOESNT_APPEAR_IN_TOSTRING} 
 *      class defines member variable doesn't appear in its toString().</li>
 *      <li>{@link ForgotToUpdateHashCodeEqualsToStringDetector#HE_NO_HASHCODE_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE} 
 *      class doesn't define hashCode() unlike parent or outer class or interfaces that defines hashCode().</li>
 *      <li>{@link ForgotToUpdateHashCodeEqualsToStringDetector#HE_NO_EQUALS_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE} 
 *      class doesn't define equals() unlike parent or outer class or interfaces that defines equals().</li>
 *      <li>{@link ForgotToUpdateHashCodeEqualsToStringDetector#USELESS_STRING_NO_TOSTRING_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE} 
 *      class doesn't define toString() unlike parent or outer class or interfaces that defines toString(). priority low</li>
 * </ul>
 * 
 * <p>TODO: Many classes of spotbugs are detected by this detector. Too many to fix. 
 * However, since the bugs in this detector is IGNORE or EXP_PRIORITY, it is not detected in gradle tasks such as spotbugsMain.</p>
 * 
 * <p>NOTE: How to fix warnings by this detector. add 'transient' to fields,
 * or modify HET methods, or refactor your class design, or ignore the warnings.</p>
 * 
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1415">issue1415</a>
 * @author lifeinwild1@gmail.com https://github.com/lifeinwild
 */
public class ForgotToUpdateHashCodeEqualsToStringDetector implements Detector2 {
    public static final String HE_NO_EQUALS_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE = "HE_NO_EQUALS_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE";
    public static final String HE_NO_HASHCODE_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE = "HE_NO_HASHCODE_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE";
    public static final String USELESS_STRING_NO_TOSTRING_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE =
            "USELESS_STRING_NO_TOSTRING_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE";
    public static final String HE_MEMBER_DOESNT_APPEAR_IN_EQUALS = "HE_MEMBER_DOESNT_APPEAR_IN_EQUALS";
    public static final String HE_MEMBER_DOESNT_APPEAR_IN_HASHCODE = "HE_MEMBER_DOESNT_APPEAR_IN_HASHCODE";
    public static final String USELESS_STRING_MEMBER_DOESNT_APPEAR_IN_TOSTRING = "USELESS_STRING_MEMBER_DOESNT_APPEAR_IN_TOSTRING";

    private final BugReporter bugReporter;

    /**
     * parent class of targetCls
     */
    private JavaClass parentCls = null;
    /**
     * parent class has hashCode() except {@link java.lang.Object}.
     */
    private boolean parentHasHashCode = false;
    /**
     * parent class has equals() except {@link java.lang.Object}.
     */
    private boolean parentHasEquals = false;
    /**
     * parent class has toString() except {@link java.lang.Object}.
     */
    private boolean parentHasToString = false;

    private JavaClass interfaceDefinesHashCode = null;
    private JavaClass interfaceDefinesEquals = null;
    private JavaClass interfaceDefinesToString = null;
    private boolean interfaceHasHashCode = false;
    private boolean interfaceHasEquals = false;
    private boolean interfaceHasToString = false;

    private boolean targetClsHasHashCode = false;
    private boolean targetClsHasEquals = false;
    private boolean targetClsHasToString = false;

    private boolean targetClsHashCodeIsDummy = false;
    private boolean targetClsEqualsIsDummy = false;
    private boolean targetClsToStringIsDummy = false;

    /**
     * class that this detector analyze
     */
    private JavaClass targetCls = null;

    private boolean needOuterAnalysis = false;
    private JavaClass outerCls = null;
    private boolean outerHasHashCode = false;
    private boolean outerHasEquals = false;
    private boolean outerHasToString = false;

    private boolean setup = false;

    private JavaClass[] targetClsInterfaces = null;
    private List<Field> targetClsInstanceVars = null;

    private ClassDescriptor classDescriptor;

    private Method targetClsHashCode;
    private Method targetClsEquals;
    private Method targetClsTostring;

    private HashSet<Integer> appearedVarNameIndexesInTargetClsHashCode;
    private HashSet<Integer> appearedVarNameIndexesInTargetClsEquals;
    private HashSet<Integer> appearedVarNameIndexesInTargetClsToString;

    public ForgotToUpdateHashCodeEqualsToStringDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    private void checkAboutParent() {
        if (!targetClsHasHashCode) {
            JavaClass derivedFrom = null;
            if (parentHasHashCode) {
                derivedFrom = parentCls;
            } else if (interfaceHasHashCode) {
                derivedFrom = interfaceDefinesHashCode;
            } else if (outerHasHashCode) {
                derivedFrom = outerCls;
            }
            if (derivedFrom != null) {
                BugInstance bug = new BugInstance(this, HE_NO_HASHCODE_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE,
                        EXP_PRIORITY).addClass(targetCls).addClass(derivedFrom);
                bugReporter.reportBug(bug);
            }
        }
        if (!targetClsHasEquals) {
            JavaClass derivedFrom = null;
            if (parentHasEquals) {
                derivedFrom = parentCls;
            } else if (interfaceHasEquals) {
                derivedFrom = interfaceDefinesEquals;
            } else if (outerHasEquals) {
                derivedFrom = outerCls;
            }
            if (derivedFrom != null) {
                BugInstance bug = new BugInstance(this, HE_NO_EQUALS_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE,
                        EXP_PRIORITY).addClass(targetCls).addClass(derivedFrom);
                bugReporter.reportBug(bug);
            }
        }
        if (!targetClsHasToString) {
            JavaClass derivedFrom = null;
            if (parentHasToString) {
                derivedFrom = parentCls;
            } else if (interfaceHasToString) {
                derivedFrom = interfaceDefinesToString;
            } else if (outerHasToString) {
                derivedFrom = outerCls;
            }
            if (derivedFrom != null) {
                BugInstance bug = new BugInstance(this,
                        USELESS_STRING_NO_TOSTRING_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE, IGNORE_PRIORITY)
                                .addClass(targetCls).addClass(derivedFrom);
                bugReporter.reportBug(bug);
            }
        }
    }

    private void checkAboutTarget() {
        if (!targetClsHashCodeIsDummy) {
            checkByInstanceVar(targetClsHashCode, appearedVarNameIndexesInTargetClsHashCode, EXP_PRIORITY);
        }
        if (!targetClsEqualsIsDummy) {
            checkByInstanceVar(targetClsEquals, appearedVarNameIndexesInTargetClsEquals, EXP_PRIORITY);
        }
        if (!targetClsToStringIsDummy) {
            checkByInstanceVar(targetClsTostring, appearedVarNameIndexesInTargetClsToString, IGNORE_PRIORITY);
        }
    }

    private void checkByInstanceVar(Method het, HashSet<Integer> appearedVarNameIndexes, int priority) {
        if (het == null || appearedVarNameIndexes == null)
            return;

        String bugType = getBugTypeOfInstanceVar(het);
        if (bugType == null)
            return;

        for (Field shouldAppearInHETMethod : targetClsInstanceVars) {
            if (!appearedVarNameIndexes.contains(shouldAppearInHETMethod.getNameIndex())) {
                BugInstance bug = new BugInstance(this, bugType, priority).addClassAndMethod(targetCls, het);
                bugReporter.reportBug(bug);
            }
        }
    }

    private void clearAboutInterface() {
        interfaceHasHashCode = false;
        interfaceHasEquals = false;
        interfaceHasToString = false;
    }

    private void clearAboutOuter() {
        outerCls = null;
        outerHasHashCode = false;
        outerHasEquals = false;
        outerHasToString = false;
    }

    private void clearAboutParent() {
        parentCls = null;
        parentHasHashCode = false;
        parentHasEquals = false;
        parentHasToString = false;
    }

    private void clearAboutTarget() {
        targetCls = null;

        targetClsHashCode = null;
        targetClsEquals = null;
        targetClsTostring = null;

        targetClsHasHashCode = false;
        targetClsHasEquals = false;
        targetClsHasToString = false;

        targetClsHashCodeIsDummy = false;
        targetClsEqualsIsDummy = false;
        targetClsToStringIsDummy = false;

        targetClsInstanceVars = null;
        targetClsInterfaces = null;

        appearedVarNameIndexesInTargetClsHashCode = null;
        appearedVarNameIndexesInTargetClsEquals = null;
        appearedVarNameIndexesInTargetClsToString = null;

        needOuterAnalysis = false;
    }

    private void clearExceptBugReporterAndClassDescriptor() {
        clearAboutTarget();
        clearAboutParent();
        clearAboutInterface();
        clearAboutOuter();

        setup = false;
    }

    @Override
    public void finishPass() {
    }

    private String getBugTypeOfInstanceVar(Method het) {
        switch (het.getName().toUpperCase()) {
        case "HASHCODE":
            return HE_MEMBER_DOESNT_APPEAR_IN_HASHCODE;
        case "EQUALS":
            return HE_MEMBER_DOESNT_APPEAR_IN_EQUALS;
        case "TOSTRING":
            return USELESS_STRING_MEMBER_DOESNT_APPEAR_IN_TOSTRING;
        default:
            return null;
        }
    }

    /**
     * @param m
     * @param lineThreshold long method is usually not dummy method and the check cost is heavy. so set 4-20 for performance. set -1 for logical consistency.
     * @return String representation of bytecode of m.
     */
    private String[] getCodeLines(Method m, int lineThreshold) {
        Code code = m.getCode();
        if (code == null) {
            return null;
        }
        byte[] bytecode = code.getCode();
        if (bytecode == null) {
            return null;
        }

        if (lineThreshold != -1 && bytecode.length > lineThreshold) {
            return null;
        }

        String codeStr = Utility.codeToString(bytecode, m.getConstantPool(), 0, -1, true);
        if (codeStr == null || codeStr.length() == 0)
            return null;
        return codeStr.split("\\r?\\n");
    }

    @Override
    public String getDetectorClassName() {
        return this.getClass().getName();
    }

    private InnerClass getInnerClass(JavaClass cls, Function<InnerClass, Boolean> condition) {
        return visitInnerClasses(cls, inner -> cls.getClassNameIndex() == inner.getInnerClassIndex()
                && (condition == null || condition.apply(inner)));
    }

    /**
     * @param cls
     * @return normal instance members.
     */
    private List<Field> getInstanceVariables(JavaClass cls) {
        List<Field> r = new ArrayList<>();
        for (Field f : cls.getFields()) {
            if (f.isTransient() || f.isStatic() || f.isAbstract() || f.isAnnotation() || f.isEnum() || f.isNative()
                    || f.isSynthetic()) {
                continue;
            }
            r.add(f);
        }
        return r;
    }

    private Integer getNameIndexOfDirectFieldAccess(ConstantPoolGen cpg, GETFIELD getfield) {
        if (cpg == null || getfield == null)
            return null;
        Constant c = cpg.getConstant(getfield.getIndex());
        if (c == null || !(c instanceof ConstantFieldref))
            return null;
        ConstantFieldref ref = (ConstantFieldref) c;
        Constant nameTmp = cpg.getConstant(ref.getNameAndTypeIndex());
        if (nameTmp == null || !(nameTmp instanceof ConstantNameAndType))
            return null;
        ConstantNameAndType name = (ConstantNameAndType) nameTmp;
        return name.getNameIndex();
    }

    private Integer getNameIndexOfGetterFieldAccess(ConstantPoolGen cpg, InvokeInstruction invoke) {
        if (targetClsInstanceVars == null || targetClsInstanceVars.size() == 0 || cpg == null || invoke == null)
            return null;
        String invoked = invoke.getMethodName(cpg);
        if (invoked == null)
            return null;
        if (invoked.equals("getClass"))
            return null;
        String mightFieldName = null;
        if (invoked.startsWith("get")) {
            mightFieldName = invoked.substring(3);
        } else {
            mightFieldName = invoked;
        }
        for (Field f : targetClsInstanceVars)
            if (f.getName().equalsIgnoreCase(mightFieldName))
                return f.getNameIndex();
        return null;
    }

    /**
     * scanning method bytecode for detecting field access.
     * 
     * this method supports getXxx() of java bean convention or xxx().
     * 
     * @param m analysis target
     * @return name indexes of fields in m
     */
    private HashSet<Integer> getVariableOrGetterNameIndexes(Method m) {
        HashSet<Integer> nameIndexesOfFields = new HashSet<>();
        if (m == null || m.getCode() == null)
            return nameIndexesOfFields;

        visitInstructions(m, (cpg, inst) -> {
            if (inst.getOpcode() == Const.GETFIELD) {
                Integer index = getNameIndexOfDirectFieldAccess(cpg, (GETFIELD) inst);
                if (index != null)
                    nameIndexesOfFields.add(index);
            } else if (inst instanceof InvokeInstruction) {
                Integer index = getNameIndexOfGetterFieldAccess(cpg, (InvokeInstruction) inst);
                if (index != null)
                    nameIndexesOfFields.add(index);
            }
        });

        return nameIndexesOfFields;
    }

    /**
     * in generally, some methods are dummy like: 
     * {@code 
     * public void f(){
     *    throw new UnsupportedOperationException();
     * }
     * }
     * 
     * @param m
     * @return m is dummy
     */
    private boolean isDummyMethod(Method m) {
        if (m == null)
            return false;
        String[] lines = getCodeLines(m, -1);
        if (lines == null)
            return false;

        // WARNING: This code depends on the implementation of Utility.codeToString().
        int invokeSpecialUnsupportedOperationExceptionLine = -1, athrowLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            line = line.replaceAll("\\s+", " ");
            if (line == null)
                continue;
            String[] elements = line.split("\\s");
            if (elements == null || elements.length < 2)
                continue;
            switch (elements[1]) {
            case "invokespecial":
                if (elements.length >= 3 && elements[2].startsWith("java.lang.UnsupportedOperationException"))
                    invokeSpecialUnsupportedOperationExceptionLine = i;
                break;
            case "athrow":
                athrowLine = i;
                break;
            default:
            }
        }

        // check the line 'throw new UnsupportedOperationException()' exists.
        if (athrowLine == -1 || invokeSpecialUnsupportedOperationExceptionLine == -1)
            return false;
        if (athrowLine - 1 != invokeSpecialUnsupportedOperationExceptionLine)
            return false;
        return true;
    }

    private boolean isEquals(Method m) {
        return "equals".equals(m.getName()) && "(Ljava/lang/Object;)Z".equals(m.getSignature());
    }

    private boolean isHashCode(Method m) {
        return "hashCode".equals(m.getName()) && "()I".equals(m.getSignature());
    }

    private boolean isInnerClass(JavaClass cls, Function<InnerClass, Boolean> condition) {
        return getInnerClass(cls, condition) != null;
    }

    private boolean isNormalClass(JavaClass cls) {
        if (cls.isInterface() || cls.isAnnotation() || cls.isAnonymous() || cls.isNative() || cls.isSynthetic()) {
            return false;
        }
        return true;
    }

    private boolean isNotStaticInnerClass(JavaClass cls) {
        return isInnerClass(cls, inner -> (Opcodes.ACC_STATIC & inner.getInnerAccessFlags()) == 0);
    }

    private boolean isOrigin(JavaClass cls) {
        final String origin = "java.lang.Object";
        if (cls.getClassName().equals(origin)) {
            return true;
        }
        return false;
    }

    private boolean isToString(Method m) {
        return "toString".equals(m.getName()) && "()Ljava/lang/String;".equals(m.getSignature());
    }

    private void setup() throws CheckedAnalysisException {
        if (setup) {
            clearExceptBugReporterAndClassDescriptor();
        }

        setupAboutTarget();
        setupAboutOtherCls();

        setup = true;
    }

    private void setupAboutOtherCls() {
        try {
            parentCls = targetCls.getSuperClass();
        } catch (ClassNotFoundException e) {
        }

        if (needOuterAnalysis) {
            try {
                outerCls = Util.getOuterClass(targetCls);
            } catch (ClassNotFoundException e) {
            }
        }

        try {
            targetClsInterfaces = targetCls.getInterfaces();
        } catch (ClassNotFoundException e) {
        }

        setupAboutOtherClsHET();
    }

    private void setupAboutOtherClsHET() {
        if (parentCls != null && !isOrigin(parentCls)) {
            setupParentClsHasHETMethod();
        }

        if (targetClsInterfaces != null && targetClsInterfaces.length > 0) {
            traverseInterfaces(targetCls, interf -> {
                setupInterfaceHasHETMethod(interf);
            });
        }

        if (outerCls != null && !isOrigin(outerCls)) {
            setupOuterClsHasHETMethod();
        }
    }

    private void setupAboutTarget() throws CheckedAnalysisException {
        targetCls = Global.getAnalysisCache().getClassAnalysis(JavaClass.class, classDescriptor);

        targetClsInstanceVars = getInstanceVariables(targetCls);

        setupHETMethods();

        targetClsHashCodeIsDummy = isDummyMethod(targetClsHashCode);
        targetClsEqualsIsDummy = isDummyMethod(targetClsEquals);
        targetClsToStringIsDummy = isDummyMethod(targetClsTostring);

        appearedVarNameIndexesInTargetClsHashCode = getVariableOrGetterNameIndexes(targetClsHashCode);
        appearedVarNameIndexesInTargetClsEquals = getVariableOrGetterNameIndexes(targetClsEquals);
        appearedVarNameIndexesInTargetClsToString = getVariableOrGetterNameIndexes(targetClsTostring);

        needOuterAnalysis = isNotStaticInnerClass(targetCls);
    }

    private void setupHETMethods() {
        for (Method m : targetCls.getMethods()) {
            setupHETMethods(m);
        }
    }

    private void setupHETMethods(Method targetClsMethod) {
        if (isHashCode(targetClsMethod)) {
            targetClsHasHashCode = true;
            targetClsHashCode = targetClsMethod;
        }
        if (isEquals(targetClsMethod)) {
            targetClsHasEquals = true;
            targetClsEquals = targetClsMethod;
        }
        if (isToString(targetClsMethod)) {
            targetClsHasToString = true;
            targetClsTostring = targetClsMethod;
        }
    }

    private void setupInterfaceHasHETMethod(JavaClass interf) {
        for (Method m : interf.getMethods()) {
            if (isHashCode(m)) {
                interfaceHasHashCode = true;
                interfaceDefinesHashCode = interf;
            } else if (isEquals(m)) {
                interfaceHasEquals = true;
                interfaceDefinesEquals = interf;
            } else if (isToString(m)) {
                interfaceHasToString = true;
                interfaceDefinesToString = interf;
            }
        }
    }

    private void setupOuterClsHasHETMethod() {
        for (Method m : outerCls.getMethods()) {
            if (isHashCode(m)) {
                outerHasHashCode = true;
            } else if (isEquals(m)) {
                outerHasEquals = true;
            } else if (isToString(m)) {
                outerHasToString = true;
            }
        }
    }

    private void setupParentClsHasHETMethod() {
        for (Method m : parentCls.getMethods()) {
            if (isHashCode(m)) {
                parentHasHashCode = true;
            } else if (isEquals(m)) {
                parentHasEquals = true;
            } else if (isToString(m)) {
                parentHasToString = true;
            }
        }
    }

    /**
     * this method assume no cycle definition of interface about extend-relations.
     * the assumption is safe in Java.
     * 
     * @param here
     * @param f
     */
    private void traverseInterfaces(JavaClass here, Consumer<JavaClass> f) {
        if (here == null || f == null)
            return;

        JavaClass[] hereInterfaces = null;
        try {
            hereInterfaces = here.getInterfaces();
        } catch (ClassNotFoundException e) {
        }

        if (hereInterfaces == null) {
            return;
        }

        for (JavaClass interf : hereInterfaces) {
            if (!interf.isInterface()) {
                continue;
            }
            f.accept(interf);
            traverseInterfaces(interf, f);
        }
    }

    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        this.classDescriptor = classDescriptor;

        setup();

        if (isOrigin(targetCls) || !isNormalClass(targetCls))
            return;

        checkAboutTarget();
        checkAboutParent();
    }

    private InnerClass visitInnerClasses(JavaClass cls, Function<InnerClass, Boolean> condition) {
        for (Attribute a : cls.getAttributes()) {
            if (a instanceof InnerClasses) {
                InnerClasses inners = (InnerClasses) a;
                for (InnerClass inner : inners.getInnerClasses()) {
                    if (condition == null)
                        return inner;
                    if (condition.apply(inner)) {
                        return inner;
                    }
                }
            }
        }
        return null;
    }

    private void visitInstructions(Method m, BiConsumer<ConstantPoolGen, Instruction> perInstruction) {
        if (m == null || perInstruction == null) {
            return;
        }
        InstructionList il = new InstructionList(m.getCode().getCode());
        ConstantPoolGen cpg = new ConstantPoolGen(m.getConstantPool());
        il.forEach(handle -> {
            Instruction inst = handle.getInstruction();
            if (inst == null)
                return;
            perInstruction.accept(cpg, inst);
        });
    }

}
