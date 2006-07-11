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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;

public class UnreadFields extends BytecodeScanningDetector  {
	private static final boolean DEBUG = Boolean.getBoolean("unreadfields.debug");

	static class ProgramPoint {
		ProgramPoint(BytecodeScanningDetector v) {
			method = MethodAnnotation.fromVisitedMethod(v);
			sourceLine = SourceLineAnnotation
				.fromVisitedInstruction(v,v.getPC());
			}
		MethodAnnotation method;
		SourceLineAnnotation sourceLine;
		}

	Map<FieldAnnotation,HashSet<ProgramPoint> >
		assumedNonNull = new HashMap<FieldAnnotation,HashSet<ProgramPoint>>();
	Set<FieldAnnotation> nullTested = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> declaredFields = new TreeSet<FieldAnnotation>();
	Set<FieldAnnotation> fieldsOfSerializableOrNativeClassed
	        = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> staticFieldsReadInThisMethod = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> allMyFields = new TreeSet<FieldAnnotation>();
	Set<FieldAnnotation> myFields = new TreeSet<FieldAnnotation>();
	Set<FieldAnnotation> writtenFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> writtenNonNullFields = new HashSet<FieldAnnotation>();
	
	Set<FieldAnnotation> writtenInConstructorFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> readFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> constantFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> finalFields = new HashSet<FieldAnnotation>();
	Set<String> needsOuterObjectInConstructor = new HashSet<String>();
	Set<String> superReadFields = new HashSet<String>();
	Set<String> superWrittenFields = new HashSet<String>();
	Set<String> innerClassCannotBeStatic = new HashSet<String>();
	boolean hasNativeMethods;
	boolean isSerializable;
	boolean sawSelfCallInConstructor;
	private BugReporter bugReporter;
	boolean publicOrProtectedConstructor;

	static final int doNotConsider = ACC_PUBLIC | ACC_PROTECTED;

	public UnreadFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	@Override
         public void visit(JavaClass obj) {
		hasNativeMethods = false;
		sawSelfCallInConstructor = false;
		publicOrProtectedConstructor = false;
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
		for (String interface_name : interface_names) {
			if (interface_name.equals("java.io.Externalizable")) {
				isSerializable = true;
			} else if (interface_name.equals("java.io.Serializable")) {
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

	@Override
         public void visitAfter(JavaClass obj) {
		declaredFields.addAll(myFields);
		if (hasNativeMethods || isSerializable)
			fieldsOfSerializableOrNativeClassed.addAll(myFields);
		if (sawSelfCallInConstructor) 
			writtenInConstructorFields.addAll(myFields);
		myFields.clear();
		allMyFields.clear();
	}


	@Override
         public void visit(Field obj) {
		super.visit(obj);
		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
		allMyFields.add(f);
		int flags = obj.getAccessFlags();
		if ((flags & doNotConsider) == 0
		        && !getFieldName().equals("serialVersionUID")) {

			myFields.add(f);
			if (obj.isFinal()) finalFields.add(f);
		}
	}

	@Override
         public void visit(ConstantValue obj) {
		// ConstantValue is an attribute of a field, so the instance variables
		// set during visitation of the Field are still valid here
		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
		constantFields.add(f);
	}


	int count_aload_1;

	private OpcodeStack opcodeStack = new OpcodeStack();
	private int previousOpcode;
	private int previousPreviousOpcode;
	@Override
         public void visit(Code obj) {
	
		count_aload_1 = 0;
		previousOpcode = -1;
		previousPreviousOpcode = -1;
		nullTested.clear();
		seenInvokeStatic = false;
                opcodeStack.resetForMethodEntry(this);
                staticFieldsReadInThisMethod.clear();
		super.visit(obj);
		if (getMethodName().equals("<init>") && count_aload_1 > 1
		        && (getClassName().indexOf('$') >= 0
		        || getClassName().indexOf('+') >= 0)) {
			needsOuterObjectInConstructor.add(getDottedClassName());
			// System.out.println(betterClassName + " needs outer object in constructor");
		}
	}

	@Override
         public void visit(Method obj) {
		if (getMethodName().equals("<init>")
			&& (obj.isPublic() 
			    || obj.isProtected() ))
			publicOrProtectedConstructor = true;
		super.visit(obj);
		int flags = obj.getAccessFlags();
		if ((flags & ACC_NATIVE) != 0)
			hasNativeMethods = true;
	}

	boolean seenInvokeStatic;

	@Override
         public void sawOpcode(int seen) {
		
		opcodeStack.mergeJumps(this);
		if (seen == GETSTATIC) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
                	staticFieldsReadInThisMethod.add(f);
			}
		else if (seen == INVOKESTATIC) {
			seenInvokeStatic = true;
			}
		else if (seen == PUTSTATIC 
			&& !getMethod().isStatic()) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
                	if (!staticFieldsReadInThisMethod.contains(f)) {
				int priority = LOW_PRIORITY;
				if (!publicOrProtectedConstructor)
					priority--;
				if (!seenInvokeStatic 
				     && staticFieldsReadInThisMethod.isEmpty())
					priority--;
				if (getThisClass().isPublic() 
					&& getMethod().isPublic())
					priority--;
				if (getThisClass().isPrivate() 
				    || getMethod().isPrivate())
					priority++;
				if (getClassName().indexOf('$') != -1)
					priority++;
				bugReporter.reportBug(new BugInstance(this, 
						"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
					priority
					)
				        .addClassAndMethod(this)
				        .addField(f)
				        .addSourceLine(this)
					);
				}
			}


		if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE
			|| seen == INVOKESPECIAL)  {
				String sig = getSigConstantOperand();
				int pos = PreorderVisitor.getNumberArguments(sig);
				if (opcodeStack.getStackDepth() > pos) {
				OpcodeStack.Item item = opcodeStack.getStackItem(pos);
				if (DEBUG)
				System.out.println("In " + getFullyQualifiedMethodName()
					+ " saw call on " + item);
				boolean superCall = seen == INVOKESPECIAL
					&&  !getClassConstantOperand() .equals(getClassName());
				boolean selfCall = item.getRegisterNumber() == 0 
					&& !superCall;
				if (selfCall && getMethodName().equals("<init>")) {
					sawSelfCallInConstructor = true;	
					if (DEBUG)
					System.out.println("Saw self call in " + getFullyQualifiedMethodName()  + " to " + getClassConstantOperand() + "." + getNameConstantOperand()
					);
					}
				}
			}

		if ((seen == IFNULL || seen == IFNONNULL) 
			&& opcodeStack.getStackDepth() > 0)  {
			OpcodeStack.Item item = opcodeStack.getStackItem(0);
			FieldAnnotation f = item.getField();
			if (f != null) {
				nullTested.add(f);
				if (DEBUG)
				System.out.println(f + " null checked in " +
					getFullyQualifiedMethodName());
				}
			}

		if (seen == GETFIELD || seen == INVOKEVIRTUAL 
				|| seen == INVOKEINTERFACE
				|| seen == INVOKESPECIAL || seen == PUTFIELD 
				|| seen == IALOAD
				|| seen == IASTORE)  {
			int pos = 0;
			switch(seen) {
			case GETFIELD :
				pos = 0;
				break;
			case INVOKEVIRTUAL :
			case INVOKEINTERFACE:
			case INVOKESPECIAL:
				String sig = getSigConstantOperand();
				pos = PreorderVisitor.getNumberArguments(sig);
				break;
			case PUTFIELD :
			case IALOAD :
			case IASTORE :
				pos = 1;
				break;
			default: throw new RuntimeException("Impossible");
			}
			if (opcodeStack.getStackDepth() > pos) {
			OpcodeStack.Item item = opcodeStack.getStackItem(pos);
			FieldAnnotation f = item.getField();
			if (f != null && !nullTested.contains(f) 
					&& ! (writtenInConstructorFields.contains(f)
						 && writtenNonNullFields.contains(f))
					) {
				ProgramPoint p = new ProgramPoint(this);
				HashSet <ProgramPoint> s = assumedNonNull.get(f);
				if (s == null) {
					s = new HashSet<ProgramPoint>();
					assumedNonNull.put(f,s);
					}
				s.add(p);
				if (DEBUG)
				System.out.println(f + " assumed non-null in " +
					getFullyQualifiedMethodName());
				}
			}
			}

		if (seen == ALOAD_1) {
			count_aload_1++;
		} else if (seen == GETFIELD || seen == GETSTATIC) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			if (DEBUG) System.out.println("get: " + f);
			readFields.add(f);
			if (getClassConstantOperand().equals(getClassName()) &&
			        !allMyFields.contains(f)) {
				superReadFields.add(getNameConstantOperand());
			}
		} else if (seen == PUTFIELD || seen == PUTSTATIC) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			OpcodeStack.Item item = null;
			if (opcodeStack.getStackDepth() > 0) {
				item = opcodeStack.getStackItem(0);
				if (!item.isNull()) nullTested.add(f);
			}
			writtenFields.add(f);
			if (previousOpcode != ACONST_NULL || previousPreviousOpcode == GOTO )  {
				writtenNonNullFields.add(f);
				if (DEBUG) System.out.println("put nn: " + f);
			}
			else if (DEBUG) System.out.println("put: " + f);
			
			if (
					getMethodName().equals("<init>") 
					|| getMethodName().equals("<clinit>") 
					|| getMethod().isPrivate()) {
				writtenInConstructorFields.add(f);
				if (previousOpcode != ACONST_NULL || previousPreviousOpcode == GOTO ) 
					assumedNonNull.remove(f);
			}
			
			if (getClassConstantOperand().equals(getClassName()) &&
					!allMyFields.contains(f)) {
				superWrittenFields.add(getNameConstantOperand());
			}
			
		}
		opcodeStack.sawOpcode(this, seen);
		previousPreviousOpcode = previousOpcode;
		previousOpcode = seen;
		if (DEBUG) {
		System.out.println("After " + OPCODE_NAMES[seen] + " opcode stack is");
		System.out.println(opcodeStack);
		}
		
	}

	Pattern dontComplainAbout = Pattern.compile("class[$]");
	@Override
         public void report() {

		TreeSet<FieldAnnotation> notInitializedInConstructors =
		        new TreeSet<FieldAnnotation>(declaredFields);
		notInitializedInConstructors.retainAll(readFields);
		notInitializedInConstructors.retainAll(writtenFields);
		notInitializedInConstructors.retainAll(assumedNonNull.keySet());
		notInitializedInConstructors.removeAll(writtenInConstructorFields);
		
		TreeSet<FieldAnnotation> readOnlyFields =
		        new TreeSet<FieldAnnotation>(declaredFields);
		readOnlyFields.removeAll(writtenFields);
		readOnlyFields.retainAll(readFields);
		
		TreeSet<FieldAnnotation> nullOnlyFields =
	        new TreeSet<FieldAnnotation>(declaredFields);
		nullOnlyFields.removeAll(writtenNonNullFields);
		nullOnlyFields.retainAll(readFields);
		
		Set<FieldAnnotation> writeOnlyFields = declaredFields;
		writeOnlyFields.removeAll(readFields);

		for (FieldAnnotation f : notInitializedInConstructors) {
			String fieldName = f.getFieldName();
			String className = f.getClassName();
			String fieldSignature = f.getFieldSignature();
			if (!superWrittenFields.contains(fieldName)
					&& !fieldsOfSerializableOrNativeClassed.contains(f)
					&& (fieldSignature.charAt(0) == 'L' || fieldSignature.charAt(0) == '[')
					) {
				int priority = LOW_PRIORITY;
				bugReporter.reportBug(new BugInstance(this,
						"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
						priority)
						.addClass(className)
						.addField(f));
			}
		}


		for (FieldAnnotation f : readOnlyFields) {
			String fieldName = f.getFieldName();
			String className = f.getClassName();
			String fieldSignature = f.getFieldSignature();
			if (!superWrittenFields.contains(fieldName)
					&& !fieldsOfSerializableOrNativeClassed.contains(f)) {
				int priority = NORMAL_PRIORITY;
				if (!(fieldSignature.charAt(0) == 'L' || fieldSignature.charAt(0) == '['))
					priority++;

				bugReporter.reportBug(new BugInstance(this,
						"UWF_UNWRITTEN_FIELD",
						priority)
						.addClass(className)
						.addField(f));
			}

		}
		for (FieldAnnotation f : nullOnlyFields) {
			String fieldName = f.getFieldName();
			String className = f.getClassName();
			String fieldSignature = f.getFieldSignature();
			if (DEBUG) {
				System.out.println("Null only: " + f);
				System.out.println("   : " + assumedNonNull.containsKey(f));
			}
			if (superWrittenFields.contains(fieldName)) continue;
			if (fieldsOfSerializableOrNativeClassed.contains(f)) continue;
			int priority = NORMAL_PRIORITY;
			if (assumedNonNull.containsKey(f)) {
				priority = HIGH_PRIORITY;
				for (ProgramPoint p : assumedNonNull.get(f))
					bugReporter.reportBug(new BugInstance(this,
							"NP_UNWRITTEN_FIELD",
							NORMAL_PRIORITY)
							.addClassAndMethod(p.method)
							.addField(f)
							.addSourceLine(p.sourceLine)
					);
			}
			if (!readOnlyFields.contains(f))
				bugReporter.reportBug(new BugInstance(this,
						"UWF_NULL_FIELD",
						priority)
						.addClass(className)
						.addField(f));
		}

		for (FieldAnnotation f : writeOnlyFields) {
			String fieldName = f.getFieldName();
			String className = f.getClassName();
			int lastDollar =
					Math.max(className.lastIndexOf('$'),
							className.lastIndexOf('+'));
			boolean isAnonymousInnerClass =
					(lastDollar > 0)
							&& (lastDollar < className.length() - 1)
							&& Character.isDigit(className.charAt(className.length() - 1));

			if (DEBUG) {
				System.out.println("Checking write only field " + className
						+ "." + fieldName
						+ "\t" + superReadFields.contains(fieldName)
						+ "\t" + constantFields.contains(f)
						+ "\t" + f.isStatic()
				);
			}
			if (superReadFields.contains(fieldName)) continue;
			if (dontComplainAbout.matcher(fieldName).find()) continue;
			if (fieldName.startsWith("this$")
					|| fieldName.startsWith("this+")) {
				if (!innerClassCannotBeStatic.contains(className)) {
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
			} else {
				if (constantFields.contains(f)) {
					if (!f.isStatic())
						bugReporter.reportBug(new BugInstance(this,
								"SS_SHOULD_BE_STATIC",
								NORMAL_PRIORITY)
								.addClass(className)
								.addField(f));
				} else if (fieldsOfSerializableOrNativeClassed.contains(f)) {
					// ignore it
				} else if (!writtenFields.contains(f) && !superWrittenFields.contains(fieldName))
					bugReporter.reportBug(new BugInstance(this, "UUF_UNUSED_FIELD", NORMAL_PRIORITY)
							.addClass(className)
							.addField(f));
				else if (!f.isStatic() || !finalFields.contains(f))
					bugReporter.reportBug(new BugInstance(this, "URF_UNREAD_FIELD", NORMAL_PRIORITY)
							.addClass(className)
							.addField(f));
			}
		}

	}
}
