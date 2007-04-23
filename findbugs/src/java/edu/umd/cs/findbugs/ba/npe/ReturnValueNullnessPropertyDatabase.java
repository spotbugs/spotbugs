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

import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;

/**
 * Method property database storing which methods return values guaranteed to be nonnull
 * 
 * @author David Hovemeyer
 */
public class ReturnValueNullnessPropertyDatabase extends MethodPropertyDatabase<Boolean> {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#decodeProperty(java.lang.String)
	 */

	@Override
		 protected Boolean decodeProperty(String propStr) {
			return TigerSubstitutes.parseBoolean(propStr);

	}



	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#encodeProperty(Property)
	 */

	@Override
		 protected String encodeProperty(Boolean property) {
		return property.toString();
	}

}
