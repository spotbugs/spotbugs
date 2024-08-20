/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.github.spotbugs.jdk.internal.classfile.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.spotbugs.java.lang.classfile.ClassSignature;
import com.github.spotbugs.java.lang.classfile.MethodSignature;
import com.github.spotbugs.java.lang.classfile.Signature;
import com.github.spotbugs.java.lang.classfile.Signature.*;

import java.util.Collections;

public final class SignaturesImpl {

    public SignaturesImpl() {
    }

    private String sig;
    private int sigp;

    public ClassSignature parseClassSignature(String signature) {
        this.sig = signature;
        sigp = 0;
        List<TypeParam> typeParamTypes = parseParamTypes();
        RefTypeSig superclass = referenceTypeSig();
        ArrayList<RefTypeSig> superinterfaces = null;
        while (sigp < sig.length()) {
            if (superinterfaces == null)
                superinterfaces = new ArrayList<>();
            superinterfaces.add(referenceTypeSig());
        }
        return new ClassSignatureImpl(typeParamTypes, superclass, null2Empty(superinterfaces));
    }

    public MethodSignature parseMethodSignature(String signature) {
        this.sig = signature;
        sigp = 0;
        List<TypeParam> typeParamTypes = parseParamTypes();
        assert sig.charAt(sigp) == '(';
        sigp++;
        ArrayList<Signature> paramTypes = null;
        while (sig.charAt(sigp) != ')') {
            if (paramTypes == null)
                paramTypes = new ArrayList<>();
            paramTypes.add(typeSig());
        }
        sigp++;
        Signature returnType = typeSig();
        ArrayList<ThrowableSig> throwsTypes = null;
        while (sigp < sig.length() && sig.charAt(sigp) == '^') {
            sigp++;
            if (throwsTypes == null)
                throwsTypes = new ArrayList<>();
            Signature t = typeSig();
            if (t instanceof ThrowableSig) {
                ThrowableSig ts = (ThrowableSig) t;
                throwsTypes.add(ts);
            } else
                throw new IllegalArgumentException("not a valid type signature: " + sig);
        }
        return new MethodSignatureImpl(typeParamTypes, null2Empty(throwsTypes), returnType, null2Empty(paramTypes));
    }

    public Signature parseSignature(String signature) {
        this.sig = signature;
        sigp = 0;
        return typeSig();
    }

    private List<TypeParam> parseParamTypes() {
        ArrayList<TypeParam> typeParamTypes = null;
        if (sig.charAt(sigp) == '<') {
            sigp++;
            typeParamTypes = new ArrayList<>();
            while (sig.charAt(sigp) != '>') {
                int sep = sig.indexOf(":", sigp);
                String name = sig.substring(sigp, sep);
                RefTypeSig classBound = null;
                ArrayList<RefTypeSig> interfaceBounds = null;
                sigp = sep + 1;
                if (sig.charAt(sigp) != ':')
                    classBound = referenceTypeSig();
                while (sig.charAt(sigp) == ':') {
                    sigp++;
                    if (interfaceBounds == null)
                        interfaceBounds = new ArrayList<>();
                    interfaceBounds.add(referenceTypeSig());
                }
                typeParamTypes.add(new TypeParamImpl(name, Optional.ofNullable(classBound), null2Empty(interfaceBounds)));
            }
            sigp++;
        }
        return null2Empty(typeParamTypes);
    }

    private Signature typeSig() {
        char c = sig.charAt(sigp++);
        switch (c) {
        case 'B':
        case 'C':
        case 'D':
        case 'F':
        case 'I':
        case 'J':
        case 'V':
        case 'S':
        case 'Z':
            return Signature.BaseTypeSig.of(c);
        default:
            sigp--;
            return referenceTypeSig();
        }
    }

    private RefTypeSig referenceTypeSig() {
        char c = sig.charAt(sigp++);
        switch (c) {
        case 'L':
            StringBuilder sb = new StringBuilder();
            ArrayList<TypeArg> argTypes = null;
            Signature.ClassTypeSig t = null;
            char sigch;
            do {
                switch (sigch = sig.charAt(sigp++)) {
                case '<': {
                    argTypes = new ArrayList<>();
                    while (sig.charAt(sigp) != '>')
                        argTypes.add(typeArg());
                    sigp++;
                    break;
                }
                case '.':
                case ';': {
                    t = new ClassTypeSigImpl(Optional.ofNullable(t), sb.toString(), null2Empty(argTypes));
                    sb.setLength(0);
                    argTypes = null;
                    break;
                }
                default: {
                    sb.append(sigch);
                    break;
                }
                }
            } while (sigch != ';');
            return t;
        case 'T':
            int sep = sig.indexOf(';', sigp);
            TypeVarSig ty = Signature.TypeVarSig.of(sig.substring(sigp, sep));
            sigp = sep + 1;
            return ty;
        case '[':
            return ArrayTypeSig.of(typeSig());
        }
        throw new IllegalArgumentException("not a valid type signature: " + sig);
    }

    private TypeArg typeArg() {
        char c = sig.charAt(sigp++);
        switch (c) {
        case '*':
            return TypeArg.unbounded();
        case '+':
            return TypeArg.extendsOf(referenceTypeSig());
        case '-':
            return TypeArg.superOf(referenceTypeSig());
        default:
            sigp--;
            return TypeArg.of(referenceTypeSig());
        }
    }

    public static class BaseTypeSigImpl implements Signature.BaseTypeSig {
        private char baseType;

        public BaseTypeSigImpl(char baseType) {
            this.baseType = baseType;
        }

        @Override
        public String signatureString() {
            return "" + baseType;
        }

        @Override
        public char baseType() {
            return baseType;
        }
    }

    public static class TypeVarSigImpl implements Signature.TypeVarSig {
        private String identifier;

        public TypeVarSigImpl(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String signatureString() {
            return "T" + identifier + ';';
        }

        @Override
        public String identifier() {
            return identifier;
        }
    }

    public static class ArrayTypeSigImpl implements Signature.ArrayTypeSig {
        private int arrayDepth;
        private Signature elemType;

        public ArrayTypeSigImpl(int arrayDepth, Signature elemType) {
            this.arrayDepth = arrayDepth;
            this.elemType = elemType;
        }

        @Override
        public Signature componentSignature() {
            return arrayDepth > 1 ? new ArrayTypeSigImpl(arrayDepth - 1, elemType) : elemType;
        }

        @Override
        public String signatureString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arrayDepth; i++) {
                sb.append("[");
            }
            sb.append(elemType.signatureString());
            return sb.toString();
        }

        public int arrayDepth() {
            return arrayDepth;
        }

        public Signature elemType() {
            return elemType;
        }
    }

    public static class ClassTypeSigImpl
            implements Signature.ClassTypeSig {
        private Optional<ClassTypeSig> outerType;
        private String className;
        private List<Signature.TypeArg> typeArgs;

        public ClassTypeSigImpl(Optional<ClassTypeSig> outerType, String className, List<Signature.TypeArg> typeArgs) {
            this.outerType = outerType;
            this.className = className;
            this.typeArgs = typeArgs;
        }

        @Override
        public String signatureString() {
            String prefix = "L";
            if (outerType.isPresent()) {
                prefix = outerType.get().signatureString();
                assert prefix.charAt(prefix.length() - 1) == ';';
                prefix = prefix.substring(0, prefix.length() - 1) + '.';
            }
            String suffix = ";";
            if (!typeArgs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append('<');
                for (TypeArg ta : typeArgs)
                    sb.append(((TypeArgImpl) ta).signatureString());
                suffix = sb.append(">;").toString();
            }
            return prefix + className + suffix;
        }

        @Override
        public String className() {
            return className;
        }

        @Override
        public Optional<ClassTypeSig> outerType() {
            return outerType;
        }

        @Override
        public List<TypeArg> typeArgs() {
            return typeArgs;
        }
    }

    public static class TypeArgImpl implements Signature.TypeArg {
        private WildcardIndicator wildcardIndicator;
        private Optional<RefTypeSig> boundType;

        public TypeArgImpl(WildcardIndicator wildcardIndicator, Optional<RefTypeSig> boundType) {
            this.wildcardIndicator = wildcardIndicator;
            this.boundType = boundType;
        }

        public String signatureString() {
            switch (wildcardIndicator) {
            case DEFAULT:
                return boundType.get().signatureString();
            case EXTENDS:
                return "+" + boundType.get().signatureString();
            case SUPER:
                return "-" + boundType.get().signatureString();
            case UNBOUNDED:
                return "*";
            default:
                throw new IllegalArgumentException("Unexpected wildcard indicator: " + wildcardIndicator);
            }
        }

        @Override
        public WildcardIndicator wildcardIndicator() {
            return wildcardIndicator;
        }

        @Override
        public Optional<RefTypeSig> boundType() {
            return boundType;
        }
    }

    public static class TypeParamImpl
            implements TypeParam {
        private String identifier;
        private Optional<RefTypeSig> classBound;
        private List<RefTypeSig> interfaceBounds;

        public TypeParamImpl(String identifier, Optional<RefTypeSig> classBound, List<RefTypeSig> interfaceBounds) {
            this.identifier = identifier;
            this.classBound = classBound;
            this.interfaceBounds = interfaceBounds;
        }

        @Override
        public String identifier() {
            return identifier;
        }

        @Override
        public Optional<RefTypeSig> classBound() {
            return classBound;
        }

        @Override
        public List<RefTypeSig> interfaceBounds() {
            return interfaceBounds;
        }
    }

    private static StringBuilder printTypeParameters(List<TypeParam> typeParameters) {
        StringBuilder sb = new StringBuilder();
        if (typeParameters != null && !typeParameters.isEmpty()) {
            sb.append('<');
            for (TypeParam tp : typeParameters) {
                sb.append(tp.identifier()).append(':');
                if (tp.classBound().isPresent())
                    sb.append(tp.classBound().get().signatureString());
                if (tp.interfaceBounds() != null)
                    for (RefTypeSig is : tp.interfaceBounds())
                        sb.append(':').append(is.signatureString());
            }
            sb.append('>');
        }
        return sb;
    }

    public static class ClassSignatureImpl implements ClassSignature {
        private List<TypeParam> typeParameters;
        private RefTypeSig superclassSignature;
        private List<RefTypeSig> superinterfaceSignatures;

        public ClassSignatureImpl(List<TypeParam> typeParameters, RefTypeSig superclassSignature,
                List<RefTypeSig> superinterfaceSignatures) {
            this.typeParameters = typeParameters;
            this.superclassSignature = superclassSignature;
            this.superinterfaceSignatures = superinterfaceSignatures;
        }

        @Override
        public String signatureString() {
            StringBuilder sb = printTypeParameters(typeParameters);
            sb.append(superclassSignature.signatureString());
            if (superinterfaceSignatures != null)
                for (RefTypeSig in : superinterfaceSignatures)
                    sb.append(in.signatureString());
            return sb.toString();
        }

        @Override
        public List<TypeParam> typeParameters() {
            return typeParameters;
        }

        @Override
        public RefTypeSig superclassSignature() {
            return superclassSignature;
        }

        @Override
        public List<RefTypeSig> superinterfaceSignatures() {
            return superinterfaceSignatures;
        }
    }

    public static class MethodSignatureImpl implements MethodSignature {
        private List<TypeParam> typeParameters;
        private List<ThrowableSig> throwableSignatures;
        private Signature result;
        private List<Signature> arguments;

        public MethodSignatureImpl(
                List<TypeParam> typeParameters,
                List<ThrowableSig> throwableSignatures,
                Signature result,
                List<Signature> arguments) {
            this.typeParameters = typeParameters;
            this.throwableSignatures = throwableSignatures;
            this.result = result;
            this.arguments = arguments;
        }

        @Override
        public String signatureString() {
            StringBuilder sb = printTypeParameters(typeParameters);
            sb.append('(');
            for (Signature a : arguments)
                sb.append(a.signatureString());
            sb.append(')').append(result.signatureString());
            if (!throwableSignatures.isEmpty())
                for (ThrowableSig t : throwableSignatures)
                    sb.append('^').append(t.signatureString());
            return sb.toString();
        }

        @Override
        public List<TypeParam> typeParameters() {
            return typeParameters;
        }

        @Override
        public List<Signature> arguments() {
            return arguments;
        }

        @Override
        public Signature result() {
            return result;
        }

        @Override
        public List<ThrowableSig> throwableSignatures() {
            return throwableSignatures;
        }
    }

    private static <T> List<T> null2Empty(ArrayList<T> l) {
        return l == null ? Collections.emptyList() : Collections.unmodifiableList(l);
    }
}
