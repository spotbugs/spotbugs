/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

package edu.umd.cs.findbugs.plan;

import edu.umd.cs.findbugs.DetectorFactory;

/**
 * Select one or more DetectorFactories as part of
 * satisfying a Detector ordering constraint.
 * 
 * @author David Hovemeyer
 */
public interface DetectorFactorySelector {
	/**
	 * Is given DetectorFactory selected (as part of an ordering constraint)?
	 * 
	 * @param factory a DetectorFactory
	 * @return true if the factory is selected, false otherwise
	 */
	public boolean selectFactory(DetectorFactory factory);
}
