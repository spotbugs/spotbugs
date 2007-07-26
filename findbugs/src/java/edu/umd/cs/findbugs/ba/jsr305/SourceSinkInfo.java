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

import edu.umd.cs.findbugs.ba.Location;

/**
 * Information about a source or sink in
 * the type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public class SourceSinkInfo implements Comparable<SourceSinkInfo> {
	private final SourceSinkType type;
	private final Location location;
	private int parameter;
	private int local;

	/**
	 * Constructor.
	 * 
	 * @param type     type of the source or sink
	 * @param location Location of the source or sink
	 */
	public SourceSinkInfo(SourceSinkType type, Location location) {
		this.type = type;
		this.location = location;
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
	 * @param parameter The parameter to set.
	 */
	public void setParameter(int parameter) {
		this.parameter = parameter;
	}
	
	/**
	 * @param parameter The parameter to set.
	 * @param local     The local to set.
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
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(SourceSinkInfo o) {
		return this.location.compareTo(o.location);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return location.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		SourceSinkInfo other = (SourceSinkInfo) obj;
		return this.location.equals(other.location);
	}
}
