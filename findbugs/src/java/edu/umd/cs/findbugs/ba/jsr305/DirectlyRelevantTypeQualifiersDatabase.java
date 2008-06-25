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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

/**
 * Database of type qualfiers applied directly to methods.
 * 
 * @author William Pugh
 * @author David Hovemeyer
 */
public class DirectlyRelevantTypeQualifiersDatabase {
	
	private Map<MethodDescriptor, Collection<TypeQualifierValue>> methodToDirectlyRelevantQualifiersMap;
	private Set<TypeQualifierValue> allKnownQualifiers;
	
	/*
	 * Constructor.
	 */
	public DirectlyRelevantTypeQualifiersDatabase() {
		methodToDirectlyRelevantQualifiersMap = new HashMap<MethodDescriptor, Collection<TypeQualifierValue>> ();
		allKnownQualifiers = new HashSet<TypeQualifierValue>();
	}

	/**
	 * Get the directly-relevant type qualifiers applied to given
	 * method.
	 * 
	 * @param m MethodDescriptor identifying a method
	 * @return Collection of type qualifiers applied directly to that method
	 */
	public Collection<TypeQualifierValue> getDirectlyRelevantTypeQualifiers(MethodDescriptor m) {
		Collection<TypeQualifierValue> result = methodToDirectlyRelevantQualifiersMap.get(m);
		if (result != null) 
			return result;
		return Collections.<TypeQualifierValue>emptyList();
	}
	
	/**
	 * Return a set of all known type qualifiers.
	 * 
     * @return set of all known type qualifiers
     */
    public Set<TypeQualifierValue> getAllKnownQualifiers() {
	    return Collections.<TypeQualifierValue>unmodifiableSet(allKnownQualifiers);
    }

	/**
	 * Set the collection of directly-relevant type qualifiers for
	 * a given method. 
	 * 
     * @param methodDescriptor MethodDescriptor identifying a method
     * @param qualifiers       collection of directly-relevant type qualifiers for the method
     */
    public void setDirectlyRelevantTypeQualifiers(MethodDescriptor methodDescriptor, Collection<TypeQualifierValue> qualifiers) {
    	methodToDirectlyRelevantQualifiersMap.put(methodDescriptor, qualifiers);
    	allKnownQualifiers.addAll(qualifiers);
    }
}
