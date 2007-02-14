/*
 * Bytecode analysis framework
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

package edu.umd.cs.findbugs.ba.interproc;

import java.io.IOException;
import java.io.Writer;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.InstanceField;
import edu.umd.cs.findbugs.ba.StaticField;
import edu.umd.cs.findbugs.ba.XField;

/**
 * Interprocedural field property database.
 * 
 * @author David Hovemeyer
 */
public abstract class FieldPropertyDatabase<Property>
		extends PropertyDatabase<XField, Property> {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.PropertyDatabase#parseKey(java.lang.String)
	 */
	
	@Override
         protected XField parseKey(String s) throws PropertyDatabaseFormatException {
		String[] tuple = s.split(",");
		if (tuple.length != 4) {
			throw new PropertyDatabaseFormatException("Invalid field tuple: " + s);
		}
		
		String className = tuple[0];
		String fieldName = tuple[1];
		String signature = tuple[2];
		int accessFlags;
		try {
			accessFlags = Integer.parseInt(tuple[3]);
		} catch (NumberFormatException e) {
			throw new PropertyDatabaseFormatException("Invalid field access flags: " + tuple[3]);
		}

		return (accessFlags & Constants.ACC_STATIC) != 0
			? new StaticField(className, fieldName, signature, accessFlags)
			: new InstanceField(className, fieldName, signature, accessFlags);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.PropertyDatabase#writeKey(java.io.Writer, KeyType)
	 */
	
	@Override
         protected void writeKey(Writer writer, XField key) throws IOException {
		writer.write(key.getClassName());
		writer.write(",");
		writer.write(key.getName());
		writer.write(",");
		writer.write(key.getSignature());
		writer.write(",");
		writer.write(String.valueOf(key.getAccessFlags()));
	}

}
