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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.visitclass.BetterVisitor;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Database object to keep track of which methods, fields,
 * and classes are synthetic.
 * 
 * @author David Hovemeyer
 */
public class SyntheticElements {
	public static final boolean USE_SYNTHETIC_ELEMENTS_DB = SystemProperties.getBoolean("findbugs.useSEdb");
	
	private HashSet<Object> syntheticElements;
	
	/**
	 * Constructor.
	 */
	public SyntheticElements() {
		this.syntheticElements = new HashSet<Object>();
	}
	
	/**
	 * Mark the method being visited by given PreorderVisitor
	 * as synthetic.
	 * 
	 * @param visitor a PreorderVisitor visiting a method
	 */
	public void addVisitedMethod(PreorderVisitor visitor) {
		syntheticElements.add(XFactory.createXMethod(visitor));
	}

	/**
	 * Mark the field being visited by given PreorderVisitor
	 * as synthetic.
	 * 
	 * @param visitor a PreorderVisitor visiting a field
	 */
	public void addVisitedField(PreorderVisitor visitor) {
		syntheticElements.add(XFactory.createXField(visitor));
	}
	
	/**
	 * Mark the class being visited by given PreorderVisitor
	 * as synthetic.
	 * 
	 * @param visitor a PreorderVisitor visiting a class
	 */
	public void addVisitedClass(PreorderVisitor visitor) {
		syntheticElements.add(visitor.getDottedClassName());
	}
	
	/**
	 * Return whether or not the given object
	 * has been marked as synthetic.  The object must be
	 * one of the following:
	 * 
	 *  <ul>
	 *  <li>an XMethod (to find out if a method is synthetic)</li>
	 *  <li>an XField (to find out if a field is synthetic)</li>
	 *  <li>a String containing a dotted class name (to find out if a class is synthetic)</li>
	 *  </ul>
	 * 
	 * @param o an object (XMethod, XField, or String with dotted classname)
	 * @return true if element is synthetic, false otherwise
	 */
	public boolean isSynthetic(Object o) {
		return syntheticElements.contains(o);
	}
}
