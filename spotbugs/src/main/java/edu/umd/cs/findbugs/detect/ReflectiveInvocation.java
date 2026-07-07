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
 * A single invocation of a reflective field accessor (MethodHandle, VarHandle or
 * AtomicFieldUpdater). The {@link Kind} passed to {@link #create} selects the rules used to decide
 * whether the called method actually accesses a field and, if so, which access type it implies.
 * <p>
 * For a MethodHandle the access type is fixed at the accessor's declaration, so {@link #accessType}
 * is {@code null} here and the accessor's own type is used during resolution. For VarHandle and
 * AtomicFieldUpdater the access type is derived from the invoked method name.
 */
class ReflectiveInvocation {

    enum Kind {
        METHOD_HANDLE,
        VAR_HANDLE,
        ATOMIC_UPDATER
    }

    private static final Set<String> METHOD_HANDLE_INVOCATIONS = Set.of("invoke", "invokeExact", "invokeWithArguments");

    /** The field holding the accessor handle. */
    private final XField accessorField;

    /** GETTER/SETTER/BOTH, or {@code null} when the type comes from the accessor (MethodHandle). */
    private final AccessType accessType;

    private ReflectiveInvocation(final XField accessorField, final AccessType accessType) {
        this.accessorField = accessorField;
        this.accessType = accessType;
    }

    XField accessorField() {
        return accessorField;
    }

    AccessType accessType() {
        return accessType;
    }

    /**
     * Builds an invocation for a recognized accessor call, or returns {@code null} when the called
     * method is not a field-accessing invocation for the given accessor kind.
     */
    static ReflectiveInvocation create(final Kind kind, final XField accessorField, final String invokedMethodName) {
        switch (kind) {
        case METHOD_HANDLE:
            return METHOD_HANDLE_INVOCATIONS.contains(invokedMethodName)
                    ? new ReflectiveInvocation(accessorField, null)
                    : null;
        case VAR_HANDLE: {
            AccessType accessType = resolveVarHandleType(invokedMethodName);
            return accessType == null ? null : new ReflectiveInvocation(accessorField, accessType);
        }
        case ATOMIC_UPDATER: {
            AccessType accessType = resolveAtomicUpdaterType(invokedMethodName);
            return accessType == null ? null : new ReflectiveInvocation(accessorField, accessType);
        }
        default:
            return null;
        }
    }

    private static AccessType resolveVarHandleType(final String invokedMethodName) {
        VarHandle.AccessMode mode;

        try {
            mode = VarHandle.AccessMode.valueFromMethodName(invokedMethodName);
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

    private static AccessType resolveAtomicUpdaterType(final String invokedMethodName) {
        if (invokedMethodName == null) {
            return null;
        }

        switch (invokedMethodName) {
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
