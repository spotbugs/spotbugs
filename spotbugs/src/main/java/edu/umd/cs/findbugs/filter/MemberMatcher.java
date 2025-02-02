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

import java.util.Objects;

import javax.annotation.CheckForNull;

/**
 * @author pugh
 */
public class MemberMatcher {

    protected final NameMatch name;

    @CheckForNull
    protected final String role;

    @CheckForNull
    protected final NameMatch signature;

    public MemberMatcher(String name) {
        this(name, null, null);
    }

    public MemberMatcher(String name, String signature) {
        this(name, signature, null);
    }

    public MemberMatcher(String name, String signature, String role) {

        if (name == null) {
            if (signature == null) {
                throw new FilterException(this.getClass().getName() + " must have either name or signature attributes");
            } else {
                name = "~.*"; // any name
            }
        }

        this.name = new NameMatch(name);
        this.signature = new NameMatch(signature);
        this.role = role;

    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (!name.isUniversal()) {
            buf.append("name=\"");
            buf.append(name.getSpec());
            buf.append("\"");
        }
        if (signature != null) {
            if (buf.length() > 0) {
                buf.append(" ");
            }
            buf.append("signature=\"");
            buf.append(signature);
            buf.append("\"");
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        return name.hashCode() + Objects.hashCode(signature);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        MemberMatcher other = (MemberMatcher) o;
        return name.equals(other.name) && Objects.equals(signature, other.signature);
    }

}
