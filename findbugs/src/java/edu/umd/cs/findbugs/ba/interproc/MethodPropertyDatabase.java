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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.InstanceMethod;
import edu.umd.cs.findbugs.ba.StaticMethod;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * A MethodPropertyDatabase keeps track of properties of
 * methods.  This is useful for implementing interprocedural analyses.
 * 
 * @author David Hovemeyer
 */
public abstract class MethodPropertyDatabase<Property> {
	private Map<XMethod, Property> propertyMap;
	
	/**
	 * Constructor.
	 * Creates an empty method property database. 
	 */
	protected MethodPropertyDatabase() {
		this.propertyMap = new HashMap<XMethod, Property>();
	}
	
	/**
	 * Set a method property.
	 * 
	 * @param method   the method
	 * @param property the property
	 */
	public void setProperty(XMethod method, Property property) {
		propertyMap.put(method, property);
	}
	
	/**
	 * Get a method property.
	 * 
	 * @param method the method
	 * @return the method property, or null if no property is set for this method
	 */
	public Property getProperty(XMethod method) {
		return propertyMap.get(method);
	}

	/**
	 * Read method property database from a file.
	 * 
	 * @param in an InputStream reading the file
	 * @throws IOException
	 * @throws MethodPropertyDatabaseFormatException
	 */
	public void read(InputStream in) throws IOException, MethodPropertyDatabaseFormatException {
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(
				new InputStreamReader(in, Charset.forName("UTF-8")));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.equals(""))
					continue;
				int bar = line.indexOf('|');
				if (bar < 0) {
					throw new MethodPropertyDatabaseFormatException(
							"Invalid method property database: missing separator");
				}
				XMethod method = parseMethod(line.substring(bar));
				Property property = decodeProperty(line.substring(bar+1));
				
				setProperty(method, property);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Write method property database to a file.
	 * 
	 * @param out OutputStream writing to the file
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		BufferedWriter writer = null;
		
		try {
			writer = new BufferedWriter(
					new OutputStreamWriter(out, Charset.forName("UTF-8")));
			
			TreeSet<XMethod> sortedMethodSet = new TreeSet<XMethod>();
			sortedMethodSet.addAll(propertyMap.keySet());
			for (XMethod method : sortedMethodSet) {
				Property property = propertyMap.get(method);
				writeMethod(writer, method);
				writer.write("|");
				writer.write(encodeProperty(property));
				writer.write("\n");
			}
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	private XMethod parseMethod(String methodStr) throws MethodPropertyDatabaseFormatException {
		String[] tuple = methodStr.split(",");
		if (tuple.length != 4)
			throw new MethodPropertyDatabaseFormatException("Invalid method tuple: " + methodStr);
		
		try {
			int accessFlags = Integer.parseInt(tuple[3]);
			boolean isStatic = (accessFlags & Constants.ACC_STATIC) != 0;
			
			return isStatic
				? new StaticMethod(tuple[0], tuple[1], tuple[2], accessFlags)
				: new InstanceMethod(tuple[0], tuple[1], tuple[2], accessFlags);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void writeMethod(BufferedWriter writer, XMethod method) throws IOException {
		writer.write(method.getClassName());
		writer.write(",");
		writer.write(method.getName());
		writer.write(",");
		writer.write(method.getSignature());
		writer.write(",");
		writer.write(String.valueOf(method.getAccessFlags()));
	}

	/**
	 * Subclasses must define this to instantiate the actual property
	 * value from its string encoding.
	 * 
	 * @param propStr String containing the encoded method property
	 * @return the method property
	 * @throws MethodPropertyDatabaseFormatException
	 */
	protected abstract Property decodeProperty(String propStr)
		throws MethodPropertyDatabaseFormatException;

	/**
	 * Subclasses must define this to encode a property
	 * as a string for output to a file.
	 * 
	 * @param property the property
	 * @return a String which encodes the property
	 */
	protected abstract String encodeProperty(Property property);
}
