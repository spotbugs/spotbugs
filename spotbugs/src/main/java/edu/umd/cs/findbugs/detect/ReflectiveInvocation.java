/*
 * Contributions to SpotBugs
 * Copyright (C) 2026 PANTHEON.tech, s.r.o.
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

/**
 * Single invocation of an implicit reflective accessor field such as MethodHandle.
 */
class ReflectiveInvocation {

    private final XField accessorField;

    public ReflectiveInvocation(final XField accessorField) {
        this.accessorField = accessorField;
    }

    public XField getAccessorField() {
        return accessorField;
    }

    /**
     * Single invocation of an explicit reflective accessor field such as VarHandle, or AtomicFieldUpdater.
     * This invocation has to supply the access type.
     */
    static class SpecifiedInvocation extends ReflectiveInvocation {
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

        private VarHandleInvocation(final XField accessorField, final AccessType accessType) {
            super(accessorField, accessType);
        }

        static VarHandleInvocation create(final XField accessorField, final String invocation) {
            AccessType resolvedType = resolveInvocationType(invocation);
            return resolvedType != null ? new VarHandleInvocation(accessorField, resolvedType) : null;
        }

        private static AccessType resolveInvocationType(final String invocation) {
            switch (invocation) {
            // ---------- READ ONLY ----------
            case "get":
            case "getVolatile":
            case "getAcquire":
            case "getOpaque":
                return AccessType.GETTER;

            // ---------- WRITE ONLY ----------
            case "set":
            case "setVolatile":
            case "setRelease":
            case "setOpaque":
                return AccessType.SETTER;

            // ---------- READ + CONDITIONAL WRITE ----------
            case "compareAndSet":
            case "compareAndExchange":
            case "compareAndExchangeAcquire":
            case "compareAndExchangeRelease":
            case "compareAndExchangeVolatile":
            case "weakCompareAndSet":
            case "weakCompareAndSetAcquire":
            case "weakCompareAndSetRelease":
            case "weakCompareAndSetVolatile":

                // ---------- READ + WRITE ----------
            case "getAndSet":
            case "getAndSetAcquire":
            case "getAndSetRelease":
            case "getAndSetVolatile":
            case "getAndAdd":
            case "getAndAddAcquire":
            case "getAndAddRelease":
            case "getAndAddVolatile":
            case "getAndBitwiseOr":
            case "getAndBitwiseOrAcquire":
            case "getAndBitwiseOrRelease":
            case "getAndBitwiseOrVolatile":
            case "getAndBitwiseAnd":
            case "getAndBitwiseAndAcquire":
            case "getAndBitwiseAndRelease":
            case "getAndBitwiseAndVolatile":
            case "getAndBitwiseXor":
            case "getAndBitwiseXorAcquire":
            case "getAndBitwiseXorRelease":
            case "getAndBitwiseXorVolatile":
                return AccessType.BOTH;
            default:
                return null;
            }
        }
    }

    static class AtomicUpdaterInvocation extends SpecifiedInvocation {

        private AtomicUpdaterInvocation(final XField accessorField, final AccessType accessType) {
            super(accessorField, accessType);
        }

        static AtomicUpdaterInvocation create(final XField accessorField, final String invocation) {
            AccessType resolvedType = resolveInvocationType(invocation);
            return resolvedType != null ? new AtomicUpdaterInvocation(accessorField, resolvedType) : null;
        }

        private static AccessType resolveInvocationType(final String invocation) {
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
