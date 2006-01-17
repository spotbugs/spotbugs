/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, David Hovemeyer <daveho@users.sourceforge.net>
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

package edu.umd.cs.findbugs.ba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Global information about the source code for an application.
 * Currently, this is just a map of source line information
 * for fields and classes (items we don't get line number information
 * for directly in classfiles).
 * 
 * @author David Hovemeyer
 */
public class SourceInfoMap {
	static class ClassAndFieldName implements Comparable<ClassAndFieldName> {
		String className;
		String fieldName;
		
		public ClassAndFieldName(String className, String fieldName) {
			this.className = className;
			this.fieldName = fieldName;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(T)
		 */
		public int compareTo(ClassAndFieldName o) {
			int cmp = className.compareTo(o.className);
			if (cmp != 0)
				return cmp;
			return fieldName.compareTo(o.fieldName);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return 1277 * className.hashCode() +fieldName.hashCode(); 
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			ClassAndFieldName other = (ClassAndFieldName) obj;
			return className.equals(other.className) && fieldName.equals(other.fieldName);
		}
	}
	
	private static final boolean DEBUG = Boolean.getBoolean("sourceinfo.debug");
	
	private Map<ClassAndFieldName, Integer> fieldLineMap;
	private Map<String, Integer> classLineMap;
	
	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SourceInfoMap() {
		this.fieldLineMap = new HashMap<ClassAndFieldName, Integer>();
		this.classLineMap = new HashMap<String, Integer>();
	}
	
	/**
	 * Add a line number entry for a field.
	 * 
	 * @param className name of class containing the field
	 * @param fieldName name of field
	 * @param line      the line number of the field
	 */
	public void addFieldLine(String className, String fieldName, int line) {
		fieldLineMap.put(new ClassAndFieldName(className, fieldName), new Integer(line));
	}
	
	/**
	 * Add line number entry for a class.
	 * 
	 * @param className name of class
	 * @param line      the line number of the class
	 */
	public void addClassLine(String className, int line) {
		classLineMap.put(className, new Integer(line));
	}
	
	/**
	 * Look up the line number for a field.
	 * 
	 * @param className name of class containing the field
	 * @param fieldName name of field
	 * @return the line number, or null if no line number is known for the field
	 */
	public Integer getFieldLine(String className, String fieldName) {
		return fieldLineMap.get(new ClassAndFieldName(className, fieldName));
	}
	
	/**
	 * Look up the line number for a class.
	 * 
	 * @param className name of the class
	 * @return the line number, or null if no line number is known for the class
	 */
	public Integer getClassLine(String className) {
		return classLineMap.get(className);
	}
	
	private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
	
	/**
	 * Read source info from given InputStream.
	 * The stream is guaranteed to be closed.
	 * 
	 * @param inputStream  the InputStream
	 * @throws IOException if an I/O error occurs, or if the format is invalid
	 */
	public void read(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		int lineNumber = 0;
		try {
			String line;
			
			while ((line = reader.readLine()) != null) {
				++lineNumber;
				StringTokenizer tokenizer = new StringTokenizer(line, ",");
				
				String className = tokenizer.nextToken();
				String next = tokenizer.nextToken();
				if (DIGITS.matcher(next).matches()) {
					// Line number for class
					Integer value = Integer.valueOf(next);
					classLineMap.put(className, value);
					if (DEBUG) System.out.println("class:" + className + "," + value);
				} else {
					// Line number for field
					Integer value = Integer.valueOf(tokenizer.nextToken()); 
					fieldLineMap.put(
							new ClassAndFieldName(className, next),
							value);
					if (DEBUG) System.out.println("field:" + className + "," +
							next + "," + value);
				}
				
				// Note: we could complain if there are more tokens,
				// but instead we'll just ignore them.
			}
		} catch (NoSuchElementException e) {
			IOException ioe =
				new IOException("Invalid syntax in source info file at line " + lineNumber);
			ioe.initCause(e);
			throw ioe;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
