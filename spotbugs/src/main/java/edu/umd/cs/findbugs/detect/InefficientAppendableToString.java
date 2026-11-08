/*
 * SpotBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Detects inefficient usage of {@code appendable.append(charSequence.toString())}.
 *
 * <p>Instead of converting a CharSequence to String before appending, the CharSequence
 * can be directly appended using {@code appendable.append(charSequence)}, which avoids
 * creating an intermediate String object.</p>
 *
 * @author Nikita Dhamapurkar
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/704">Issue #704</a>
 */
public class InefficientAppendableToString extends OpcodeStackDetector {

    private final BugReporter bugReporter;

    /** Classes that implement Appendable and have append(CharSequence) method */
    private static final Set<String> APPENDABLE_CLASSES = Set.of(
            "java/lang/StringBuilder",
            "java/lang/StringBuffer",
            "java/io/StringWriter",
            "java/io/PrintStream",
            "java/io/PrintWriter",
            "java/io/Writer",
            "java/io/CharArrayWriter",
            "java/io/BufferedWriter",
            "java/io/OutputStreamWriter",
            "java/io/FileWriter",
            "java/nio/CharBuffer");

    /** Classes that implement CharSequence */
    private static final Set<String> CHARSEQUENCE_CLASSES = Set.of(
            "java/lang/String",
            "java/lang/StringBuilder",
            "java/lang/StringBuffer",
            "java/lang/CharSequence",
            "java/nio/CharBuffer");

    public InefficientAppendableToString(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        // Only analyze if class uses append methods
        try {
            JavaClass javaClass = classContext.getJavaClass();
            String constantPool = javaClass.getConstantPool().toString();
            if (constantPool.contains("append") && constantPool.contains("toString")) {
                super.visitClassContext(classContext);
            }
        } catch (Exception e) {
            // If we can't check, analyze anyway
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        // Look for INVOKEVIRTUAL or INVOKEINTERFACE for append method
        if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE)
                && "append".equals(getNameConstantOperand())
                && stack.getStackDepth() >= 2) {

            String sig = getSigConstantOperand();
            String calledClass = getClassConstantOperand();

            // Check if this is append(CharSequence) or append(CharSequence, int, int)
            boolean isAppendCharSequence = "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;".equals(sig)
                    || "(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;".equals(sig)
                    || "(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;".equals(sig)
                    || "(Ljava/lang/CharSequence;II)Ljava/lang/Appendable;".equals(sig)
                    || "(Ljava/lang/CharSequence;II)Ljava/lang/StringBuilder;".equals(sig)
                    || "(Ljava/lang/CharSequence;II)Ljava/lang/StringBuffer;".equals(sig)
                    // Also check for String parameter which accepts CharSequence subclasses
                    || "(Ljava/lang/String;)Ljava/lang/StringBuilder;".equals(sig)
                    || "(Ljava/lang/String;)Ljava/lang/StringBuffer;".equals(sig);

            // Check if this is called on an Appendable
            boolean isAppendable = isAppendableClass(calledClass);

            if (isAppendCharSequence && isAppendable) {
                // Get the stack item that's being appended (argument to append)
                int argIndex = sig.contains("II") ? 2 : 0; // Account for int, int parameters
                if (stack.getStackDepth() > argIndex) {
                    OpcodeStack.Item item = stack.getStackItem(argIndex);

                    // Check if this item came from a toString() call on a CharSequence
                    if (isFromCharSequenceToString(item)) {
                        bugReporter.reportBug(new BugInstance(this, "IATS_INEFFICIENT_APPEND_TO_STRING", LOW_PRIORITY)
                                .addClassAndMethod(this)
                                .addCalledMethod(this)
                                .addSourceLine(this));
                    }
                }
            }
        }
    }

    /**
     * Check if the class is an Appendable implementation.
     */
    private boolean isAppendableClass(String className) {
        if (APPENDABLE_CLASSES.contains(className)) {
            return true;
        }
        // Check if it implements Appendable
        try {
            return Hierarchy.isSubtype(className.replace('/', '.'), "java.lang.Appendable");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if the stack item came from calling toString() on a CharSequence.
     */
    private boolean isFromCharSequenceToString(OpcodeStack.Item item) {
        // Check if the return type is String and it came from a toString() call
        XMethod xMethod = item.getReturnValueOf();
        if (xMethod == null) {
            return false;
        }

        if (!"toString".equals(xMethod.getName())
                || !"()Ljava/lang/String;".equals(xMethod.getSignature())) {
            return false;
        }

        // Check if the class is a CharSequence
        String className = xMethod.getClassName();
        if (className == null) {
            return false;
        }

        String slashClassName = className.replace('.', '/');
        if (CHARSEQUENCE_CLASSES.contains(slashClassName)) {
            return true;
        }

        // Check if it implements CharSequence
        try {
            return Hierarchy.isSubtype(className, "java.lang.CharSequence");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
