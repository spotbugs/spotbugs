/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import java.util.BitSet;
import java.util.HashSet;
import java.util.TreeSet;

import javax.annotation.CheckForNull;

import org.apache.bcel.Const;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;

import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo.Builder;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.classfile.engine.asm.FindBugsASM;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author William Pugh
 */
public class ClassParserUsingASM implements ClassParserInterface {

    // static final boolean NO_SHIFT_INNER_CLASS_CTOR =
    // SystemProperties.getBoolean("classparser.noshift");

    private static final BitSet RETURN_OPCODE_SET = new BitSet();
    static {
        RETURN_OPCODE_SET.set(Opcodes.ARETURN);
        RETURN_OPCODE_SET.set(Opcodes.IRETURN);
        RETURN_OPCODE_SET.set(Opcodes.LRETURN);
        RETURN_OPCODE_SET.set(Opcodes.DRETURN);
        RETURN_OPCODE_SET.set(Opcodes.FRETURN);
        RETURN_OPCODE_SET.set(Opcodes.RETURN);
    }

    private final ClassReader classReader;

    private @SlashedClassName String slashedClassName;

    //    private final ClassDescriptor expectedClassDescriptor;

    private final ICodeBaseEntry codeBaseEntry;



    /**
     * @author pugh
     */
    private final class ClassParserMethodVisitor extends AbstractMethodVisitor {
        /**
         *
         */
        private final TreeSet<ClassDescriptor> calledClassSet;

        /**
         *
         */
        private final MethodInfo.Builder mBuilder;

        /**
         *
         */
        private final String methodName;

        /**
         *
         */
        private final int access;

        /**
         *
         */
        private final String methodDesc;

        /**
         *
         */
        private final ClassNameAndSuperclassInfo.Builder cBuilder;

        boolean sawReturn;

        boolean sawNormalThrow = false;

        boolean sawUnsupportedThrow = false;

        boolean sawSystemExit = false;

        boolean sawBranch = false;

        boolean sawBackBranch = false;

        int methodCallCount = 0;

        int fieldInstructionCount = 0;

        boolean sawStubThrow = false;

        boolean justSawInitializationOfUnsupportedOperationException;

        boolean isBridge;

        String bridgedMethodSignature;

        IdentityMethodState identityState =
                IdentityMethodState.INITIAL;

        ParameterLoadState parameterLoadState = ParameterLoadState.OTHER;

        int parameterForLoadState;

        StubState stubState = StubState.INITIAL;

        boolean isAccessMethod;

        String accessOwner, accessName, accessDesc;

        boolean accessForField;

        boolean accessIsStatic;

        HashSet<Label> labelsSeen = new HashSet<>();

        private int parameterCount = -1;

        /**
         * @param calledClassSet
         * @param mBuilder
         * @param methodName
         * @param access
         * @param methodDesc
         * @param cBuilder
         */
        private ClassParserMethodVisitor(TreeSet<ClassDescriptor> calledClassSet,
                MethodInfo.Builder mBuilder, String methodName, int access,
                String methodDesc, ClassNameAndSuperclassInfo.Builder cBuilder) {
            this.calledClassSet = calledClassSet;
            this.mBuilder = mBuilder;
            this.methodName = methodName;
            this.access = access;
            this.methodDesc = methodDesc;
            this.cBuilder = cBuilder;
            sawReturn = (access & Opcodes.ACC_NATIVE) != 0;
            isBridge = (access & Opcodes.ACC_SYNTHETIC) != 0 && (access & Opcodes.ACC_BRIDGE) != 0;
            isAccessMethod = methodName.startsWith("access$");
        }

        boolean isStatic() {
            return (access & Opcodes.ACC_STATIC) != 0;
        }

        @Override
        public void visitLocalVariable(String name,
                String desc,
                String signature,
                Label start,
                Label end,
                int index) {
            mBuilder.setVariableHasName(index);

        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst.equals("Stub!")) {
                stubState = StubState.LOADED_STUB;
            } else {
                stubState = StubState.INITIAL;
            }
            identityState = IdentityMethodState.NOT;
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
            case Opcodes.MONITORENTER:
                mBuilder.setUsesConcurrency();
                break;
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
            case Opcodes.FRETURN:
                if (identityState == IdentityMethodState.LOADED_PARAMETER) {
                    mBuilder.setIsIdentity();
                }
                sawReturn = true;
                break;
            case Opcodes.RETURN:
                sawReturn = true;
                break;
            case Opcodes.ATHROW:
                if (stubState == StubState.INITIALIZE_RUNTIME) {
                    sawStubThrow = true;
                } else if (justSawInitializationOfUnsupportedOperationException) {
                    sawUnsupportedThrow = true;
                } else {
                    sawNormalThrow = true;
                }
                break;
            default:
                break;
            }

            resetState();
        }

        public void resetState() {
            stubState = StubState.INITIAL;
        }

        @Override
        public void visitSomeInsn() {
            identityState = IdentityMethodState.NOT;
            parameterLoadState = ParameterLoadState.OTHER;
            resetState();
        }

        @Override
        public void visitVarInsn(int opcode, int var) {

            boolean match = false;
            if (parameterLoadState == ParameterLoadState.OTHER && !isStatic() && var == 0) {
                parameterLoadState = ParameterLoadState.LOADED_THIS;

                match = true;
            } else if (parameterLoadState == ParameterLoadState.LOADED_THIS && var > 0) {
                parameterLoadState = ParameterLoadState.LOADED_THIS_AND_PARAMETER;
                parameterForLoadState = var;
                match = true;
            }

            if (identityState == IdentityMethodState.INITIAL) {
                match = true;
                if (var > 0 || isStatic()) {
                    identityState = IdentityMethodState.LOADED_PARAMETER;
                } else {
                    identityState = IdentityMethodState.NOT;
                }

            }
            if (!match) {
                visitSomeInsn();
            }
        }

        @Override
        public void visitFieldInsn(int opcode,
                String owner,
                String name,
                String desc) {
            if (opcode == Opcodes.PUTFIELD && parameterLoadState == ParameterLoadState.LOADED_THIS_AND_PARAMETER
                    && owner.equals(slashedClassName) && name.startsWith("this$")) {
                // the field that has name starts with "this$" is generated for non-static inner class
                // https://sourceforge.net/p/findbugs/bugs/1015/
                mBuilder.setVariableIsSynthetic(parameterForLoadState);
            }
            fieldInstructionCount++;

            if (isAccessMethod && this.accessOwner == null) {
                this.accessOwner = owner;
                this.accessName = name;
                this.accessDesc = desc;
                this.accessIsStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
                this.accessForField = true;
            }
            visitSomeInsn();
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
            AnnotationValue value = new AnnotationValue(desc);
            mBuilder.addAnnotation(desc, value);
            return value.getAnnotationVisitor();
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                Object... bsmArgs) {
            mBuilder.setUsesInvokeDynamic();
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            identityState = IdentityMethodState.NOT;
            methodCallCount++;
            if (isAccessMethod && this.accessOwner == null) {
                this.accessOwner = owner;
                this.accessName = name;
                this.accessDesc = desc;
                this.accessIsStatic = opcode == Opcodes.INVOKESTATIC;
                this.accessForField = false;
            }
            if (stubState == StubState.LOADED_STUB && opcode == Opcodes.INVOKESPECIAL
                    && "java/lang/RuntimeException".equals(owner) && "<init>".equals(name)) {
                stubState = StubState.INITIALIZE_RUNTIME;
            } else {
                stubState = StubState.INITIAL;
            }
            if (owner.startsWith("java/util/concurrent")) {
                mBuilder.setUsesConcurrency();
            }
            if (opcode == Opcodes.INVOKEINTERFACE) {
                return;
            }

            if (owner.charAt(0) == '[' && owner.charAt(owner.length() - 1) != ';') {
                // primitive array
                return;
            }
            if (opcode == Opcodes.INVOKESTATIC && "java/lang/System".equals(owner) && "exit".equals(name)
                    && !sawReturn) {
                sawSystemExit = true;
            }
            justSawInitializationOfUnsupportedOperationException = opcode == Opcodes.INVOKESPECIAL
                    && "java/lang/UnsupportedOperationException".equals(owner) && "<init>".equals(name);

            if (isBridge && bridgedMethodSignature == null) {
                switch (opcode) {
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEINTERFACE:
                    if (desc != null && name.equals(methodName)) {
                        bridgedMethodSignature = desc;
                    }
                    break;
                default:
                    break;
                }
            }

            // System.out.println("Call from " +
            // ClassParserUsingASM.this.slashedClassName +
            // " to " + owner + " : " + desc);
            if (desc == null || desc.indexOf('[') == -1 && desc.indexOf('L') == -1) {
                return;
            }
            if (ClassParserUsingASM.this.slashedClassName.equals(owner)) {
                return;
            }
            ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(owner);
            calledClassSet.add(classDescriptor);

        }

        private void sawBranchTo(Label label) {
            sawBranch = true;
            if (labelsSeen.contains(label)) {
                sawBackBranch = true;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            sawBranchTo(label);
            identityState = IdentityMethodState.NOT;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            sawBranchTo(dflt);
            for (Label lbl : labels) {
                sawBranchTo(lbl);
            }
            identityState = IdentityMethodState.NOT;
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            sawBranchTo(dflt);
            for (Label lbl : labels) {
                sawBranchTo(lbl);
            }
            identityState = IdentityMethodState.NOT;
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLabel(Label label) {
            labelsSeen.add(label);
            super.visitLabel(label);

        }

        @Override
        public void visitEnd() {
            labelsSeen.clear();
            if (isAccessMethod && accessOwner != null) {
                if (!accessForField && methodCallCount == 1) {
                    mBuilder.setAccessMethodForMethod(accessOwner, accessName, accessDesc, accessIsStatic);
                } else if (accessForField && fieldInstructionCount == 1) {
                    boolean isSetter = methodDesc.endsWith(")V");
                    int numArg = new SignatureParser(methodDesc).getNumParameters();
                    int expected = 0;
                    if (!accessIsStatic) {
                        expected++;
                    }
                    if (isSetter) {
                        expected++;
                    }
                    boolean OK;
                    if (isSetter) {
                        OK = methodDesc.substring(1).startsWith(ClassName.toSignature(accessOwner) + accessDesc);
                    } else {
                        OK = methodDesc.substring(1).startsWith(ClassName.toSignature(accessOwner));
                    }
                    if (numArg == expected && OK) {
                        mBuilder.setAccessMethodForField(accessOwner, accessName, accessDesc, accessIsStatic);
                    }
                }
            }
            if (sawBackBranch) {
                mBuilder.setHasBackBranch();
            }
            boolean sawThrow = sawNormalThrow || sawUnsupportedThrow || sawStubThrow;
            if (sawThrow && !sawReturn || sawSystemExit && !sawBranch) {

                mBuilder.setIsUnconditionalThrower();
                if (!sawReturn && !sawNormalThrow) {
                    if (sawUnsupportedThrow) {
                        mBuilder.setUnsupported();
                    }
                    if (sawStubThrow) {
                        mBuilder.addAccessFlags(Opcodes.ACC_SYNTHETIC);
                        mBuilder.setIsStub();

                    }
                }
                // else
                // System.out.println(slashedClassName+"."+methodName+methodDesc
                // + " is thrower");
            }
            mBuilder.setNumberMethodCalls(methodCallCount);
            MethodInfo methodInfo = mBuilder.build();
            Builder classBuilder = (ClassInfo.Builder) cBuilder;
            if (isBridge && bridgedMethodSignature != null && !bridgedMethodSignature.equals(methodDesc)) {
                classBuilder.addBridgeMethodDescriptor(methodInfo, bridgedMethodSignature);
            } else {
                classBuilder.addMethodDescriptor(methodInfo);
            }

            if (methodInfo.usesConcurrency()) {
                classBuilder.setUsesConcurrency();
            }
            if (methodInfo.isStub()) {
                classBuilder.setHasStubs();
            }
        }

        @Override
        public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
            this.parameterCount = parameterCount;
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitParameterAnnotation(int parameter, String desc,
                boolean visible) {
            AnnotationValue value = new AnnotationValue(desc);
            int shift = 0;
            if (parameterCount >= 0) {
                // if we have synthetic parameter, shift `parameter` value
                shift = new SignatureParser(methodDesc).getNumParameters() - parameterCount;
            }
            mBuilder.addParameterAnnotation(parameter + shift, desc, value);
            return value.getAnnotationVisitor();
        }

        @Override
        public org.objectweb.asm.AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                String desc, boolean visible) {
            TypeReference typeRefObject = new TypeReference(typeRef);
            if (typeRefObject.getSort() == TypeReference.METHOD_FORMAL_PARAMETER && typePath == null) {
                // treat as parameter annotation
                AnnotationValue value = new AnnotationValue(desc);
                mBuilder.addParameterAnnotation(typeRefObject.getFormalParameterIndex(), desc, value);
                return value.getAnnotationVisitor();
            }
            if (typeRefObject.getSort() == TypeReference.METHOD_RETURN && typePath == null) {
                // treat as method annotation
                AnnotationValue value = new AnnotationValue(desc);
                mBuilder.addAnnotation(desc, value);
                return value.getAnnotationVisitor();
            }
            return null;
        }
    }

    enum StubState {
        INITIAL, LOADED_STUB, INITIALIZE_RUNTIME
    }

    enum IdentityMethodState {
        INITIAL, LOADED_PARAMETER, NOT;
    }
    enum ParameterLoadState {
        OTHER, LOADED_THIS, LOADED_THIS_AND_PARAMETER;
    }

    public ClassParserUsingASM(ClassReader classReader, @CheckForNull ClassDescriptor expectedClassDescriptor,
            ICodeBaseEntry codeBaseEntry) {
        this.classReader = classReader;
        //        this.expectedClassDescriptor = expectedClassDescriptor;
        this.codeBaseEntry = codeBaseEntry;
    }

    @Override
    public void parse(final ClassNameAndSuperclassInfo.Builder cBuilder) throws InvalidClassFileFormatException {

        cBuilder.setCodeBaseEntry(codeBaseEntry);

        final TreeSet<ClassDescriptor> calledClassSet = new TreeSet<>();

        classReader.accept(new ClassVisitor(FindBugsASM.ASM_VERSION) {

            //            boolean isInnerClass = false;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                ClassParserUsingASM.this.slashedClassName = name;
                cBuilder.setClassfileVersion(version >>> 16, version & 0xffff);
                cBuilder.setAccessFlags(access);
                cBuilder.setClassDescriptor(DescriptorFactory.createClassDescriptor(name));
                cBuilder.setInterfaceDescriptorList(DescriptorFactory.createClassDescriptor(interfaces));
                if (superName != null) {
                    cBuilder.setSuperclassDescriptor(DescriptorFactory.createClassDescriptor(superName));
                }
                if (cBuilder instanceof ClassInfo.Builder) {
                    ((ClassInfo.Builder) cBuilder).setSourceSignature(signature);
                }
            }

            @Override
            public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean isVisible) {
                if (cBuilder instanceof ClassInfo.Builder) {
                    AnnotationValue value = new AnnotationValue(desc);
                    ((ClassInfo.Builder) cBuilder).addAnnotation(desc, value);
                    return value.getAnnotationVisitor();
                }
                return null;
            }

            @Override
            public void visitAttribute(Attribute arg0) {
                //
            }

            @Override
            public void visitEnd() {
                //
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                //                if (name.equals("this$0"))
                //                    isInnerClass = true;

                if (desc == null) {
                    throw new NullPointerException("Description cannot be null");
                }
                if (cBuilder instanceof ClassInfo.Builder) {
                    final ClassInfo.Builder cBuilder2 = (ClassInfo.Builder) cBuilder;
                    if ((access & Opcodes.ACC_VOLATILE) != 0 || desc.contains("util/concurrent")) {
                        cBuilder2.setUsesConcurrency();
                    }
                    final FieldInfo.Builder fBuilder = new FieldInfo.Builder(slashedClassName, name, desc, access);
                    fBuilder.setSourceSignature(signature);
                    return new AbstractFieldAnnotationVisitor() {

                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
                            AnnotationValue value = new AnnotationValue(desc);
                            fBuilder.addAnnotation(desc, value);
                            return value.getAnnotationVisitor();
                        }

                        @Override
                        public void visitEnd() {
                            cBuilder2.addFieldDescriptor(fBuilder.build());

                        }

                    };

                }
                return null;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                if (name.equals(slashedClassName) && outerName != null) {
                    if (cBuilder instanceof ClassInfo.Builder) {
                        ClassDescriptor outerClassDescriptor = DescriptorFactory.createClassDescriptor(outerName);
                        ((ClassInfo.Builder) cBuilder).setImmediateEnclosingClass(outerClassDescriptor);
                        ((ClassInfo.Builder) cBuilder).setAccessFlags(access);
                    }

                }

            }

            @Override
            public MethodVisitor visitMethod(final int access, final String methodName, final String methodDesc,
                    String signature, String[] exceptions) {
                if (cBuilder instanceof ClassInfo.Builder) {
                    final MethodInfo.Builder mBuilder = new MethodInfo.Builder(slashedClassName, methodName, methodDesc, access);
                    mBuilder.setSourceSignature(signature);
                    mBuilder.setThrownExceptions(exceptions);
                    if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
                        mBuilder.setUsesConcurrency();
                    }

                    return new ClassParserMethodVisitor(calledClassSet, mBuilder, methodName, access, methodDesc, cBuilder);

                }
                return null;
            }

            @Override
            public void visitOuterClass(String owner, String name, String desc) {

            }

            @Override
            public void visitSource(String arg0, String arg1) {
                if (cBuilder instanceof ClassInfo.Builder) {
                    ((ClassInfo.Builder) cBuilder).setSource(arg0);
                }

            }
        }, ClassReader.SKIP_FRAMES);
        HashSet<ClassDescriptor> referencedClassSet = new HashSet<>();

        // collect class references

        int constantPoolCount = classReader.readUnsignedShort(8);
        int offset = 10;
        char[] buf = new char[1024];
        // System.out.println("constant pool count: " + constantPoolCount);
        for (int count = 1; count < constantPoolCount; count++) {
            int tag = classReader.readByte(offset);

            int size;
            switch (tag) {
            case Const.CONSTANT_Methodref:
            case Const.CONSTANT_InterfaceMethodref:
            case Const.CONSTANT_Fieldref:
            case Const.CONSTANT_Integer:
            case Const.CONSTANT_Float:
            case Const.CONSTANT_NameAndType:
            case Const.CONSTANT_Dynamic:
            case Const.CONSTANT_InvokeDynamic:
                size = 5;
                break;
            case Const.CONSTANT_Long:
            case Const.CONSTANT_Double:
                size = 9;
                count++;
                break;
            case Const.CONSTANT_Utf8:
                size = 3 + classReader.readUnsignedShort(offset + 1);
                break;
            case Const.CONSTANT_Class:
                @SlashedClassName
                String className = classReader.readUTF8(offset + 1, buf);
                if (className.indexOf('[') >= 0) {
                    ClassParser.extractReferencedClassesFromSignature(referencedClassSet, className);
                } else if (ClassName.isValidClassName(className)) {
                    ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(className);
                    referencedClassSet.add(classDescriptor);
                }
                size = 3;
                break;
            case Const.CONSTANT_String:
            case Const.CONSTANT_MethodType:
                size = 3;
                break;
            case Const.CONSTANT_MethodHandle:
                size = 4;
                break;
            case Const.CONSTANT_Module:
            case Const.CONSTANT_Package:
                size = 3;
                break;
            default:
                throw new IllegalStateException("Unexpected tag of " + tag + " at offset " + offset + " while parsing "
                        + slashedClassName + " from " + codeBaseEntry);
            }
            // System.out.println(count + "@" + offset + " : [" + tag
            // +"] size="+size);
            offset += size;
        }
        cBuilder.setCalledClassDescriptors(calledClassSet);
        cBuilder.setReferencedClassDescriptors(referencedClassSet);
    }

    @Override
    public void parse(ClassInfo.Builder builder) throws InvalidClassFileFormatException {
        parse((ClassNameAndSuperclassInfo.Builder) builder);

    }
}
