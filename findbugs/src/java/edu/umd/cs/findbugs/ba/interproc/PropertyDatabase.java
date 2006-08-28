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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassMember;

/**
 * Property database for interprocedural analysis.
 * 
 * @param <KeyType>   key type: either XMethod or XField
 * @param <ValueType> value type: a value that summarizes some property of the associated key
 * @author David Hovemeyer
 */
public abstract class PropertyDatabase<KeyType extends ClassMember, ValueType> {
	private Map<KeyType, ValueType> propertyMap;
	
	/**
	 * Constructor.
	 * Creates an empty property database. 
	 */
	protected PropertyDatabase() {
		this.propertyMap = new HashMap<KeyType, ValueType>();
	}
	
	/**
	 * Set a property.
	 * 
	 * @param key   the key
	 * @param property the property
	 */
	public void setProperty(KeyType key, ValueType property) {
		propertyMap.put(key, property);
	}
	
	/**
	 * Get a property.
	 * 
	 * @param key the key
	 * @return the property, or null if no property is set for this key
	 */
	public ValueType getProperty(KeyType key) {
		return propertyMap.get(key);
	}
	
	/**
	 * Return whether or not the database is empty.
	 * 
	 * @return true if the database is empty, false it it has at least one entry
	 */
	public boolean isEmpty() {
		return propertyMap.isEmpty();
	}
	
	/**
	 * Remove a property.
	 * 
	 * @param key the key
	 * @return the old property, or null if there was no property defined for this key
	 */
	public ValueType removeProperty(KeyType key) {
		return propertyMap.remove(key);
	}
	
	/**
	 * Read property database from given file.
	 * 
	 * @param fileName name of the database file
	 * @throws IOException
	 * @throws MethodPropertyDatabaseFormatException
	 */
	public void readFromFile(String fileName) throws IOException, PropertyDatabaseFormatException {
		read(new FileInputStream(fileName));
	}

	/**
	 * Read property database from an input stream.
	 * The InputStream is guaranteed to be closed, even if an
	 * exception is thrown.
	 * 
	 * @param in the InputStream
	 * @throws IOException
	 * @throws MethodPropertyDatabaseFormatException
	 */
	public void read(InputStream in) throws IOException, PropertyDatabaseFormatException {
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
					throw new PropertyDatabaseFormatException(
							"Invalid property database: missing separator");
				}
				KeyType key = parseKey(line.substring(0, bar));
				ValueType property = decodeProperty(line.substring(bar+1));
				
				setProperty(key, property);
			}
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}
	
	/**
	 * Write property database to given file.
	 * 
	 * @param fileName name of the database file
	 * @throws IOException
	 */
	public void writeToFile(String fileName) throws IOException {
		write(new FileOutputStream(fileName));
	}

	/**
	 * Write property database to an OutputStream.
	 * The OutputStream is guaranteed to be closed, even if an
	 * exception is thrown.
	 * 
	 * @param out the OutputStream
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		BufferedWriter writer = null;
		
		try {
			writer = new BufferedWriter(
					new OutputStreamWriter(out, Charset.forName("UTF-8")));
			
			TreeSet<KeyType> sortedMethodSet = new TreeSet<KeyType>();
			sortedMethodSet.addAll(propertyMap.keySet());
			for (KeyType key : sortedMethodSet) {
				if (AnalysisContext.currentAnalysisContext().isApplicationClass(key.getClassName())) {
				ValueType property = propertyMap.get(key);
				writeKey(writer, key);
				writer.write("|");
				writer.write(encodeProperty(property));
				writer.write("\n");
				}
			}
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Parse a key from a String.
	 * 
	 * @param s a String
	 * @return the decoded key
	 * @throws PropertyDatabaseFormatException
	 */
	protected abstract KeyType parseKey(String s) throws PropertyDatabaseFormatException;

	/**
	 * Write an encoded key to given Writer.
	 * 
	 * @param writer the Writer
	 * @param key    the key
	 */
	protected abstract void writeKey(Writer writer, KeyType key) throws IOException;

	/**
	 * Subclasses must define this to instantiate the actual property
	 * value from its string encoding.
	 * 
	 * @param propStr String containing the encoded property
	 * @return the property
	 * @throws MethodPropertyDatabaseFormatException
	 */
	protected abstract ValueType decodeProperty(String propStr)
		throws PropertyDatabaseFormatException;

	/**
	 * Subclasses must define this to encode a property
	 * as a string for output to a file.
	 * 
	 * @param property the property
	 * @return a String which encodes the property
	 */
	protected abstract String encodeProperty(ValueType property);

}
