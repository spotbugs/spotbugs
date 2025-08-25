/*
 * SpotBugs - SpotBugs in Java programs
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
package edu.umd.cs.findbugs.bytecode;

import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.AnnotationEntryGen;
import org.apache.bcel.generic.FieldGenOrMethodGen;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * Utility to analyze class members.
 */
public final class MemberUtils {

    /**
     * This will capture annotations such as <code>org.immutables.value.Generated</code> or <code>lombok.Generated</code>.
     * Note that <code>javax.annotation.Generated</code>, <code>javax.annotation.processing.Generated</code> only have source retention and are not visible to SpotBugs.
     */
    private static final String GENERATED_TYPE_SUFFIX = "/Generated;";
    private static final String GENERATED_NAME_SUFFIX = "/Generated";

    private MemberUtils() {
        throw new AssertionError("Utility classes can't be instantiated");
    }

    private static boolean internalIsSynthetic(final FieldOrMethod m) {
        if (m.isSynthetic()) {
            return true;
        }

        for (final Attribute a : m.getAttributes()) {
            if (a instanceof Synthetic) {
                return true;
            }
        }

        return false;
    }

    private static boolean internalIsSynthetic(final FieldGenOrMethodGen m) {
        if (m.isSynthetic()) {
            return true;
        }

        for (final Attribute a : m.getAttributes()) {
            if (a instanceof Synthetic) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGeneratedMethod(final FieldOrMethod m) {
        for (AnnotationEntry a : m.getAnnotationEntries()) {
            String typeName = a.getAnnotationType();
            if (typeName.endsWith(GENERATED_TYPE_SUFFIX)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGeneratedMethod(final FieldGenOrMethodGen m) {
        for (AnnotationEntryGen a : m.getAnnotationEntries()) {
            String typeName = a.getAnnotation().getAnnotationType();
            if (typeName.endsWith(GENERATED_NAME_SUFFIX)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGenerated(final AnnotatedObject o) {
        for (AnnotationValue a : o.getAnnotations()) {
            String typeName = a.getAnnotationClass().getClassName();
            if (typeName.endsWith(GENERATED_NAME_SUFFIX)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the method could be a lambda. Notice this is a best-check,
     * since once compiled lambda methods are not univocally distinguishable.
     *
     * @param m The method to check if it's a lambda
     * @return True if this could be a lambda, false otherwise
     */
    public static boolean couldBeLambda(final Method m) {
        return m.isPrivate() && internalIsSynthetic(m);
    }

    /**
     * Checks if the method could be a lambda. Notice this is a best-check,
     * since once compiled lambda methods are not univocally distinguishable.
     *
     * @param m The method to check if it's a lambda
     * @return True if this could be a lambda, false otherwise
     */
    public static boolean couldBeLambda(final XMethod m) {
        return m.isPrivate() && m.isSynthetic();
    }

    /**
     * Checks if the method could be a lambda. Notice this is a best-check,
     * since once compiled lambda methods are not univocally distinguishable.
     *
     * @param m The method to check if it's a lambda
     * @return True if this could be a lambda, false otherwise
     */
    public static boolean couldBeLambda(final MethodGen m) {
        return m.isPrivate() && internalIsSynthetic(m);
    }

    /**
     * Checks if the given method was user-generated. This takes into
     * account for instance lambda methods, that even though they are marked as
     * "synthetic", they are user-generated, and therefore interesting to
     * analysis. Methods annotated with annotations such as Lombok's Generated are not considered user-generated.
     *
     * @param m The field or method to check.
     * @return True if the given member is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final FieldOrMethod m) {
        return (!internalIsSynthetic(m) || (m instanceof Method && couldBeLambda((Method) m))) && !isGeneratedMethod(m);
    }

    /**
     * Checks if the given method was user-generated. This takes into
     * account for instance lambda methods, that even though they are marked as
     * "synthetic", they are user-generated, and therefore interesting to
     * analysis. Methods annotated with annotations such as Lombok's Generated are not considered user-generated.
     *
     * @param m The field or method to check.
     * @return True if the given member is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final ClassMember m) {
        return (!m.isSynthetic() || (m instanceof XMethod && couldBeLambda((XMethod) m))) && (!(m instanceof XMethod) || !isGenerated(
                (XMethod) m));
    }

    /**
     * Checks if the given method was user-generated. This takes into
     * account for instance lambda methods, that even though they are marked as
     * "synthetic", they are user-generated, and therefore interesting to
     * analysis. Methods annotated with annotations such as Lombok's Generated are not considered user-generated.
     *
     * @param m The field or method to check.
     * @return True if the given member is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final FieldGenOrMethodGen m) {
        return (!internalIsSynthetic(m) || (m instanceof MethodGen && couldBeLambda((MethodGen) m))) && !isGeneratedMethod(m);
    }

    /**
     * Checks if the given class was user-generated, classes annotated with annotations such as Immutables' Generated are not considered user-generated.
     *
     * @param c The class to check.
     * @return True if the given class is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final XClass c) {
        return !isGenerated(c);
    }

    /**
     * Checks if the given method is a main method. It takes into account the changes introduced in JEP 445.
     * A main method has non-private access, is named "main", returns void, and has a single string array argument or has no arguments.
     * @see <a href="https://openjdk.org/jeps/445">JEP 445: Unnamed Classes and Instance Main Methods</a>
     *
     * @param method The method to check
     * @return true if the method is a main method, false otherwise
     */
    public static boolean isMainMethod(final Method method) {
        return !method.isPrivate() && "main".equals(method.getName())
                && ("([Ljava/lang/String;)V".equals(method.getSignature()) || "()V".equals(method.getSignature()));
    }

    /**
     * Checks if the given method is a main method. It takes into account the changes introduced in JEP 445.
     * A main method has non-private access, is named "main", returns void, and has a single string array argument or has no arguments.
     * @see <a href="https://openjdk.org/jeps/445">JEP 445: Unnamed Classes and Instance Main Methods</a>
     *
     * @param method The method to check
     * @return true if the method is a main method, false otherwise
     */
    public static boolean isMainMethod(final XMethod method) {
        return !method.isPrivate() && "main".equals(method.getName())
                && ("([Ljava/lang/String;)V".equals(method.getSignature()) || "()V".equals(method.getSignature()));
    }
}
