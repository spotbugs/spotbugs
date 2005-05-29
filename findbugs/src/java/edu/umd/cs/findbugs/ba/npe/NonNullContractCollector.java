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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.JavaClassAndMethodChooser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;

/**
 * Find the contract specified by @NonNull and @PossiblyNull parameter
 * annotations in the class hierarchy.  Also, check null argument sets
 * for violations.
 * 
 * <p>TODO: this code could be generalized for other kinds of parameter annotations</p>
 * 
 * @author David Hovemeyer
 */
public class NonNullContractCollector implements JavaClassAndMethodChooser {
	private static final boolean DEBUG_NULLARG = Boolean.getBoolean("fnd.debug.nullarg");

	private final NonNullParamPropertyDatabase nonNullParamDatabase;
	private final NonNullParamPropertyDatabase possiblyNullParamDatabase;
	private final List<NonNullSpecification> specificationList;

	public NonNullContractCollector(NonNullParamPropertyDatabase nonNullParamDatabase, NonNullParamPropertyDatabase possiblyNullParamDatabase) {
		super();
		this.nonNullParamDatabase = nonNullParamDatabase;
		this.possiblyNullParamDatabase = possiblyNullParamDatabase;
		this.specificationList = new LinkedList<NonNullSpecification>();
	}

	public boolean choose(JavaClassAndMethod classAndMethod) {
		XMethod xmethod = XMethodFactory.createXMethod(
				classAndMethod.getJavaClass(), classAndMethod.getMethod());

		NonNullSpecification specification = new NonNullSpecification(
				classAndMethod,
				wrapProperty(nonNullParamDatabase.getProperty(xmethod)),
				wrapProperty(possiblyNullParamDatabase.getProperty(xmethod)));
		if (true) {
			System.out.println("Found specification: " + specification);
		}
		specificationList.add(specification);
		
		return false;
	}
	
	public void findContractForCallSite(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) throws ClassNotFoundException {
		Hierarchy.findInvocationLeastUpperBound(invokeInstruction, cpg, this);
	}
	
	public void getViolationList(int numParams, BitSet nullArgSet, List<NonNullParamViolation> violationList, BitSet violatedParamSet) {
		BitSet checkedParams = new BitSet();
		for (NonNullSpecification specification : specificationList) {
			if (DEBUG_NULLARG) {
				System.out.println("Check specification: " + specification);
			}
			
		paramLoop:
			for (int i = 0; i < numParams; ++i) {
				if (DEBUG_NULLARG) {
					System.out.print("\tParam " + i);
				}

				if (!nullArgSet.get(i)) {
					if (DEBUG_NULLARG) System.out.println(" ==> not null");
					continue paramLoop;
				}
				
				if (checkedParams.get(i)) {
					if (DEBUG_NULLARG) System.out.println(" ==> already checked");
					continue paramLoop;
				}
				
				// Arg is null, and we haven't seen a specification for the parameter yet.
				// See if this method defines a specification.
				if (specification.getPossiblyNullProperty().isNonNull(i)) {
					// Parameter declared @PossiblyNull.
					// So it's OK to pass null.
					if (DEBUG_NULLARG) System.out.println(" ==> @PossiblyNull");
					checkedParams.set(i);
				} else if (specification.getNonNullProperty().isNonNull(i)) {
					// Parameter declated @NonNull.
					// This is a violation.
					if (DEBUG_NULLARG) System.out.println(" ==> @NonNull violation!");
					violationList.add(new NonNullParamViolation(specification.getClassAndMethod(), i));
					violatedParamSet.set(i);
					checkedParams.set(i);
				} else {
					if (DEBUG_NULLARG) System.out.println(" ==> no constraint");
				}
			}
		}
	}
	
	public List<NonNullSpecification> getSpecificationList() {
		return specificationList;
	}
	
	static NonNullParamProperty wrapProperty(NonNullParamProperty property) {
		return property != null ? property : new NonNullParamProperty();
	}
}
