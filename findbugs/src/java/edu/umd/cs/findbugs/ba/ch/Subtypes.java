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


import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;




public class Subtypes {
	private static final boolean DEBUG_HIERARCHY = Boolean.getBoolean("findbugs.debug.hierarchy");

	boolean computed = false;
	Set<String> referenced = new HashSet<String>();
	Set<JavaClass> handled = new HashSet<JavaClass>();
	Set<JavaClass> parentsAdded = new HashSet<JavaClass>();
	Map<JavaClass,Set<JavaClass>> immediateSubtypes =
		new HashMap<JavaClass,Set<JavaClass>>();
	Map<JavaClass,Set<JavaClass>> transitiveSubtypes =
		new HashMap<JavaClass,Set<JavaClass>>();

	public Set<JavaClass> getImmediateSubtypes(JavaClass c) {
		if (!handled.contains(c)) addClass(c);
		if (!computed) compute();
		return immediateSubtypes.get(c);
		}

	public Set<JavaClass> getTransitiveSubtypes(JavaClass c) {
		if (!handled.contains(c)) addClass(c);
		if (!computed) compute();
		return transitiveSubtypes.get(c);
		}

	public Set<JavaClass> getReferencedClasses(JavaClass c) {
		Set<JavaClass> result = new HashSet<JavaClass>();
		ConstantPool cp = c.getConstantPool();
		Constant[] constants = cp.getConstantPool();
		for(int i = 0; i < constants.length; i++) {
		    Constant co = constants[i];
		    if (co instanceof ConstantDouble 
				|| co instanceof ConstantLong)
			i++;
		    if (co instanceof ConstantClass) {
			String name = ((ConstantClass)co).getBytes(cp);
			if (name.charAt(0) != '[' && referenced.add(name))
			try {
			JavaClass clazz = Repository.lookupClass(name);
			result.add(clazz);
			} catch (ClassNotFoundException e) {}
			}
		}
		return result;
		}
		
	public void addApplicationClass(JavaClass c) {
		if (c == null) return;
		addClass(c);
		for(JavaClass x : getReferencedClasses(c))
			addClass(c);
		}
	public void addClass(JavaClass c) {
		if (c == null) return;
		if (!handled.add(c)) return;
		if (DEBUG_HIERARCHY) 
		System.out.println("Adding " + c.getClassName());
		Set<JavaClass> children = immediateSubtypes.get(c);
		if (children == null)
			immediateSubtypes.put(c, new HashSet<JavaClass>());
		if (DEBUG_HIERARCHY && computed) 
			System.out.println("Recomputing");
		computed = false;
		}

	public void addParents(JavaClass c) {
		if (!parentsAdded.add(c)) return;
		try {
		addParent(c.getSuperClass(),c);
		for(JavaClass i : c.getInterfaces())
			addParent(i,c);
		} catch (ClassNotFoundException e) {}
		}
	
	public void addParent(JavaClass p, JavaClass c) {
		if (p == null) return;
		if (DEBUG_HIERARCHY) 
		System.out.println("adding " + c.getClassName()
				+ " is a " + p.getClassName());
		addClass(p);
		addParents(p);
		Set<JavaClass> children = immediateSubtypes.get(p);
		children.add(c);
		}

	public void compute() {
		if (DEBUG_HIERARCHY) 
		System.out.println("Computing");
		immediateSubtypes.clear();
		transitiveSubtypes.clear();
		parentsAdded.clear();
		for(JavaClass c : handled)  {
			immediateSubtypes.put(c, new HashSet<JavaClass>());
			}
		for(JavaClass c : new HashSet<JavaClass>(handled))  {
			addParents(c);
			}
		for(JavaClass c : handled)  {
			compute(c);
			}
		computed = true;
		}

	public Set<JavaClass> compute(JavaClass c) {
		Set<JavaClass> descendents = transitiveSubtypes.get(c);
		if (descendents != null) return descendents;
		descendents = new HashSet<JavaClass>();
		descendents.add(c);
		transitiveSubtypes.put(c, descendents);
		for(JavaClass child : immediateSubtypes.get(c)) 
			descendents.addAll(compute(child));
		if (DEBUG_HIERARCHY) 
		System.out.println(c.getClassName() + " has " + descendents.size()
				+ " decendents");
		return descendents;
		}
	}
		
