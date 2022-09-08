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

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.FieldGenOrMethodGen;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * Utility to analyze class members.
 */
public final class MemberUtils {

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
     * analysis.
     *
     * @param m The field or method to check.
     * @return True if the given member is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final FieldOrMethod m) {
        return !internalIsSynthetic(m) || (m instanceof Method && couldBeLambda((Method) m));
    }

    /**
     * Checks if the given method was user-generated. This takes into
     * account for instance lambda methods, that even though they are marked as
     * "synthetic", they are user-generated, and therefore interesting to
     * analysis.
     *
     * @param m The field or method to check.
     * @return True if the given member is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final ClassMember m) {
        return !m.isSynthetic() || (m instanceof XMethod && couldBeLambda((XMethod) m));
    }

    /**
     * Checks if the given method was user-generated. This takes into
     * account for instance lambda methods, that even though they are marked as
     * "synthetic", they are user-generated, and therefore interesting to
     * analysis.
     *
     * @param m The field or method to check.
     * @return True if the given member is user generated, false otherwise.
     */
    public static boolean isUserGenerated(final FieldGenOrMethodGen m) {
        return !internalIsSynthetic(m) || (m instanceof MethodGen && couldBeLambda((MethodGen) m));
    }
}
