/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class OverridingEqualsNotSymmetrical extends OpcodeStackDetector {

	private static final String EQUALS_NAME = "equals";

	private static final String EQUALS_SIGNATURE = "(Ljava/lang/Object;)Z";

	static enum KindOfEquals {
		OBJECT_EQUALS, ABSTRACT_INSTANCE_OF, INSTANCE_OF_EQUALS, CHECKED_CAST_EQUALS, RETURNS_SUPER, GETCLASS_GOOD_EQUALS, GETCLASS_BAD_EQUALS, DELEGATE_EQUALS, TRIVIAL_EQUALS, INVOKES_SUPER, ALWAYS_TRUE, ALWAYS_FALSE, UNKNOWN };
		
	Map<ClassAnnotation, KindOfEquals> kindMap = new HashMap<ClassAnnotation, KindOfEquals>();
	Map<ClassDescriptor,Set<ClassDescriptor>> classesWithGetClassBasedEquals = new HashMap<ClassDescriptor,Set<ClassDescriptor>>();
	Map<ClassAnnotation, ClassAnnotation> parentMap = new TreeMap<ClassAnnotation, ClassAnnotation>();

	Map<ClassAnnotation, MethodAnnotation> equalsMethod = new TreeMap<ClassAnnotation, MethodAnnotation>();

	BugReporter bugReporter;

	public OverridingEqualsNotSymmetrical(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	
	@Override
	public void visit(Code obj) {
		if (getMethodName().equals(EQUALS_NAME) && !getMethod().isStatic() && getMethod().isPublic()
		        && getMethodSig().equals(EQUALS_SIGNATURE)) {
			sawCheckedCast = sawSuperEquals = sawInstanceOf = sawGetClass = sawReturnSuper = sawReturnNonSuper = prevWasSuperEquals = sawGoodEqualsClass = sawBadEqualsClass = dangerDanger 
			= sawInstanceOfSupertype
				= alwaysTrue = alwaysFalse = false;
			sawInitialIdentityCheck = obj.getCode().length == 11 || obj.getCode().length == 9;
			equalsCalls = 0;
			super.visit(obj);
			KindOfEquals kind = KindOfEquals.UNKNOWN;
			if (alwaysTrue)  
				kind = KindOfEquals.ALWAYS_TRUE;
			else if (alwaysFalse) 
				kind = KindOfEquals.ALWAYS_FALSE;
			else if (sawReturnSuper && !sawReturnNonSuper)
				kind = KindOfEquals.RETURNS_SUPER;
			else if (sawSuperEquals)
				kind = KindOfEquals.INVOKES_SUPER;
			else if (sawInstanceOf || sawInstanceOfSupertype)
				kind = getThisClass().isAbstract() ? KindOfEquals.ABSTRACT_INSTANCE_OF : KindOfEquals.INSTANCE_OF_EQUALS;
			else if (sawGetClass && sawGoodEqualsClass)
				kind = KindOfEquals.GETCLASS_GOOD_EQUALS;
			else if (sawGetClass && sawBadEqualsClass) 
					kind = KindOfEquals.GETCLASS_BAD_EQUALS;
			else if (equalsCalls == 1)
				kind = KindOfEquals.DELEGATE_EQUALS;
			else if (sawInitialIdentityCheck)
				kind = KindOfEquals.TRIVIAL_EQUALS;
			else if (sawCheckedCast)
				kind = KindOfEquals.CHECKED_CAST_EQUALS;
			else {
				bugReporter.reportBug(new BugInstance(this, "EQ_UNUSUAL", Priorities.NORMAL_PRIORITY).addClassAndMethod(this).addString("Strange equals method"));
			}

			if (kind == KindOfEquals.GETCLASS_GOOD_EQUALS || kind == KindOfEquals.GETCLASS_BAD_EQUALS) {
				
				ClassDescriptor classDescriptor = getClassDescriptor();
				try {
	                Set<ClassDescriptor> subtypes = AnalysisContext.currentAnalysisContext().getSubtypes2().getSubtypes(classDescriptor);
	                if (subtypes.size() > 1) {
	                	classesWithGetClassBasedEquals.put(classDescriptor,subtypes);
	                }
                } catch (ClassNotFoundException e) {
	               assert true;
                }
				
			}
			ClassAnnotation classAnnotation = new ClassAnnotation(getDottedClassName());
			kindMap.put(classAnnotation, kind);
			String superClassName = getSuperclassName().replace('/', '.');
			if (!superClassName.equals("java.lang.Object"))
				parentMap.put(classAnnotation, new ClassAnnotation(superClassName));
			equalsMethod.put(classAnnotation, MethodAnnotation.fromVisitedMethod(this));
			
		}
	}

	boolean sawInstanceOf, sawInstanceOfSupertype, sawCheckedCast;

	boolean sawGetClass;

	boolean sawReturnSuper;

	boolean sawSuperEquals;

	boolean sawReturnNonSuper;

	boolean prevWasSuperEquals;

	boolean sawInitialIdentityCheck;
	boolean alwaysTrue, alwaysFalse;

	int equalsCalls;
	boolean sawGoodEqualsClass, sawBadEqualsClass;
	boolean dangerDanger = false;

	@Override
	public void sawOpcode(int seen) {
		if (getPC() == 2 && seen != IF_ACMPEQ && seen != IF_ACMPNE) {
			// System.out.println(OPCODE_NAMES[seen]);
			sawInitialIdentityCheck = false;
		}
		if (seen == IRETURN && getPC() == 1 && getPrevOpcode(1) == ICONST_0 ) {
			alwaysFalse = true;
			bugReporter.reportBug(new BugInstance(this, "EQ_ALWAYS_FALSE", Priorities.HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(this));

		}
		if (seen == IRETURN && getPC() == 1 && getPrevOpcode(1) == ICONST_1 ) {
			alwaysTrue = true;
			bugReporter.reportBug(new BugInstance(this, "EQ_ALWAYS_TRUE", Priorities.NORMAL_PRIORITY).addClassAndMethod(this).addSourceLine(this));

		}
		if (seen == IF_ACMPEQ || seen == IF_ACMPNE) {
			checkForComparingClasses();
		}
		if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals(EQUALS_NAME)
		        && getSigConstantOperand().equals(EQUALS_SIGNATURE)) {
			equalsCalls++;
			checkForComparingClasses();
		}
		
		if (dangerDanger && seen == INVOKEVIRTUAL && getNameConstantOperand().equals(EQUALS_NAME)
		        && getSigConstantOperand().equals(EQUALS_SIGNATURE)) {
			bugReporter.reportBug(new BugInstance(this, "EQ_COMPARING_CLASS_NAMES", Priorities.NORMAL_PRIORITY).addClassAndMethod(this).addSourceLine(this));
			
		}
		dangerDanger = false;
		if (seen == INVOKEVIRTUAL && getClassConstantOperand().equals("java/lang/Class") && getNameConstantOperand().equals("getName")
		        && getSigConstantOperand().equals("()Ljava/lang/String;") && stack.getStackDepth() >= 2) {
			Item left = stack.getStackItem(1);
	    	XMethod leftM = left.getReturnValueOf();
	    	Item right = stack.getStackItem(0);
	    	XMethod rightM = right.getReturnValueOf();
	    	if (leftM != null && rightM != null && leftM.getName().equals("getName") && rightM.getName().equals("getClass")) {
	    		dangerDanger = true;
	    	}
	    	 
		}
		if (seen == INVOKESPECIAL && getNameConstantOperand().equals(EQUALS_NAME)
		        && getSigConstantOperand().equals(EQUALS_SIGNATURE)) {
			sawSuperEquals = prevWasSuperEquals = true;
		} else {
			if (seen == IRETURN) {
				if (prevWasSuperEquals)
					sawReturnSuper = true;
				else
					sawReturnNonSuper = true;
			}
			prevWasSuperEquals = false;
		}

		if (seen == INSTANCEOF && stack.getStackDepth() > 0 && stack.getStackItem(0).getRegisterNumber() == 1) {
			ClassDescriptor instanceOfCheck = getClassDescriptorOperand();
			if (instanceOfCheck.equals(getClassDescriptor()))
				sawInstanceOf = true;
            else
	            try {
	                if (AnalysisContext.currentAnalysisContext().getSubtypes2().isSubtype(getClassDescriptor(), instanceOfCheck))
	                	sawInstanceOfSupertype = true;
                } catch (ClassNotFoundException e) {
                	sawInstanceOfSupertype = true;
                }
		}
		

		if (seen == CHECKCAST && stack.getStackDepth() > 0 && stack.getStackItem(0).getRegisterNumber() == 1) {
			ClassDescriptor castTo = getClassDescriptorOperand();
			if (castTo.equals(getClassDescriptor()))
				sawCheckedCast = true;
			try {
                if (AnalysisContext.currentAnalysisContext().getSubtypes2().isSubtype(getClassDescriptor(), castTo))
                	sawCheckedCast = true;
            } catch (ClassNotFoundException e) {
            	sawCheckedCast = true;
            }
		}
		if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals("getClass")
		        && getSigConstantOperand().equals("()Ljava/lang/Class;")) {
			sawGetClass = true;
		}

	}


	/**
     * 
     */
    private void checkForComparingClasses() {
	    if (stack.getStackDepth() >= 2) {
	    	Item left = stack.getStackItem(1);
	    	XMethod leftM = left.getReturnValueOf();
	    	Item right = stack.getStackItem(0);
	    	XMethod rightM = right.getReturnValueOf();
	    	if (left.getSignature().equals("Ljava/lang/Class;") && right.getSignature().equals("Ljava/lang/Class;") ) {
	    	boolean leftMatch = leftM != null && leftM.getName().equals("getClass");
			boolean rightMatch = rightM != null && rightM.getName().equals("getClass");
			if (leftMatch && rightMatch) {
	    		sawGoodEqualsClass = true;
	    	} else {
	    		if (left.getConstant() != null  && rightMatch || leftMatch && right.getConstant() != null) {
	    			sawBadEqualsClass = true;
	    			if (!getThisClass().isFinal()) {
						int priority = Priorities.NORMAL_PRIORITY;
						try {
	                        if (AnalysisContext.currentAnalysisContext().getSubtypes2().hasSubtypes(getClassDescriptor()))
	                        	priority--;
                        } catch (ClassNotFoundException e) {
	                        bugReporter.reportMissingClass(e);
                        }
						bugReporter.reportBug(new BugInstance(this,"EQ_GETCLASS_AND_CLASS_CONSTANT", priority).addClassAndMethod(this).addSourceLine(this).addString("doesn't work for subtypes"));
					}
	    		}
	    	}
	    	}
	    	
	    }
    }

	@Override
	public void report() {

		if (false) 
		for (Map.Entry<ClassDescriptor, Set<ClassDescriptor>> e : classesWithGetClassBasedEquals.entrySet()) {
			ClassAnnotation parentClass = ClassAnnotation.fromClassDescriptor(e.getKey());
			KindOfEquals parentKind = kindMap.get(parentClass);
			for(ClassDescriptor child : e.getValue()) {
				if (child.equals(e.getKey())) continue;
				XClass xChild = AnalysisContext.currentXFactory().getXClass(child);
				if (xChild == null) continue;
				ClassAnnotation childClass = ClassAnnotation.fromClassDescriptor(child);
				KindOfEquals childKind = kindMap.get(childClass);
				int fieldsOfInterest = 0;
				for(XField f : xChild.getXFields())
					if (!f.isStatic() && !f.isSynthetic()) fieldsOfInterest++;
				System.out.println(parentKind + " " + childKind + " " + parentClass + " " + childClass + " " + fieldsOfInterest);
				
			}
		}
			
		for (Map.Entry<ClassAnnotation, ClassAnnotation> e : parentMap.entrySet()) {
			ClassAnnotation childClass = e.getKey();
			KindOfEquals childKind = kindMap.get(childClass);
			ClassAnnotation parentClass = e.getValue();
			KindOfEquals parentKind = kindMap.get(parentClass);
			if (parentKind != null && childKind == KindOfEquals.INSTANCE_OF_EQUALS && parentKind == KindOfEquals.INSTANCE_OF_EQUALS)
				bugReporter.reportBug(new BugInstance(this, "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", NORMAL_PRIORITY)
				        .add(childClass).add(equalsMethod.get(childClass)).add(equalsMethod.get(parentClass)));

		}
	}
}
