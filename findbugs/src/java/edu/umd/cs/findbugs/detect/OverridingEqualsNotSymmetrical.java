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
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;

public class OverridingEqualsNotSymmetrical extends BytecodeScanningDetector {

	private static final String EQUALS_NAME = "equals";

	private static final String EQUALS_SIGNATURE = "(Ljava/lang/Object;)Z";

	static class KindOfEquals {
		static int next = 0;

		final int ordinal;

		final String name;

		static LinkedList<KindOfEquals> valueCollection = new LinkedList<KindOfEquals>();

		public static KindOfEquals[] values() {
			return valueCollection.toArray(new KindOfEquals[valueCollection.size()]);
		}

		KindOfEquals(String name) {
			this.ordinal = next++;
			this.name = name;
			valueCollection.add(this);
		}

		public int ordinal() {
			return ordinal;
		}

		@Override
		public String toString() {
			return name;
		}

		static KindOfEquals OBJECT_EQUALS = new KindOfEquals("OBJECT_EQUALS");

		static KindOfEquals ABSTRACT_INSTANCE_OF = new KindOfEquals("ABSTRACT_INSTANCE_OF");

		static KindOfEquals INSTANCE_OF_EQUALS = new KindOfEquals("INSTANCE_OF_EQUALS");

		static KindOfEquals CHECKED_CAST_EQUALS = new KindOfEquals("CHECKED_CAST_EQUALS");

		static KindOfEquals RETURNS_SUPER = new KindOfEquals("RETURNS_SUPER");

		static KindOfEquals GETCLASS_EQUALS = new KindOfEquals("GETCLASS_EQUALS");

		static KindOfEquals DELEGATE_EQUALS = new KindOfEquals("DELEGATE_EQUALS");

		static KindOfEquals TRIVIAL_EQUALS = new KindOfEquals("TRIVIAL_EQUALS");

		static KindOfEquals INVOKES_SUPER = new KindOfEquals("INVOKES_SUPER");

		static KindOfEquals UNKNOWN = new KindOfEquals("UNKNOWN");
	}

	Map<ClassAnnotation, KindOfEquals> kindMap = new HashMap<ClassAnnotation, KindOfEquals>();

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
			sawCheckedCast = sawSuperEquals = sawInstanceOf = sawGetClass = sawReturnSuper = sawReturnNonSuper = prevWasSuperEquals = false;
			sawInitialIdentityCheck = obj.getCode().length == 11 || obj.getCode().length == 9;
			equalsCalls = 0;
			super.visit(obj);
			KindOfEquals kind = KindOfEquals.UNKNOWN;
			if (sawReturnSuper && !sawReturnNonSuper)
				kind = KindOfEquals.RETURNS_SUPER;
			else if (sawSuperEquals)
				kind = KindOfEquals.INVOKES_SUPER;
			else if (sawInstanceOf)
				kind = getThisClass().isAbstract() ? KindOfEquals.ABSTRACT_INSTANCE_OF : KindOfEquals.INSTANCE_OF_EQUALS;
			else if (sawGetClass)
				kind = KindOfEquals.GETCLASS_EQUALS;
			else if (equalsCalls == 1)
				kind = KindOfEquals.DELEGATE_EQUALS;
			else if (sawInitialIdentityCheck)
				kind = KindOfEquals.TRIVIAL_EQUALS;
			else if (sawCheckedCast)
				kind = KindOfEquals.CHECKED_CAST_EQUALS;

			ClassAnnotation classAnnotation = new ClassAnnotation(getDottedClassName());
			kindMap.put(classAnnotation, kind);
			String superClassName = getSuperclassName().replace('/', '.');
			if (!superClassName.equals("java.lang.Object"))
				parentMap.put(classAnnotation, new ClassAnnotation(superClassName));
			equalsMethod.put(classAnnotation, MethodAnnotation.fromVisitedMethod(this));
		}
	}

	boolean sawInstanceOf, sawCheckedCast;

	boolean sawGetClass;

	boolean sawReturnSuper;

	boolean sawSuperEquals;

	boolean sawReturnNonSuper;

	boolean prevWasSuperEquals;

	boolean sawInitialIdentityCheck;

	int equalsCalls;

	@Override
	public void sawOpcode(int seen) {
		if (getPC() == 2 && seen != IF_ACMPEQ && seen != IF_ACMPNE) {
			// System.out.println(OPCODE_NAMES[seen]);
			sawInitialIdentityCheck = false;
		}

		if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals(EQUALS_NAME)
		        && getSigConstantOperand().equals(EQUALS_SIGNATURE)) {
			equalsCalls++;
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

		if (seen == INSTANCEOF && getClassConstantOperand().equals(getClassName())) {
			sawInstanceOf = true;
		}
		if (seen == CHECKCAST && getClassConstantOperand().equals(getClassName())) {
			sawCheckedCast = true;
		}
		if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals("getClass")
		        && getSigConstantOperand().equals("()Ljava/lang/Class;")) {
			sawGetClass = true;
		}

	}

	@Override
	public void report() {

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
