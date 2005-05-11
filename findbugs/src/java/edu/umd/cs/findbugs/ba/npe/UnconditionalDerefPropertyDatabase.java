/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.ba.interproc.HierarchyWalkDirection;
import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabaseFormatException;
import edu.umd.cs.findbugs.ba.interproc.PropertyCombinator;

/**
 * Method property database storing which method parameters might
 * be unconditionally dereferenced.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalDerefPropertyDatabase extends MethodPropertyDatabase<UnconditionalDerefProperty> {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#decodeProperty(java.lang.String)
	 */
	//@Override
	protected UnconditionalDerefProperty decodeProperty(String propStr)
			throws MethodPropertyDatabaseFormatException {
		try {
			int unconditionalDerefSet = Integer.parseInt(propStr);
			UnconditionalDerefProperty prop = new UnconditionalDerefProperty();
			prop.setUnconditionalDerefParamSet(unconditionalDerefSet);
			return prop;
		} catch (NumberFormatException e) {
			throw new MethodPropertyDatabaseFormatException("Invalid unconditional deref param set: " + propStr);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#encodeProperty(Property)
	 */
	//@Override
	protected String encodeProperty(UnconditionalDerefProperty property) {
		return String.valueOf(property.getUnconditionalDerefParamSet());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#getHierarchyWalkDirection()
	 */
	//@Override
	protected HierarchyWalkDirection getHierarchyWalkDirection() {
		// Properties go upwards towards supertypes:
		// if a subtype param is dereferenced unconditionally,
		// then we assume that subtype mether is reachable from
		// supertype method call sites.
		return HierarchyWalkDirection.TOWARDS_SUPERTYPES;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#getPropertyCombinator()
	 */
	//@Override
	protected PropertyCombinator<UnconditionalDerefProperty> getPropertyCombinator() {
		return new PropertyCombinator<UnconditionalDerefProperty>() {
			public UnconditionalDerefProperty combine(UnconditionalDerefProperty sourceProperty, UnconditionalDerefProperty targetProperty) {
				// Take the union of unconditional deref param sets.
				UnconditionalDerefProperty prop = new UnconditionalDerefProperty();
				prop.setUnconditionalDerefParamSet(sourceProperty.getUnconditionalDerefParamSet() | targetProperty.getUnconditionalDerefParamSet());
				return prop;
			}
		};
	}
	
	//@Override
	protected UnconditionalDerefProperty createDefault() {
		return new UnconditionalDerefProperty();
	}

}
