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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * @author pwilliam
 */
public class OpcodeStackScanner {

    static final boolean DEBUG = SystemProperties.getBoolean("oss.debug");

    public static class UnreachableCodeException extends RuntimeException {

        public UnreachableCodeException( @DottedClassName String className, String methodName, String methodSignature, int pc) {
            super("Didn't reach pc " + pc + " of " + className + "." + methodName + methodSignature);
            this.className = className;
            this.methodName = methodName;
            this.methodSignature = methodSignature;
            this.pc = pc;
        }
        @DottedClassName String className;
        String methodName;
        String methodSignature;
        int pc;
    }

    static class EarlyExitException extends RuntimeException {
        final OpcodeStack stack;

        public EarlyExitException(OpcodeStack stack) {

            this.stack = stack;
        }
    }

    public static OpcodeStack getStackAt(JavaClass theClass, Method method, int pc) {
        Scanner scanner = new Scanner(theClass, method, pc);
        try {
            scanner.execute();
        } catch (EarlyExitException e) {
            return e.stack;
        }
        throw new UnreachableCodeException(theClass.getClassName(), method.getName(), method.getSignature(), pc);
    }

    static class Scanner extends OpcodeStackDetector {

        Scanner(JavaClass theClass, Method targetMethod, int targetPC) {
            if(DEBUG) {
                System.out.println("Scanning " + theClass.getClassName() + "." + targetMethod.getName());
            }
            this.theClass = theClass;
            this.targetMethod = targetMethod;
            this.targetPC = targetPC;
        }

        final JavaClass theClass;

        final Method targetMethod;

        final int targetPC;

        @Override
        public void sawOpcode(int seen) {
        }

        @Override
        public void afterOpcode(int seen) {
            if(DEBUG) {
                System.out.printf("%3d: %8s %s%n", getPC(), OPCODE_NAMES[seen], getStack());
            }
            if (getPC() == targetPC) {
                throw new EarlyExitException(stack);
            }
            super.afterOpcode(seen);
        }

        @Override
        public void visitJavaClass(JavaClass obj) {
            setupVisitorForClass(obj);
            getConstantPool().accept(this);
            doVisitMethod(targetMethod);
        }

        public void execute() {
            theClass.accept(this);
        }
    }

}
