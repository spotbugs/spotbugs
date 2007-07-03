/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.util.HashSet;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.visitclass.BetterVisitor;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Database object to keep track of which methods, fields,
 * and classes are synthetic.
 * 
 * @author David Hovemeyer
 */
public class SyntheticElements {
	private HashSet<Object> syntheticElements;
	
	public SyntheticElements() {
		this.syntheticElements = new HashSet<Object>();
	}
	
	public void addVisitedMethod(PreorderVisitor visitor) {
		syntheticElements.add(XFactory.createXMethod(visitor));
	}
	
	public void addVisitedField(PreorderVisitor visitor) {
		syntheticElements.add(XFactory.createXField(visitor));
	}
	
	public void addVisitedClass(PreorderVisitor visitor) {
		syntheticElements.add(visitor.getDottedClassName());
	}
	
	public boolean isSynthetic(Object o) {
		return syntheticElements.contains(o);
	}
}
