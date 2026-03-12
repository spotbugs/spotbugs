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

import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.detect.ReflectiveAccessTracker.AccessType;
import java.lang.invoke.VarHandle;
import java.util.Set;

/**
 * Single invocation of an implicit reflective accessor field such as MethodHandle.
 */
abstract class ReflectiveInvocation {

    private final XField accessorField;

    public ReflectiveInvocation(final XField accessorField) {
        this.accessorField = accessorField;
    }

    public XField getAccessorField() {
        return accessorField;
    }

    static class MethodHandleInvocation extends ReflectiveInvocation {

        private static final Set<String> ALLOWED_INVOCATIONS = Set.of("invoke", "invokeExact", "invokeWithArguments");

        public MethodHandleInvocation(XField accessorField) {
            super(accessorField);
        }

        static boolean isValidInvocation(final String invocation) {
            return invocation != null && ALLOWED_INVOCATIONS.contains(invocation);
        }
    }

    /**
     * Single invocation of an explicit reflective accessor field such as VarHandle, or AtomicFieldUpdater.
     * This invocation has to supply the access type.
     */
    abstract static class SpecifiedInvocation extends ReflectiveInvocation {
        private final AccessType accessType;

        private SpecifiedInvocation(final XField accessorField, final AccessType accessType) {
            super(accessorField);
            this.accessType = accessType;
        }

        public AccessType getAccessType() {
            return accessType;
        }
    }

    static class VarHandleInvocation extends SpecifiedInvocation {

        VarHandleInvocation(final XField accessorField, final AccessType accessType) {
            super(accessorField, accessType);
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

    static class AtomicUpdaterInvocation extends SpecifiedInvocation {

        AtomicUpdaterInvocation(final XField accessorField, final AccessType accessType) {
            super(accessorField, accessType);
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
