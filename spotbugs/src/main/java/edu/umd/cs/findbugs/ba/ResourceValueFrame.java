/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

public class ResourceValueFrame extends Frame<ResourceValue> {

    public enum State {
        /**
         * The resource escapes the method.
         */
        ESCAPED(0),

        /**
         * The resource is open (or locked, etc) on paths that include only normal
         * control flow.
         */
        OPEN(1),

        /**
         * The resource is open (or locked, etc) on paths that include exception
         * control flow.
         */
        OPEN_ON_EXCEPTION_PATH(2),

        /**
         * The resource is closed (or unlocked, etc).
         */
        CLOSED(3),

        /**
         * The resource has been created, but is not open.
         */
        CREATED(4),

        /**
         * The resource doesn't exist.
         */
        NONEXISTENT(5);

        State(int type) {
            this.type = type;
        }

        private final int type;

        public int getType() {
            return type;
        }
    }

    private State status;

    public ResourceValueFrame(int numSlots) {
        super(numSlots);
        this.status = State.NONEXISTENT;
    }

    public State getStatus() {
        return status;
    }

    public void setStatus(State status) {
        this.status = status;
    }

    @Override
    public boolean sameAs(Frame<ResourceValue> other_) {
        if (!super.sameAs(other_)) {
            return false;
        }

        ResourceValueFrame other = (ResourceValueFrame) other_;
        return this.status == other.status;
    }

    @Override
    public void copyFrom(Frame<ResourceValue> other_) {
        super.copyFrom(other_);
        ResourceValueFrame other = (ResourceValueFrame) other_;
        this.status = other.status;
    }

    private static final String[] statusList = { "(escaped)", "(open)", "(open_exception)", "(closed)", "(created)",
        "(nonexistent)" };

    @Override
    public String toString() {
        return super.toString() + statusList[status.getType()];
    }

}
