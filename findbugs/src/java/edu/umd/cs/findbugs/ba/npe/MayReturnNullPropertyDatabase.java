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
 * MethodPropertyDatabase for keeping track of which methods
 * may return null.
 * 
 * @author David Hovemeyer
 */
public class MayReturnNullPropertyDatabase extends MethodPropertyDatabase<MayReturnNullProperty> {

	//@Override
	protected MayReturnNullProperty decodeProperty(String propStr)
			throws MethodPropertyDatabaseFormatException {
		return (Boolean.valueOf(propStr).booleanValue())
			? MayReturnNullProperty.doesReturnNullInstance()
			: MayReturnNullProperty.doesNotReturnNullInstance();
	}

	//@Override
	protected String encodeProperty(MayReturnNullProperty property) {
		return property.mayReturnNull() ? "true" : "false";
	}

	//@Override
	protected HierarchyWalkDirection getHierarchyWalkDirection() {
		// Information propagates up towards supertypes.
		// If a subclass method returns null, that means calls to the
		// (potentially overridden) superclass method may return
		// null as well.
		return HierarchyWalkDirection.TOWARDS_SUPERTYPES;
	}

	//@Override
	protected PropertyCombinator<MayReturnNullProperty> getPropertyCombinator() {
		return new PropertyCombinator<MayReturnNullProperty>() {
			public MayReturnNullProperty combine(MayReturnNullProperty sourceProperty, MayReturnNullProperty targetProperty) {
				if (sourceProperty.mayReturnNull() || targetProperty.mayReturnNull())
					return MayReturnNullProperty.doesReturnNullInstance();
				else
					return MayReturnNullProperty.doesNotReturnNullInstance();
			}
		};
	}

}
