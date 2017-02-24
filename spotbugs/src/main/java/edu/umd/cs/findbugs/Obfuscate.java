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

package edu.umd.cs.findbugs;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Iterator;

import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pugh
 */
public class Obfuscate {

    final static String HASH_SEED = SystemProperties.getProperty("hashSeed", "");

    public static String hashData(String in) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest((HASH_SEED + in).getBytes("UTF-8"));
            return String.format("%040x", new BigInteger(1, hash));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String hashFilename(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            String suffix = fileName.substring(lastDot);
            return hashData(fileName.substring(0, lastDot)) + suffix;
        }
        return hashData(fileName);
    }

    public static String hashClass(@DottedClassName String className) {
        if (className.startsWith("java")) {
            return className;
        }
        return "obfuscated.H" + hashData(className);
    }

    public static String hashSignature(String signature) {
        char firstChar = signature.charAt(0);
        switch (firstChar) {
        case '[':
            return '[' + hashSignature(signature.substring(1));
        case 'V':
        case 'Z':

        case 'B':
        case 'S':
        case 'C':
        case 'I':
        case 'J':
        case 'D':
        case 'F':
            if (signature.length() == 1) {
                return signature;
            }
            throw new IllegalArgumentException("bad signature: " + signature);
        case 'L':
            if (!signature.endsWith(";")) {
                throw new IllegalArgumentException("bad signature: " + signature);
            }
            return hashFieldSignature(signature);
        default:
            throw new IllegalArgumentException("bad signature: " + signature);
        }
    }

    public static String hashFieldSignature(String signature) {
        signature = signature.substring(1, signature.length() - 1);
        if (!signature.startsWith("java")) {
            signature = "obfuscated/H" + hashData(signature);
        }
        return "L" + signature + ";";
    }

    public static String hashMethodSignature(String signature) {
        SignatureParser parser = new SignatureParser(signature);
        StringBuilder buf = new StringBuilder("(");
        for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext();) {
            String param = i.next();
            buf.append(hashSignature(param));
        }
        buf.append(")");
        buf.append(hashSignature(parser.getReturnTypeSignature()));
        return buf.toString();

    }

    static MethodAnnotation obfuscate(MethodAnnotation m) {
        String className = m.getClassName();
        if (className.startsWith("java")) {
            return m;
        }

        String methodName = m.getMethodName();
        String methodSignature = m.getMethodSignature();

        if ("hashCode".equals(methodName) && "()I".equals(methodSignature) || "equals".equals(methodName)
                && "(Ljava/lang/Object;)Z".equals(methodSignature) || "compareTo".equals(methodName)
                && "(Ljava/lang/Object;)I".equals(methodSignature) || "<init>".equals(methodName)
                || "<clinit>".equals(methodName)) {
            // don't need to obfuscate method name
        } else {
            methodName = hashData(methodName);
        }

        MethodAnnotation result = new MethodAnnotation(hashClass(className), methodName, hashMethodSignature(methodSignature),
                m.isStatic());
        result.setDescription(m.getDescription());
        return result;
    }

    static FieldAnnotation obfuscate(FieldAnnotation m) {
        FieldAnnotation result = new FieldAnnotation(hashClass(m.getClassName()), hashData(m.getFieldName()),
                hashSignature(m.getFieldSignature()), m.isStatic());
        result.setDescription(m.getDescription());
        return result;

    }


    static ClassAnnotation obfuscate(ClassAnnotation m) {
        ClassAnnotation result = new ClassAnnotation(hashClass(m.getClassName()));
        result.setDescription(m.getDescription());
        return result;
    }

    static TypeAnnotation obfuscate(TypeAnnotation m) {
        TypeAnnotation result = new TypeAnnotation(hashSignature(m.getTypeDescriptor()));
        result.setDescription(m.getDescription());
        return result;

    }

    static IntAnnotation obfuscate(IntAnnotation m) {
        IntAnnotation result = new IntAnnotation(m.getValue());
        result.setDescription(m.getDescription());
        return result;

    }

    static StringAnnotation obfuscate(StringAnnotation m) {
        StringAnnotation result = new StringAnnotation("obfuscated: " + hashData(m.getValue()));
        result.setDescription(m.getDescription());
        return result;

    }

    static SourceLineAnnotation obfuscate(SourceLineAnnotation m) {
        SourceLineAnnotation result = new SourceLineAnnotation(hashClass(m.getClassName()), hashFilename(m.getSourceFile()),
                m.getStartLine(), m.getEndLine(), m.getStartBytecode(), m.getEndBytecode());
        result.setDescription(m.getDescription());
        return result;

    }

    static LocalVariableAnnotation obfuscate(LocalVariableAnnotation m) {

        LocalVariableAnnotation result = new LocalVariableAnnotation(hashData(m.getName()), m.getRegister(), m.getPC());
        result.setDescription(m.getDescription());
        return result;

    }

    public static BugInstance obfuscate(BugInstance b) {
        final BugInstance result = new BugInstance(b.getType(), b.getPriority());
        BugAnnotationVisitor visitor = new BugAnnotationVisitor() {

            @Override
            public void visitTypeAnnotation(TypeAnnotation typeAnnotation) {
                result.add(obfuscate(typeAnnotation));

            }

            @Override
            public void visitStringAnnotation(StringAnnotation stringAnnotation) {
                result.add(obfuscate(stringAnnotation));

            }

            @Override
            public void visitSourceLineAnnotation(SourceLineAnnotation sourceLineAnnotation) {
                result.add(obfuscate(sourceLineAnnotation));

            }

            @Override
            public void visitMethodAnnotation(MethodAnnotation methodAnnotation) {
                result.add(obfuscate(methodAnnotation));

            }

            @Override
            public void visitLocalVariableAnnotation(LocalVariableAnnotation fieldAnnotation) {
                result.add(obfuscate(fieldAnnotation));

            }

            @Override
            public void visitIntAnnotation(IntAnnotation fieldAnnotation) {
                result.add(obfuscate(fieldAnnotation));

            }

            @Override
            public void visitFieldAnnotation(FieldAnnotation fieldAnnotation) {
                result.add(obfuscate(fieldAnnotation));

            }

            @Override
            public void visitClassAnnotation(ClassAnnotation classAnnotation) {
                result.add(obfuscate(classAnnotation));

            }
        };
        for (BugAnnotation a : b.getAnnotations()) {
            a.accept(visitor);
        }
        result.setOldInstanceHash(hashData(b.getInstanceHash()));
        result.setHistory(b);
        return result;
    }

}
