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
import java.util.TreeMap;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Global information about the source code for an application.
 * Currently, this object contains a map of source line information
 * for fields and classes (items we don't get line number information
 * for directly in classfiles), and also source line information
 * for methods that don't appear directly in classfiles,
 * such as abstract and native methods.
 * 
 * @author David Hovemeyer
 */
public class SourceInfoMap {
	static class FieldDescriptor implements Comparable<FieldDescriptor> {
		String className;
		String fieldName;
		
		public FieldDescriptor(String className, String fieldName) {
			this.className = className;
			this.fieldName = fieldName;
		}
		
		public String toString() {
			return className +"." + fieldName;
		}
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(T)
		 */
		public int compareTo(FieldDescriptor o) {
			int cmp = className.compareTo(o.className);
			if (cmp != 0)
				return cmp;
			return fieldName.compareTo(o.fieldName);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return 1277 * className.hashCode() +fieldName.hashCode(); 
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			FieldDescriptor other = (FieldDescriptor) obj;
			return className.equals(other.className) && fieldName.equals(other.fieldName);
		}
	}
	
	static class MethodDescriptor implements Comparable<MethodDescriptor> {
		private String className;
		private String methodName;
		private String methodSignature;
		
		public MethodDescriptor(String className, String methodName, String methodSignature) {
			this.className = className;
			this.methodName = methodName;
			this.methodSignature = methodSignature;
		}
		public String toString() {
			return className +"." + methodName;
		}
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(T)
		 */
		public int compareTo(MethodDescriptor o) {
			int cmp;
			if ((cmp = className.compareTo(o.className)) != 0)
				return cmp;
			if ((cmp = methodName.compareTo(o.methodName)) != 0)
				return cmp;
			return methodSignature.compareTo(o.methodSignature);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return 1277 * className.hashCode()
				+ 37 * methodName.hashCode()
				+ methodSignature.hashCode();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			MethodDescriptor other = (MethodDescriptor) obj;
			return className.equals(other.className)
				&& methodName.equals(other.methodName)
				&& methodSignature.equals(other.methodSignature);
		}
	}
	
	/**
	 * A range of source lines.
	 */
	public static class SourceLineRange {
		private final Integer start, end;

		/**
		 * Constructor for a single line.
		 */
		public SourceLineRange(@NonNull Integer line) {
			this.start = this.end = line;
		}
		
		/**
		 * Constructor for a range of lines.
		 * 
		 * @param start start line in range
		 * @param end   end line in range
		 */
		public SourceLineRange(@NonNull Integer start, @NonNull Integer end) {
			this.start = start;
			this.end = end;
		}
		
		/**
		 * @return Returns the start.
		 */
		public @NonNull Integer getStart() {
			return start;
		}
		
		/**
		 * @return Returns the end.
		 */
		public @NonNull Integer getEnd() {
			return end;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return start + (start.equals(end) ? "" : "-" + end);
		}
	}
	
	private static final boolean DEBUG = SystemProperties.getBoolean("sourceinfo.debug");
	
	private Map<FieldDescriptor, SourceLineRange> fieldLineMap;
	private Map<MethodDescriptor, SourceLineRange> methodLineMap;
	private Map<String, SourceLineRange> classLineMap;
	
	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SourceInfoMap() {
		this.fieldLineMap = new HashMap<FieldDescriptor, SourceLineRange>();
		this.methodLineMap = new HashMap<MethodDescriptor, SourceLineRange>();
		this.classLineMap = new HashMap<String, SourceLineRange>();
	}
	
	/**
	 * Add a line number entry for a field.
	 * 
	 * @param className name of class containing the field
	 * @param fieldName name of field
	 * @param range      the line number(s) of the field
	 */
	public void addFieldLine(String className, String fieldName, SourceLineRange range) {
		fieldLineMap.put(new FieldDescriptor(className, fieldName), range);
	}
	
	/**
	 * Add a line number entry for a method.
	 * 
	 * @param className       name of class containing the method
	 * @param methodName      name of method
	 * @param methodSignature signature of method
	 * @param range           the line number of the method
	 */
	public void addMethodLine(String className, String methodName, String methodSignature,
			SourceLineRange range) {
		methodLineMap.put(new MethodDescriptor(className, methodName, methodSignature), range);
	}
	
	/**
	 * Add line number entry for a class.
	 * 
	 * @param className name of class
	 * @param range      the line numbers of the class
	 */
	public void addClassLine(String className, SourceLineRange range) {
		classLineMap.put(className, range);
	}
	
	/**
	 * Look up the line number range for a field.
	 * 
	 * @param className name of class containing the field
	 * @param fieldName name of field
	 * @return the line number range, or null if no line number is known for the field
	 */
	public @CheckForNull SourceLineRange getFieldLine(String className, String fieldName) {
		return fieldLineMap.get(new FieldDescriptor(className, fieldName));
	}
	
	/**
	 * Look up the line number range for a method.
	 * 
	 * @param className       name of class containing the method
	 * @param methodName      name of method
	 * @param methodSignature signature of method
	 * @return the line number range, or null if no line number is known for the method
	 */
	public @CheckForNull SourceLineRange getMethodLine(String className, String methodName, String methodSignature) {
		return methodLineMap.get(new MethodDescriptor(className, methodName, methodSignature));
	}
	
	/**
	 * Look up the line number range for a class.
	 * 
	 * @param className name of the class
	 * @return the line number range, or null if no line number is known for the class
	 */
	public @CheckForNull SourceLineRange getClassLine(String className) {
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
			int lparen;
			String version;
			
			while ((line = reader.readLine()) != null) {
				++lineNumber;
				
				if (lineNumber == 1) {
					if (DEBUG) System.out.println("First line: " + line);
					// Try to parse the version number string from the first line.
					// null means that the line does not appear to be a version number.
					version = parseVersionNumber(line);
					if (version != null) {
						// Check to see if version is supported.
						// Only 1.0 supported for now.
						if (!version.equals("1.0"))
							throw new IOException("Unsupported sourceInfo version " + version);
						
						// Version looks good.  Skip to next line of file.
						continue;
					}
				}
				
				StringTokenizer tokenizer = new StringTokenizer(line, ",");
				
				String className = tokenizer.nextToken();
				String next = tokenizer.nextToken();
				if (DIGITS.matcher(next).matches()) {
					// Line number for class
					SourceLineRange range = createRange(next, tokenizer.nextToken());
					classLineMap.put(className, range);
					if (DEBUG) System.out.println("class:" + className + "," + range);
				} else if ((lparen = next.indexOf('(')) >= 0) {
					// Line number for method
					String methodName = next.substring(0, lparen);
					String methodSignature = next.substring(lparen);
					
					if (methodName.equals("init^"))
						methodName = "<init>";
					else if (methodName.equals("clinit^"))
						methodName = "<clinit>";
					
					SourceLineRange range = createRange(tokenizer.nextToken(), tokenizer.nextToken());
					methodLineMap.put(new MethodDescriptor(className, methodName, methodSignature), range);
					if (DEBUG) System.out.println("method:" + methodName+methodSignature + "," + range);
				} else {
					// Line number for field
					String fieldName = next;
					SourceLineRange range = createRange(tokenizer.nextToken(), tokenizer.nextToken());
					fieldLineMap.put(
							new FieldDescriptor(className, fieldName),
							range);
					if (DEBUG) System.out.println("field:" + className + "," +
							fieldName + "," + range);
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

	/**
	 * Parse the sourceInfo version string.
	 * 
	 * @param line the first line of the sourceInfo file
	 * @return the version number constant, or null if
	 *         the line does not appear to be a version string
	 */
	private static String parseVersionNumber(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line, " \t");

		if (!expect(tokenizer, "sourceInfo")
				|| !expect(tokenizer, "version")
				|| !tokenizer.hasMoreTokens())
			return null;
		
		return tokenizer.nextToken();
	}

	/**
	 * Expect a particular token string to be returned by the given
	 * StringTokenizer.
	 * 
	 * @param tokenizer the StringTokenizer
	 * @param token     the expectedToken
	 * @return true if the expected token was returned, false if not
	 */
	private static boolean expect(StringTokenizer tokenizer, String token) {
		if (!tokenizer.hasMoreTokens())
			return false;
		String s = tokenizer.nextToken();
		if (DEBUG) System.out.println("token="+s);
		return s.equals(token);
	}

	private static SourceLineRange createRange(String start, String end) {
		return new SourceLineRange(Integer.valueOf(start), Integer.valueOf(end));
	}
}
