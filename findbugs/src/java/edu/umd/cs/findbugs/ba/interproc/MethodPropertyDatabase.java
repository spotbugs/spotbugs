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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.InstanceMethod;
import edu.umd.cs.findbugs.ba.StaticMethod;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes;

/**
 * A MethodPropertyDatabase keeps track of properties of
 * methods.  This is useful for implementing interprocedural analyses.
 * 
 * @author David Hovemeyer
 */
public abstract class MethodPropertyDatabase<Property extends MethodProperty<Property>> {
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
	 * Propagate method properties through the class hierarchy,
	 * <em>for instance methods only</em>.
	 * This step ensures that method overrides are taken into account.
	 * Depending on the kind of properties, this might work from supertypes
	 * to subtypes, or from subtypes to supertypes.
	 */
	public void propagateThroughClassHierarchy() {
		Subtypes subtypes = AnalysisContext.currentAnalysisContext().getSubtypes();
		
		Set<XMethod> methodSet = new HashSet<XMethod>();
		methodSet.addAll(propertyMap.keySet());
		
		// Step 1.  Add all target methods (overridden methods or overriding methods,
		// depending on which direction we're walking the class hierarchy in.)
		for (Iterator<XMethod> i = propertyMap.keySet().iterator(); i.hasNext();) {
			XMethod sourceMethod = i.next();
			if (sourceMethod.isStatic())
				continue;
			
			JavaClass sourceClass = getClassFor(sourceMethod);
			if (sourceClass == null)
				continue;
			
			Set<XMethod> targetMethods = getTargetMethods(sourceClass, sourceMethod);
			methodSet.addAll(targetMethods);
		}
		
		// Step 2.  Make sure the database has an entry for all reachable methods.
		for (Iterator<XMethod> i = methodSet.iterator(); i.hasNext();) {
			XMethod xmethod = i.next();
			if (propertyMap.get(xmethod) == null) {
				propertyMap.put(xmethod, createDefault());
			}
		}
		
		// Step 3.  Propagate method properties to super or subtype methods.
		// As long as the combine operation is commutative, it doesn't matter
		// what order we do this in.
		for (Iterator<Map.Entry<XMethod, Property>> i = propertyMap.entrySet().iterator(); i.hasNext();) {
			Map.Entry<XMethod, Property> entry = i.next();
			XMethod sourceMethod = entry.getKey();
			JavaClass sourceClass = getClassFor(sourceMethod);
			if (sourceClass == null)
				continue;
			
			Property sourceProperty = entry.getValue();
			
			Set<XMethod> targetMethodSet = getTargetMethods(sourceClass, sourceMethod);
			for (Iterator<XMethod> j = targetMethodSet.iterator(); j.hasNext();) {
				XMethod targetMethod = j.next();
				if (targetMethod.isStatic())
					continue;
				
				Property targetProperty = propertyMap.get(targetMethod);
				Property result = getPropertyCombinator().combine(sourceProperty, targetProperty);
				targetProperty.makeSameAs(result);
			}
		}
	}

	private JavaClass getClassFor(XMethod method) {
		AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
		try {
			JavaClass javaClass = analysisContext.lookupClass(method.getClassName());
			return javaClass;
		} catch (ClassNotFoundException e) {
			analysisContext.getLookupFailureCallback().reportMissingClass(e);
			return null;
		}
	}
	
	private static final Set<XMethod> EMPTY_METHOD_SET = new HashSet<XMethod>();

	private Set<XMethod> getTargetMethods(JavaClass sourceClass, XMethod sourceMethod) {
		try {
			Set<XMethod> result = new HashSet<XMethod>();
			Set<JavaClass> targetClassSet = getHierarchyWalkDirection().getHierarchyGraphTargets(sourceClass);
			for (Iterator<JavaClass> j = targetClassSet.iterator(); j.hasNext(); ) {
				JavaClass targetClass = j.next();
				
				XMethod targetMethod = Hierarchy.findXMethod(targetClass, sourceMethod.getName(), sourceMethod.getSignature());
				if (targetMethod == null)
					return EMPTY_METHOD_SET;
				if (targetMethod.isStatic())
					return EMPTY_METHOD_SET;
				
				result.add(targetMethod);
			}
			
			return result;
		} catch (ClassNotFoundException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			return EMPTY_METHOD_SET;
		}
	}
	
	/**
	 * Read property database from given file.
	 * 
	 * @param fileName name of the database file
	 * @throws IOException
	 * @throws MethodPropertyDatabaseFormatException
	 */
	public void readFromFile(String fileName) throws IOException, MethodPropertyDatabaseFormatException {
		read(new FileInputStream(fileName));
	}

	/**
	 * Read method property database from an input stream.
	 * The InputStream is guaranteed to be closed, even if an
	 * exception is thrown.
	 * 
	 * @param in the InputStream
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
				XMethod method = parseMethod(line.substring(0, bar));
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
	 * Write property database to given file.
	 * 
	 * @param fileName name of the database file
	 * @throws IOException
	 */
	public void writeToFile(String fileName) throws IOException {
		write(new FileOutputStream(fileName));
	}

	/**
	 * Write method property database to an OutputStream.
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
	 * Create a default property.
	 */
	protected abstract Property createDefault();

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
	
	/**
	 * Get the direction in which to walk the class hierarchy when
	 * propagating method properties.
	 * 
	 * @return the HierarchyWalkDirection
	 */
	protected abstract HierarchyWalkDirection getHierarchyWalkDirection();
	
	/**
	 * Get the PropertyCombinator used to combine method properties
	 * when propagating them in the class hierarchy.
	 * 
	 * @return
	 */
	protected abstract PropertyCombinator<Property> getPropertyCombinator();
}
