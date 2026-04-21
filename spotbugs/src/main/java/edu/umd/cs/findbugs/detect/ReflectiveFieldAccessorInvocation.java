/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.detect.ReflectiveAccessTracker.AccessType;
import java.lang.invoke.VarHandle;
import java.util.Set;

/**
 * Single invocation of an implicit reflective field accessor such as MethodHandle. The invocation doesn't carry the
 * access type, since the accessor was already created for a specific type of access - such as Getter or Setter.
 */
abstract class ReflectiveFieldAccessorInvocation {

    private final XField accessorField;
    private final SourceLineAnnotation sourceLine;

    public ReflectiveFieldAccessorInvocation(final XField accessorField, final SourceLineAnnotation sourceLine) {
        this.accessorField = accessorField;
        this.sourceLine = sourceLine;
    }

    public XField getAccessorField() {
        return accessorField;
    }

    public SourceLineAnnotation getSourceLine() {
        return sourceLine;
    }

    static class MethodHandleInvocation extends ReflectiveFieldAccessorInvocation {

        private static final Set<String> ALLOWED_INVOCATIONS = Set.of("invoke", "invokeExact", "invokeWithArguments");

        public MethodHandleInvocation(XField accessorField, SourceLineAnnotation sourceLine) {
            super(accessorField, sourceLine);
        }

        static boolean isValidInvocation(final String invocation) {
            return invocation != null && ALLOWED_INVOCATIONS.contains(invocation);
        }
    }

    /**
     * Single invocation of a generic reflective field accessor such as VarHandle or AtomicFieldUpdater. Since these
     * accessors can provide both types of access (getter and setters), the Invocation has to be aware of the access type.
     */
    abstract static class TypeAwareRFAInvocation extends ReflectiveFieldAccessorInvocation {
        private final AccessType accessType;

        private TypeAwareRFAInvocation(final XField accessorField, final AccessType accessType, final SourceLineAnnotation sourceLine) {
            super(accessorField, sourceLine);
            this.accessType = accessType;
        }

        public AccessType getAccessType() {
            return accessType;
        }
    }

    static class VarHandleInvocation extends TypeAwareRFAInvocation {

        VarHandleInvocation(final XField accessorField, final AccessType accessType, final SourceLineAnnotation sourceLine) {
            super(accessorField, accessType, sourceLine);
        }

        static AccessType resolveInvocationType(final String invocation) {
            VarHandle.AccessMode mode;

            try {
                mode = VarHandle.AccessMode.valueFromMethodName(invocation);
            } catch (IllegalArgumentException | NullPointerException e) {
                return null;
            }

            switch (mode) {
            case GET:
            case GET_VOLATILE:
            case GET_ACQUIRE:
            case GET_OPAQUE:
                return AccessType.GETTER;

            case SET:
            case SET_VOLATILE:
            case SET_RELEASE:
            case SET_OPAQUE:
                return AccessType.SETTER;

            default:
                // Since all 31 methods are accounted for, anything that isn't a direct GET or SET is a
                // Read-and-Write operation.
                return AccessType.BOTH;
            }
        }
    }

    static class AtomicUpdaterInvocation extends TypeAwareRFAInvocation {

        AtomicUpdaterInvocation(final XField accessorField, final AccessType accessType, final SourceLineAnnotation sourceLine) {
            super(accessorField, accessType, sourceLine);
        }

        static AccessType resolveInvocationType(final String invocation) {
            if (invocation == null) {
                return null;
            }

            switch (invocation) {
            // ---------- READ ONLY ----------
            case "get":
                return AccessType.GETTER;

            // ---------- WRITE ONLY ----------
            case "set":
            case "lazySet":
                return AccessType.SETTER;

            // ---------- READ + WRITE ----------
            case "compareAndSet":
            case "weakCompareAndSet":
            case "getAndSet":
            case "getAndIncrement":
            case "getAndDecrement":
            case "getAndAdd":
            case "incrementAndGet":
            case "decrementAndGet":
            case "addAndGet":
            case "getAndUpdate":
            case "updateAndGet":
            case "getAndAccumulate":
            case "accumulateAndGet":
                return AccessType.BOTH;

            default:
                return null;
            }
        }
    }
}
