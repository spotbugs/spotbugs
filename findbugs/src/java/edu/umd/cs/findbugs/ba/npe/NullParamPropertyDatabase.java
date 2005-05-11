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
 * @author daveho
 */
public class NullParamPropertyDatabase extends MethodPropertyDatabase<NullParamProperty> {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#decodeProperty(java.lang.String)
	 */
	//@Override
	protected NullParamProperty decodeProperty(String propStr)
			throws MethodPropertyDatabaseFormatException {
		try {
			int nullParamSet = Integer.parseInt(propStr);
			NullParamProperty prop = new NullParamProperty();
			prop.setNullParamSet(nullParamSet);
			return prop;
		} catch (NumberFormatException e) {
			throw new MethodPropertyDatabaseFormatException("Invalid null param set: " + propStr);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#encodeProperty(Property)
	 */
	//@Override
	protected String encodeProperty(NullParamProperty property) {
		return String.valueOf(property.getNullParamSet());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#getHierarchyWalkDirection()
	 */
	//@Override
	protected HierarchyWalkDirection getHierarchyWalkDirection() {
		// Properties go downwards towards subtypes:
		// if a null is passed to a supertype parameter,
		// then we assume it can propagate to any subtype method.
		return HierarchyWalkDirection.TOWARDS_SUBTYPES;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#getPropertyCombinator()
	 */
	//@Override
	protected PropertyCombinator<NullParamProperty> getPropertyCombinator() {
		return new PropertyCombinator<NullParamProperty>() {
			public NullParamProperty combine(NullParamProperty sourceProperty, NullParamProperty targetProperty) {
				// Take the union of null param sets.
				NullParamProperty prop = new NullParamProperty();
				prop.setNullParamSet(sourceProperty.getNullParamSet() | targetProperty.getNullParamSet());
				return prop;
			}
		};
	}

}
