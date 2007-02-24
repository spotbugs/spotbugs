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
package edu.umd.cs.findbugs.ba.interproc;

import java.io.IOException;
import java.io.Writer;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.InstanceMethod;
import edu.umd.cs.findbugs.ba.StaticMethod;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * A MethodPropertyDatabase keeps track of properties of
 * methods.  This is useful for implementing interprocedural analyses.
 * 
 * @author David Hovemeyer
 */
public abstract class MethodPropertyDatabase<Property>
		extends PropertyDatabase<XMethod, Property> {

	@Override
         protected XMethod parseKey(String methodStr) throws PropertyDatabaseFormatException {
		String[] tuple = methodStr.split(",");
		if (tuple.length != 4)
			throw new PropertyDatabaseFormatException("Invalid method tuple: " + methodStr);
		
		try {
            int accessFlags = Integer.parseInt(tuple[3]);
            return XFactory.createXMethod(tuple[0], tuple[1], tuple[2], accessFlags);

		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
         protected void writeKey(Writer writer, XMethod method) throws IOException {
		writer.write(method.getClassName());
		writer.write(",");
		writer.write(method.getName());
		writer.write(",");
		writer.write(method.getSignature());
		writer.write(",");
		writer.write(String.valueOf(method.getAccessFlags()));
	}
}
