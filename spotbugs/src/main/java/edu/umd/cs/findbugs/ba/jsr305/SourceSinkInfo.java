/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.Objects;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Information about a source or sink in the type qualifier dataflow analysis.
 *
 * @author David Hovemeyer
 */
public class SourceSinkInfo implements Comparable<SourceSinkInfo> {
    private static final int PRIME = 31;

    private final SourceSinkType type;

    private final Location location;

    private final ValueNumber vn;

    private final When when;

    private int parameter;

    private int local;

    private Object constantValue;

    private boolean interproc;

    /**
     * Constructor.
     *
     * @param type
     *            type of the source or sink
     * @param location
     *            Location of the source or sink
     * @param vn
     *            the ValueNumber of the annotated value
     * @param when
     *            the When value used (explicitly or implicitly) to annotate
     *            this source or sink
     */
    public SourceSinkInfo(SourceSinkType type, Location location, ValueNumber vn, When when) {
        this.type = type;
        this.location = location;
        this.vn = vn;
        this.when = when;
    }

    /**
     * @return Returns the type.
     */
    public SourceSinkType getType() {
        return type;
    }

    /**
     * @return Returns the location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return Returns the ValueNumber.
     */
    public ValueNumber getValueNumber() {
        return vn;
    }

    /**
     * @return Returns the when.
     */
    public When getWhen() {
        return when;
    }

    /**
     * @param parameter
     *            The parameter to set.
     */
    public void setParameter(int parameter) {
        this.parameter = parameter;
    }

    /**
     * @param parameter
     *            The parameter to set.
     * @param local
     *            The local to set.
     */
    public void setParameterAndLocal(int parameter, int local) {
        this.parameter = parameter;
        this.local = local;
    }

    /**
     * @return Returns the parameter.
     */
    public int getParameter() {
        return parameter;
    }

    /**
     * @return Returns the local.
     */
    public int getLocal() {
        return local;
    }

    /**
     * Set the SourceSinkInfo as having been created based on the results of
     * type qualifiers computed for a called method (and not explicitly
     * annotating the called method).
     *
     * @param interproc
     *            true if the SourceSinkInfo results from computed type
     *            qualifiers for a called method, false otherwise
     */
    public void setInterproc(boolean interproc) {
        this.interproc = interproc;
    }

    /**
     * Return whether or not the SourceSinkInfo was created based on the results
     * of type qualifiers computed for a called method (and not explicitly
     * annotating the called method).
     *
     * @return true if the SourceSinkInfo results from computed type qualifiers
     *         for a called method, false otherwise
     */
    public boolean getInterproc() {
        return interproc;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(SourceSinkInfo o) {
        return this.location.compareTo(o.location);
    }

    public Object getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(Object constantValue) {
        this.constantValue = constantValue;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = PRIME * result + ((constantValue == null) ? 0 : constantValue.hashCode());
        result = PRIME * result + (interproc ? 1231 : 1237);
        result = PRIME * result + local;
        result = PRIME * result + ((location == null) ? 0 : location.hashCode());
        result = PRIME * result + parameter;
        result = PRIME * result + ((type == null) ? 0 : type.hashCode());
        result = PRIME * result + ((vn == null) ? 0 : vn.hashCode());
        result = PRIME * result + ((when == null) ? 0 : when.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SourceSinkInfo other = (SourceSinkInfo) obj;
        return Objects.equals(constantValue, other.constantValue)
                && interproc == other.interproc
                && local == other.local
                && Objects.equals(location, other.location)
                && parameter == other.parameter
                && type == other.type
                && Objects.equals(vn, other.vn)
                && when == other.when;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type.toString() + "@" + location.toCompactString() + "[vn=" + vn.getNumber() + ",when=" + when + "]";
    }
}
