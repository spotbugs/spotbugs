/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

package edu.umd.cs.findbugs.ba.ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;

/**
 * Support for class hierarchy queries.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class Subtypes {
	private static final boolean DEBUG_HIERARCHY = false || SystemProperties.getBoolean("findbugs.debug.hierarchy");

	private boolean computed = false;

	private Set<String> referenced = new HashSet<String>();

	private Set<JavaClass> allClasses = new HashSet<JavaClass>();

	private Set<JavaClass> applicationClasses = new HashSet<JavaClass>();

	private Set<JavaClass> parentsAdded = new HashSet<JavaClass>();

	private Map<JavaClass, Set<JavaClass>> immediateSubtypes = new HashMap<JavaClass, Set<JavaClass>>();

	private Map<JavaClass, Set<JavaClass>> transitiveSubtypes = new HashMap<JavaClass, Set<JavaClass>>();

	public Subtypes() {
	}

	/**
	 * Get immediate subtypes of given class or interface.
	 * 
	 * @param c a class or interface
	 * @return set of immediate subtypes
	 */
	public Set<JavaClass> getImmediateSubtypes(JavaClass c) {
		if (!allClasses.contains(c))
			addClass(c);
		compute();
		return immediateSubtypes.get(c);
	}

	/**
	 * Determine if a class or interface has subtypes
	 * 
	 * @param c a class or interface
	 * @return true if c has any subtypes/interfaces
	 */
	public boolean hasSubtypes(JavaClass c) {
		if (!allClasses.contains(c))
			addClass(c);
		compute();
		return !immediateSubtypes.get(c).isEmpty();
	}

	/**
	 * Get set of all known classes and interfaces. 
	 * 
	 * @return set of all known classes and interfaces
	 */
	public Set<JavaClass> getAllClasses() {
		compute();
		return allClasses;
	}

	/**
	 * Get set of all transitive subtypes of given class or interface,
	 * <em>not including the class or interface itself</em>.
	 * 
	 * @param c a class or interface
	 * @return set of all transitive subtypes
	 */
	public Set<JavaClass> getTransitiveSubtypes(JavaClass c) {
		if (!allClasses.contains(c))
			addClass(c);
		compute();
		return transitiveSubtypes.get(c);
	}

	/**
	 * Get set of all known transitive classes and interfaces which are subtypes of
	 * both of the given classes and/or interfaces.  Note that in this method,
	 * we consider a class to be a subtype of itself.  Therefore, this method
	 * can be used to determine, e.g., if there are any classes implementing
	 * both of two given interfaces.
	 * 
	 * @param a a class or interface
	 * @param b another class or interface
	 * @return set of all common subtypes of <i>a</i> and <i>b</i>
	 */
	public Set<JavaClass> getTransitiveCommonSubtypes(JavaClass a, JavaClass b) {
		Set<JavaClass> result = new HashSet<JavaClass>();

		// Get all subtypes of A, inclusive
		result.addAll(getTransitiveSubtypes(a));
		result.add(a);

		// Is B a subtype of A?
		boolean bIsSubtypeOfA = result.contains(b); 

		// Remove all subtypes of A which aren't proper subtypes of B
		result.retainAll(getTransitiveSubtypes(b));

		// Fix up: if B is a subtype of A, then we were wrong to
		// remove it.  Add it back if appropriate.
		if (bIsSubtypeOfA)
			result.add(b);

		return result;
	}

	private void addReferencedClasses(JavaClass c) {
		if (DEBUG_HIERARCHY)
			System.out.println("adding referenced classes for "
					+ c.getClassName());
		ConstantPool cp = c.getConstantPool();
		Constant[] constants = cp.getConstantPool();
		for (int i = 0; i < constants.length; i++) {
			Constant co = constants[i];
			if (co instanceof ConstantDouble || co instanceof ConstantLong)
				i++;
			if (co instanceof ConstantClass) {
				 String originalName = null;
				try {
				originalName = ((ConstantClass) co).getBytes(cp);
				String name = extractClassName(originalName);
				if (DEBUG_HIERARCHY)
					System.out.println(i + " : " + name);
				addNamedClass(name);
				} catch (RuntimeException e) {
					AnalysisContext.logError("Error extracting name from " + originalName, e);
				}
			} else if (co instanceof ConstantCP) {
				ConstantCP co2 = (ConstantCP) co;
				ConstantNameAndType nt = (ConstantNameAndType) cp
						.getConstant(co2.getNameAndTypeIndex());
				String sig = ((ConstantUtf8) cp.getConstant(nt
						.getSignatureIndex())).getBytes();
				while (true) {
					int j = sig.indexOf('L');
					if (j == -1)
						break;
					int k = sig.indexOf(';', j);
					String name = sig.substring(j + 1, k);
					if (DEBUG_HIERARCHY)
						System.out.println(i + " : " + name);
					addNamedClass(name);
					sig = sig.substring(k + 1);
				}

			}
		}
	}

	public static void learnFieldsAndMethods(JavaClass c) {
		for(Field f : c.getFields())
			XFactory.createXField(c, f);
		for(Method m : c.getMethods())
			XFactory.createXMethod(c, m);
	}
	public void addNamedClass(String name) {
		if (name.length() == 0 || name.charAt(0) == '[') {
			AnalysisContext.logError("Bad class name: " + name);
			return;
		}

		name = name.replace('/', '.');

		if (referenced.add(name))
			try {

				JavaClass clazz = Repository.lookupClass(name);
				learnFieldsAndMethods(clazz);
				addClass(clazz);
			} catch (ClassNotFoundException e) {

				if (name.length() > 1) {
					AnalysisContext.reportMissingClass(e);
					// e.printStackTrace(System.out);
				}
			}
	}

	public void addApplicationClass(JavaClass c) {
		if (c == null)
			return;
		if (DEBUG_HIERARCHY)
			System.out.println("Adding application class " + c.getClassName());
		if (applicationClasses.add(c)) {
			learnFieldsAndMethods(c);
			if (DEBUG_HIERARCHY && computed)
				System.out.println("Need to recompute");
			computed = false;
		}

	}

	public void addClass(JavaClass c) {
		if (c == null)
			return;
		if (allClasses.add(c)) {
			if (DEBUG_HIERARCHY)
				System.out.println("ADDING " + c.getClassName() + " " + System.identityHashCode(c) + " " + c.hashCode());

			immediateSubtypes.put(c, new HashSet<JavaClass>());
			if (DEBUG_HIERARCHY && computed)
				System.out.println("Need to recompute");
			computed = false;
		} else if (!immediateSubtypes.containsKey(c)) {
			if (DEBUG_HIERARCHY)
				System.out.println("INITIALIZING " + c.getClassName() + " " + System.identityHashCode(c) + " " + c.hashCode());
			immediateSubtypes.put(c, new HashSet<JavaClass>());
			}
	}

	private void addParents(JavaClass c) {
		if (!parentsAdded.add(c))
			return;
		try {
			addParent(c.getSuperClass(), c);
			for (JavaClass i : c.getInterfaces())
				addParent(i, c);
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
	}

	private void addParent(JavaClass p, JavaClass c) {
		if (p == null)
			return;
		if (DEBUG_HIERARCHY)
			System.out.println("adding " + c.getClassName() + " is a "
					+ p.getClassName());
		addClass(p);
		addParents(p);
		Set<JavaClass> children = immediateSubtypes.get(p);
		children.add(c);
	}

	private void compute() {
		if (computed) return;
		if (DEBUG_HIERARCHY)
			System.out.println("Computing {");
		transitiveSubtypes.clear();
		for (JavaClass c : applicationClasses) {
			addClass(c);
			addReferencedClasses(c);
		}

		for (JavaClass c : new HashSet<JavaClass>(allClasses)) {
			addParents(c);
		}
		for (JavaClass c : allClasses) {
			compute(c);
		}
		parentsAdded.clear();
		if (DEBUG_HIERARCHY)
			System.out.println("} Done Computing");
		computed = true;
	}

	private Set<JavaClass> compute(JavaClass c) {
		if (DEBUG_HIERARCHY)
			System.out.println(" compute " + c.getClassName()
			+ " " 
			+ System.identityHashCode(c)
			+ " " 
			+ c.hashCode()
			+ " " 
			+ (immediateSubtypes.get(c) == null 
			   ? " id is null" : " id is non null"));
		Set<JavaClass> descendents = transitiveSubtypes.get(c);
		if (descendents != null) {
			if (!descendents.contains(c))
				System.out.println("This is wrong for " + c.getClassName());
			return descendents;
		}
		descendents = new HashSet<JavaClass>();
		descendents.add(c);
		transitiveSubtypes.put(c, descendents);
		if (DEBUG_HIERARCHY)
			System.out.println("immediate subtypes of "
				+ c.getClassName()
				+ " " 
				+ System.identityHashCode(c)
				+ " " 
				+ c.hashCode()
				 + (immediateSubtypes.get(c) == null 
				   ? " is null" : " is non null"));

		for (JavaClass child : immediateSubtypes.get(c)) {
			if (DEBUG_HIERARCHY)
				System.out.println("Updating child " 
					+ child.getClassName()
					+ " of " 
					+ c.getClassName());
			descendents.addAll(compute(child));
		}
		if (DEBUG_HIERARCHY)
			System.out.println(c.getClassName() + " has " + descendents.size()
					+ " decendents");
		return descendents;
	}

	public static String extractClassName(String originalName) {
		String name = originalName;
		if (name.charAt(0) != '[' && name.charAt(name.length() - 1) != ';')
			return name;
		while (name.charAt(0) == '[')
			name = name.substring(1);
		if (name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';')
			name = name.substring(1, name.length() - 1);
		if (name.charAt(0) == '[') throw new IllegalArgumentException("Bad class name: " + originalName);
		return name;
	}

	/**
	 * Determine whether or not the given class is an application class.
	 * 
	 * @param javaClass a class
	 * @return true if it's an application class, false if not
	 */
	public boolean isApplicationClass(JavaClass javaClass) {
		boolean isAppClass = applicationClasses.contains(javaClass);
		if (DEBUG_HIERARCHY) {
			System.out.println(javaClass.getClassName() + " ==> " + (isAppClass ? "IS" : "IS NOT") + " an application class (" + applicationClasses.size() + " entries in app class map)");
		}
		return isAppClass;
	}
}
