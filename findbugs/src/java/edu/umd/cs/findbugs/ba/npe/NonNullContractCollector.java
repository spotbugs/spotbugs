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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.JavaClassAndMethodChooser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * Find the contract specified by @NonNull and @CheckForNull parameter
 * annotations in the class hierarchy.  Also, check null argument sets
 * for violations.
 * 
 * <p>TODO: this code could be generalized for other kinds of parameter annotations</p>
 * 
 * @author David Hovemeyer
 */
public class NonNullContractCollector implements JavaClassAndMethodChooser {
	private static final boolean DEBUG_NULLARG = SystemProperties.getBoolean("fnd.debug.nullarg");

	private final ParameterNullnessPropertyDatabase nonNullParamDatabase;
	private final ParameterNullnessPropertyDatabase possiblyNullParamDatabase;
	private final List<NonNullSpecification> specificationList;

	public NonNullContractCollector(ParameterNullnessPropertyDatabase nonNullParamDatabase, ParameterNullnessPropertyDatabase possiblyNullParamDatabase) {
		super();
		this.nonNullParamDatabase = nonNullParamDatabase;
		this.possiblyNullParamDatabase = possiblyNullParamDatabase;
		this.specificationList = new LinkedList<NonNullSpecification>();
	}

	public boolean choose(JavaClassAndMethod classAndMethod) {
		XMethod xmethod = XFactory.createXMethod(
				classAndMethod.getJavaClass(), classAndMethod.getMethod());

		NonNullSpecification specification = new NonNullSpecification(
				classAndMethod,
				wrapProperty(nonNullParamDatabase.getProperty(xmethod)),
				wrapProperty(possiblyNullParamDatabase.getProperty(xmethod)));
		if (DEBUG_NULLARG) {
			System.out.println("Found specification: " + specification);
		}
		specificationList.add(specification);
		
		return false;
	}
	
	public void findContractForCallSite(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) throws ClassNotFoundException {
		Hierarchy.findInvocationLeastUpperBound(invokeInstruction, cpg, this);
	}
	
	public void findContractForMethod(JavaClassAndMethod classAndMethod) throws ClassNotFoundException {
		String methodName = classAndMethod.getMethod().getName();
		String signature = classAndMethod.getMethod().getSignature();
		Hierarchy.findMethod(classAndMethod.getJavaClass(), methodName, signature, this);
		Hierarchy.visitSuperClassMethods(classAndMethod, this);
		Hierarchy.visitSuperInterfaceMethods(classAndMethod, this);
	}
	
	public interface SpecificationBuilder {
		public boolean checkParam(int param);
		public void setNonNullParam(int param, NonNullSpecification specification);
		public void setCheckForNullParam(int param, NonNullSpecification specification);
	}
	
	public void checkSpecifications(int numParams, SpecificationBuilder builder) {
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
				
				if (checkedParams.get(i)) {
					if (DEBUG_NULLARG) System.out.println(" ==> already checked");
					continue paramLoop;
				}
				
				if (!builder.checkParam(i))
					continue paramLoop;
				
				// Param should be checked, and we haven't seen a specification for the parameter yet.
				// See if this method defines a specification.
				if (specification.getCheckForNullProperty().isNonNull(i)) {
					// Parameter declared @CheckForNull.
					builder.setCheckForNullParam(i, specification);
					checkedParams.set(i);
				} else if (specification.getNonNullProperty().isNonNull(i)) {
					// Parameter declated @NonNull.
					builder.setNonNullParam(i, specification);
					checkedParams.set(i);
				} else {
					if (DEBUG_NULLARG) System.out.println(" ==> no constraint");
				}
			}
		}
	}
	
	public void getViolationList(
			int numParams,
			final BitSet nullArgSet,
			final List<NonNullParamViolation> violationList,
			final BitSet violatedParamSet) {
		
		SpecificationBuilder builder = new SpecificationBuilder() {
			public boolean checkParam(int param) {
				if (!argIsNull(param)) {
					if (DEBUG_NULLARG) System.out.println(" ==> not null");
					return false;
				}
				return true;
			}
			
			public void setNonNullParam(int param, NonNullSpecification specification) {
				if (DEBUG_NULLARG) System.out.println(" ==> @NonNull violation!");
				violationList.add(new NonNullParamViolation(specification.getClassAndMethod(), param));
				violatedParamSet.set(param);
			}
			
			public void setCheckForNullParam(int param, NonNullSpecification specification) {
				if (DEBUG_NULLARG) System.out.println(" ==> @CheckForNull");
			}
			
			private boolean argIsNull(int param) {
				return nullArgSet.get(param);
			}
		};
		checkSpecifications(numParams, builder);
	}
	
	public void getAnnotationSets(int numParams, final BitSet nonNullParamSet, final BitSet possiblyNullParamSet) {
		SpecificationBuilder builder = new SpecificationBuilder() {
			public boolean checkParam(int param) {
				return true;
			}
			
			public void setNonNullParam(int param, NonNullSpecification specification) {
				nonNullParamSet.set(param);
			}
			
			public void setCheckForNullParam(int param, NonNullSpecification specification) {
				possiblyNullParamSet.set(param);
			}
		};
		checkSpecifications(numParams, builder);
	}
	
	static ParameterNullnessProperty wrapProperty(ParameterNullnessProperty property) {
		return property != null ? property : new ParameterNullnessProperty();
	}
}
