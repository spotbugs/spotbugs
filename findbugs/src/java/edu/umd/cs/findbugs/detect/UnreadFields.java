/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class UnreadFields extends BytecodeScanningDetector implements Constants2 {
	private static final boolean DEBUG = Boolean.getBoolean("unreadfields.debug");

	Set<FieldAnnotation> declaredFields = new TreeSet<FieldAnnotation>();
	Set<FieldAnnotation> fieldsOfSerializableOrNativeClassed
	        = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> myFields = new TreeSet<FieldAnnotation>();
	HashSet<FieldAnnotation> writtenFields = new HashSet<FieldAnnotation>();
	HashSet<FieldAnnotation> readFields = new HashSet<FieldAnnotation>();
	HashSet<FieldAnnotation> constantFields = new HashSet<FieldAnnotation>();
	// HashSet finalFields = new HashSet();
	HashSet<String> needsOuterObjectInConstructor = new HashSet<String>();
	HashSet<String> superReadFields = new HashSet<String>();
	HashSet<String> superWrittenFields = new HashSet<String>();
	HashSet<String> innerClassCannotBeStatic = new HashSet<String>();
	boolean hasNativeMethods;
	boolean isSerializable;
	private BugReporter bugReporter;

	static final int doNotConsider = ACC_PUBLIC | ACC_PROTECTED | ACC_STATIC;

	public UnreadFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	public void visit(JavaClass obj) {
		hasNativeMethods = false;
		isSerializable = false;
		if (getSuperclassName().indexOf("$") >= 0
		        || getSuperclassName().indexOf("+") >= 0) {
			// System.out.println("hicfsc: " + betterClassName);
			innerClassCannotBeStatic.add(getDottedClassName());
			// System.out.println("hicfsc: " + betterSuperclassName);
			innerClassCannotBeStatic.add(getDottedSuperclassName());
		}
		// Does this class directly implement Serializable?
		String[] interface_names = obj.getInterfaceNames();
		for (int i = 0; i < interface_names.length; i++) {
			if (interface_names[i].equals("java.io.Externalizable")) {
				isSerializable = true;
			} else if (interface_names[i].equals("java.io.Serializable")) {
				isSerializable = true;
				break;
			}
		}

		// Does this class indirectly implement Serializable?
		if (!isSerializable) {
			try {
				if (Repository.instanceOf(obj, "java.io.Externalizable"))
					isSerializable = true;
				if (Repository.instanceOf(obj, "java.io.Serializable"))
					isSerializable = true;
				if (Repository.instanceOf(obj, "java.rmi.Remote")) {
					isSerializable = true;
				}
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}

		// System.out.println(getDottedClassName() + " is serializable: " + isSerializable);
		super.visit(obj);
	}

	public void visitAfter(JavaClass obj) {
		declaredFields.addAll(myFields);
		if (hasNativeMethods || isSerializable)
			fieldsOfSerializableOrNativeClassed.addAll(myFields);
		myFields.clear();
	}


	public void visit(Field obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		if ((flags & doNotConsider) == 0
		        && !getFieldName().equals("serialVersionUID")) {

			FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
			myFields.add(f);
		}
	}

	public void visit(ConstantValue obj) {
		// ConstantValue is an attribute of a field, so the instance variables
		// set during visitation of the Field are still valid here
		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
		constantFields.add(f);
	}


	int count_aload_1;

	public void visit(Code obj) {
		count_aload_1 = 0;
		super.visit(obj);
		if (getMethodName().equals("<init>") && count_aload_1 > 1
		        && (getClassName().indexOf('$') >= 0
		        || getClassName().indexOf('+') >= 0)) {
			needsOuterObjectInConstructor.add(getDottedClassName());
			// System.out.println(betterClassName + " needs outer object in constructor");
		}
	}

	public void visit(Method obj) {
		super.visit(obj);
		int flags = obj.getAccessFlags();
		if ((flags & ACC_NATIVE) != 0)
			hasNativeMethods = true;
	}


	public void sawOpcode(int seen) {

		if (seen == ALOAD_1) {
			count_aload_1++;
		} else if (seen == GETFIELD) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			if (DEBUG) System.out.println("get: " + f);
			readFields.add(f);
			if (getClassConstantOperand().equals(getClassName()) &&
			        !myFields.contains(f)) {
				superReadFields.add(getNameConstantOperand());
			}
		} else if (seen == PUTFIELD) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			if (DEBUG) System.out.println("put: " + f);
			writtenFields.add(f);
			if (getClassConstantOperand().equals(getClassName()) &&
			        !myFields.contains(f)) {
				superWrittenFields.add(getNameConstantOperand());
			}
		}
	}

	public void report() {

		TreeSet<FieldAnnotation> readOnlyFields =
		        new TreeSet<FieldAnnotation>(declaredFields);
		readOnlyFields.removeAll(writtenFields);
		readOnlyFields.retainAll(readFields);
		Set<FieldAnnotation> writeOnlyFields = declaredFields;
		writeOnlyFields.removeAll(readFields);

		for (Iterator<FieldAnnotation> i = readOnlyFields.iterator(); i.hasNext();) {
			FieldAnnotation f = i.next();
			String fieldName = f.getFieldName();
			String className = f.getClassName();
			if (!superWrittenFields.contains(fieldName)
				 && !fieldsOfSerializableOrNativeClassed.contains(f))
				bugReporter.reportBug(new BugInstance(this, "UWF_UNWRITTEN_FIELD", NORMAL_PRIORITY)
				        .addClass(className)
				        .addField(f));
		}


		for (Iterator<FieldAnnotation> i = writeOnlyFields.iterator(); i.hasNext();) {
			FieldAnnotation f = i.next();
			String fieldName = f.getFieldName();
			String className = f.getClassName();
			int lastDollar =
			        Math.max(className.lastIndexOf('$'),
			                className.lastIndexOf('+'));
			boolean isAnonymousInnerClass =
			        (lastDollar > 0)
			        && (lastDollar < className.length() - 1)
			        && Character.isDigit(className.charAt(className.length() - 1));
			boolean allUpperCase =
			        fieldName.equals(fieldName.toUpperCase());
			if (superReadFields.contains(f.getFieldName())) continue;
			if (!fieldName.startsWith("this$")
			        && !fieldName.startsWith("this+")
			) {
				if (constantFields.contains(f))
					bugReporter.reportBug(new BugInstance(this, "SS_SHOULD_BE_STATIC", NORMAL_PRIORITY)
					        .addClass(className)
					        .addField(f));
				else if (fieldsOfSerializableOrNativeClassed.contains(f)) {
					// ignore it
				} else if (!writtenFields.contains(f) && !superWrittenFields.contains(f.getFieldName()))
					bugReporter.reportBug(new BugInstance(this, "UUF_UNUSED_FIELD", NORMAL_PRIORITY)
					        .addClass(className)
					        .addField(f));
				else
					bugReporter.reportBug(new BugInstance(this, "URF_UNREAD_FIELD", NORMAL_PRIORITY)
					        .addClass(className)
					        .addField(f));
			} else if (!innerClassCannotBeStatic.contains(className)) {
				boolean easyChange = !needsOuterObjectInConstructor.contains(className);
				if (easyChange || !isAnonymousInnerClass) {

					// easyChange    isAnonymousInnerClass
					// true          false			medium, SIC
					// true          true				low, SIC_ANON
					// false         true				not reported
					// false         false			low, SIC_THIS
					int priority = LOW_PRIORITY;
					if (easyChange && !isAnonymousInnerClass)
						priority = NORMAL_PRIORITY;

					String bug = "SIC_INNER_SHOULD_BE_STATIC";
					if (isAnonymousInnerClass)
						bug = "SIC_INNER_SHOULD_BE_STATIC_ANON";
					else if (!easyChange)
						bug = "SIC_INNER_SHOULD_BE_STATIC_NEEDS_THIS";

					bugReporter.reportBug(new BugInstance(this, bug, priority)
					        .addClass(className));
				}
			}
		}

	}
}
