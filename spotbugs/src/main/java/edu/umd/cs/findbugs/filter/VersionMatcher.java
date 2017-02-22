/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.filter;

import javax.annotation.Nonnull;

/**
 * @author pugh
 */
public class VersionMatcher {

    protected final long version;

    protected final RelationalOp relOp;

    @Override
    public int hashCode() {
        return (int) version + relOp.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        VersionMatcher m = (VersionMatcher) o;
        return version == m.version && relOp.equals(m.relOp);
    }

    public VersionMatcher(long version, @Nonnull RelationalOp relOp) {
        if (relOp == null) {
            throw new NullPointerException("relOp must be nonnull");
        }
        this.version = version;
        this.relOp = relOp;
    }

}
