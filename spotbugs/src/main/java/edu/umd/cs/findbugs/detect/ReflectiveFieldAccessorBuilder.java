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
import edu.umd.cs.findbugs.detect.ReflectiveFieldAccessor.ExplicitAccessor;
import edu.umd.cs.findbugs.detect.ReflectiveFieldAccessor.ImplicitAccessor;

/**
 * Gathers details about an Accessor field, the actual field the Accessor references and the type of access it provides
 */
abstract class ReflectiveFieldAccessorBuilder {
    private final XField actualField;
    private final int assignmentExpectedAtPC;
    private final AccessType accessType;
    private XField accessorField;

    ReflectiveFieldAccessorBuilder(final XField actualField, int assignmentExpectedAtPC,
            final AccessType accessType) {
        this.actualField = actualField;
        this.assignmentExpectedAtPC = assignmentExpectedAtPC;
        this.accessType = accessType;
    }

    /**
     * Builds the finalized ReflectiveFieldAccessor with all the details about the accessed field, type of access and
     * a log of accesses.
     * @param accessLog of the accessed field
     * @return finalized ReflectiveFieldAccessor which will handle the invocations of the actual field
     */
    abstract ReflectiveFieldAccessor buildWithAccessLog(final ReflectiveFieldAccessLog accessLog);

    public XField getAccessorField() {
        return accessorField;
    }

    public int getAssignmentExpectedAtPC() {
        return assignmentExpectedAtPC;
    }

    public XField getActualField() {
        return actualField;
    }

    ReflectiveFieldAccessorBuilder withAccessor(final XField accessor) {
        accessorField = accessor;
        return this;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    /**
     * Builds ImplicitAccessor, since a MethodHandle is already created as a Getter or a Setter. The type of access
     * is therefore set at instantiation.
     */
    static class MethodHandleAccessorBuilder extends ReflectiveFieldAccessorBuilder {

        public MethodHandleAccessorBuilder(final XField actualField, final int assignmentExpectedAtPC,
                final AccessType accessType) {
            super(actualField, assignmentExpectedAtPC, accessType);
        }

        @Override
        ImplicitAccessor buildWithAccessLog(final ReflectiveFieldAccessLog accessLog) {
            return new ImplicitAccessor(accessLog, getAccessType());
        }
    }

    /**
     * Builds ExplicitAccessor, since a VarHandle provides both Getter and Setter methods. The access type is therefore
     * only known at invocation.
     */
    static class VarHandleAccessorBuilder extends ReflectiveFieldAccessorBuilder {
        public VarHandleAccessorBuilder(final XField actualField, final int assignmentExpectedAtPC) {
            super(actualField, assignmentExpectedAtPC, AccessType.BOTH);
        }

        @Override
        ReflectiveFieldAccessor buildWithAccessLog(final ReflectiveFieldAccessLog accessLog) {
            return new ExplicitAccessor(accessLog);
        }
    }

    /**
     * Builds ExplicitAccessor, since an AtomicFieldUpdater provides both Getter and Setter methods. The access type
     * is therefore only known at invocation.
     */
    static class AtomicUpdaterAccessorBuilder extends ReflectiveFieldAccessorBuilder {
        public AtomicUpdaterAccessorBuilder(final XField actualField, final int assignmentExpectedAtPC) {
            super(actualField, assignmentExpectedAtPC, AccessType.BOTH);
        }

        @Override
        ReflectiveFieldAccessor buildWithAccessLog(final ReflectiveFieldAccessLog accessLog) {
            return new ExplicitAccessor(accessLog);
        }
    }
}
