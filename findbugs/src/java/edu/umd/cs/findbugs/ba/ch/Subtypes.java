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
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;

public class Subtypes {
	private static final boolean DEBUG_HIERARCHY = false || Boolean
			.getBoolean("findbugs.debug.hierarchy");

	private boolean computed = false;

	private Set<String> referenced = new HashSet<String>();

	private Set<JavaClass> allClasses = new HashSet<JavaClass>();

	private Set<JavaClass> applicationClasses = new HashSet<JavaClass>();

	private Set<JavaClass> parentsAdded = new HashSet<JavaClass>();

	private Map<JavaClass, Set<JavaClass>> immediateSubtypes = new HashMap<JavaClass, Set<JavaClass>>();

	private Map<JavaClass, Set<JavaClass>> transitiveSubtypes = new HashMap<JavaClass, Set<JavaClass>>();

	public Set<JavaClass> getImmediateSubtypes(JavaClass c) {
		if (!allClasses.contains(c))
			addClass(c);
		compute();
		return immediateSubtypes.get(c);
	}

	public Set<JavaClass> getAllClasses() {
		compute();
		return allClasses;
	}

	public Set<JavaClass> getTransitiveSubtypes(JavaClass c) {
		if (!allClasses.contains(c))
			addClass(c);
		compute();
		return transitiveSubtypes.get(c);
	}

	private Set<JavaClass> getReferencedClasses(JavaClass c) {
		Set<JavaClass> result = new HashSet<JavaClass>();
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
				String name = ((ConstantClass) co).getBytes(cp);
				name = extractClassName(name);
				if (DEBUG_HIERARCHY)
					System.out.println(i + " : " + name);
				addNamedClass(result, name);
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
					addNamedClass(result, name);
					sig = sig.substring(k + 1);
				}

			}
		}
		return result;
	}

	private void addNamedClass(Set<JavaClass> result, String name) {
		name = name.replace('/', '.');

		if (referenced.add(name))
			try {
				
				JavaClass clazz = Repository.lookupClass(name);
				result.add(clazz);
			} catch (ClassNotFoundException e) {
				if (name.length() > 1)
				AnalysisContext.currentAnalysisContext()
						.getLookupFailureCallback().reportMissingClass(e);
			}
	}

	public void addApplicationClass(JavaClass c) {
		if (c == null)
			return;
		if (DEBUG_HIERARCHY)
			System.out.println("Adding application class " + c.getClassName());
		if (applicationClasses.add(c)) {
			if (DEBUG_HIERARCHY && computed)
				System.out.println("Need to recompute");
			computed = false;
		}

	}

	public void addClass(JavaClass c) {
		if (c == null)
			return;
		if (allClasses.add(c)) {
			immediateSubtypes.put(c, new HashSet<JavaClass>());
			if (DEBUG_HIERARCHY && computed)
				System.out.println("Need to recompute");
			computed = false;
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
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback()
					.reportMissingClass(e);
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

	public void compute() {
		if (computed) return;
		if (DEBUG_HIERARCHY)
			System.out.println("Computing {");
		immediateSubtypes.clear();
		transitiveSubtypes.clear();
		parentsAdded.clear();
		for (JavaClass c : applicationClasses) {
			addClass(c);
			for (JavaClass r : getReferencedClasses(c))
				addClass(r);
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
			System.out.println(" compute " + c.getClassName() + " : "
					+ immediateSubtypes.get(c).size());
		Set<JavaClass> descendents = transitiveSubtypes.get(c);
		if (descendents != null) {
			if (!descendents.contains(c))
				System.out.println("This is wrong for " + c.getClassName());
			return descendents;
		}
		descendents = new HashSet<JavaClass>();
		descendents.add(c);
		transitiveSubtypes.put(c, descendents);
		for (JavaClass child : immediateSubtypes.get(c)) {
			descendents.add(c);
			descendents.addAll(compute(child));
		}
		if (DEBUG_HIERARCHY)
			System.out.println(c.getClassName() + " has " + descendents.size()
					+ " decendents");
		return descendents;
	}

	public static String extractClassName(String name) {
		if (name.charAt(0) != '[' && name.charAt(name.length() - 1) != ';')
			return name;
		while (name.charAt(0) == '[')
			name = name.substring(1);
		if (name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';')
			name = name.substring(1, name.length() - 1);
		return name;
	}
}
