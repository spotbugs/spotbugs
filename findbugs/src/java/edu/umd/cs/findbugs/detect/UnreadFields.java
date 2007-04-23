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
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;

public class UnreadFields extends BytecodeScanningDetector  {
	private static final boolean DEBUG = SystemProperties.getBoolean("unreadfields.debug");

	static class ProgramPoint {
		ProgramPoint(BytecodeScanningDetector v) {
			method = MethodAnnotation.fromVisitedMethod(v);
			sourceLine = SourceLineAnnotation
				.fromVisitedInstruction(v,v.getPC());
			}
		MethodAnnotation method;
		SourceLineAnnotation sourceLine;
		}

	Map<XField,HashSet<ProgramPoint> >
		assumedNonNull = new HashMap<XField,HashSet<ProgramPoint>>();
	Set<XField> nullTested = new HashSet<XField>();
	Set<XField> staticFields = new HashSet<XField>();
	Set<XField> declaredFields = new TreeSet<XField>();
	Set<XField> containerFields = new TreeSet<XField>();
	Set<String> abstractClasses = new HashSet<String>();
	Set<String> hasNonAbstractSubClass = new HashSet<String>();
	Set<String> classesScanned = new HashSet<String>();
	Set<XField> fieldsOfNativeClasses
	= new HashSet<XField>();
	Set<XField> reflectiveFields
	= new HashSet<XField>();
	Set<XField> fieldsOfSerializableOrNativeClassed
			= new HashSet<XField>();
	Set<XField> staticFieldsReadInThisMethod = new HashSet<XField>();
	Set<XField> allMyFields = new TreeSet<XField>();
	Set<XField> myFields = new TreeSet<XField>();
	Set<XField> writtenFields = new HashSet<XField>();
	Map<XField,SourceLineAnnotation> fieldAccess = new HashMap<XField, SourceLineAnnotation>();
	Set<XField> writtenNonNullFields = new HashSet<XField>();
	Set<String> calledFromConstructors = new HashSet<String>();
	Set<XField> writtenInConstructorFields = new HashSet<XField>();
	Set<XField> writtenOutsideOfConstructorFields = new HashSet<XField>();

	Set<XField> readFields = new HashSet<XField>();
	Set<XField> constantFields = new HashSet<XField>();
	Set<XField> finalFields = new HashSet<XField>();
	Set<String> needsOuterObjectInConstructor = new HashSet<String>();
	Set<String> innerClassCannotBeStatic = new HashSet<String>();
	boolean hasNativeMethods;
	boolean isSerializable;
	boolean sawSelfCallInConstructor;
	private BugReporter bugReporter;
	boolean publicOrProtectedConstructor;

	public Set<? extends XField> getReadFields() {
		return readFields;
	}
	public Set<? extends XField> getWrittenFields() {
		return writtenFields;
	}
	public Set<? extends XField> getWrittenOutsideOfConstructorFields() {
		return writtenOutsideOfConstructorFields;
	}
	static final int doNotConsider = ACC_PUBLIC | ACC_PROTECTED;

	public UnreadFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		AnalysisContext context = AnalysisContext.currentAnalysisContext();
		context.setUnreadFields(this);
	}


	@Override
		 public void visit(JavaClass obj) {
		calledFromConstructors.clear();
		hasNativeMethods = false;
		sawSelfCallInConstructor = false;
		publicOrProtectedConstructor = false;
		isSerializable = false;
		if (obj.isAbstract()) {
			abstractClasses.add(getDottedClassName());
		}
		else {
			String superClass = obj.getSuperclassName();
			if (superClass != null) hasNonAbstractSubClass.add(superClass);
		}
		classesScanned.add(getDottedClassName());	
		if (getSuperclassName().indexOf("$") >= 0
				|| getSuperclassName().indexOf("+") >= 0 || withinAnonymousClass.matcher(getDottedClassName()).find()) {
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

	public static boolean classHasParameter(JavaClass obj) {
		for(Attribute a : obj.getAttributes()) 
			if (a instanceof Signature) {
				String sig = ((Signature)a).getSignature();
				return  sig.charAt(0) == '<';
			}
		return false;
	}
	@Override
		 public void visitAfter(JavaClass obj) {
		declaredFields.addAll(myFields);
		if (hasNativeMethods) {
			fieldsOfSerializableOrNativeClassed.addAll(myFields);
			fieldsOfNativeClasses.addAll(myFields);
		}
		if (isSerializable) {
			fieldsOfSerializableOrNativeClassed.addAll(myFields);
		}
		if (sawSelfCallInConstructor) 
			writtenInConstructorFields.addAll(myFields);
		myFields.clear();
		allMyFields.clear();
		calledFromConstructors.clear();
	}

	@Override
		 public void visit(Field obj) {
		super.visit(obj);
		XField f = XFactory.createXField(this);
		allMyFields.add(f);
		int flags = obj.getAccessFlags();
		if ((flags & doNotConsider) == 0
				&& !getFieldName().equals("serialVersionUID")) {

			myFields.add(f);
			if (obj.isFinal()) finalFields.add(f);
			if (obj.isStatic()) staticFields.add(f);
			if (obj.getName().equals("_jspx_dependants"))
				containerFields.add(f);
		}
	}

	@Override
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		if (!visitingField()) return;
		if (isInjectionAttribute(annotationClass)) {
			containerFields.add(XFactory.createXField(this));
		}


	}
	/**
	 * @param annotationClass
	 * @return
     */
	private boolean isInjectionAttribute(String annotationClass) {
		if ( annotationClass.startsWith("javax.annotation.") 
				|| annotationClass.startsWith("javax.ejb")|| annotationClass.equals("org.jboss.seam.annotations.In")  
                || annotationClass.startsWith("javax.persistence")
				|| annotationClass.endsWith("SpringBean")
				|| annotationClass.equals("com.google.inject.Inject"))
			return true;
        int lastDot = annotationClass.lastIndexOf('.');
		String lastPart = annotationClass.substring(lastDot+1);
		if (lastPart.startsWith("Inject")) return true;
		return false;
    }
	@Override
		 public void visit(ConstantValue obj) {
		// ConstantValue is an attribute of a field, so the instance variables
		// set during visitation of the Field are still valid here
		XField f = XFactory.createXField(this);
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
		if (DEBUG) System.out.println("Checking " + getClassName() + "." + obj.getName());
		if (getMethodName().equals("<init>")
			&& (obj.isPublic() 
				|| obj.isProtected() ))
			publicOrProtectedConstructor = true;
		pendingGetField = null;
		saState = 0;
		super.visit(obj);
		int flags = obj.getAccessFlags();
		if ((flags & ACC_NATIVE) != 0)
			hasNativeMethods = true;
	}

	boolean seenInvokeStatic;

	XField pendingGetField;
	int saState = 0;
	@Override
		 public void sawOpcode(int seen) {
		if (DEBUG) System.out.println(getPC() + ": " + OPCODE_NAMES[seen] + " " + saState);
		switch(saState) {
        case 0:
			if (seen == ALOAD_0)
				saState = 1;
			break;
        case 1:
			if (seen == ALOAD_0)
				saState = 2;
			else
                saState = 0;
			break;
		case 2:
			if (seen == GETFIELD)
                saState = 3;
			else
				saState = 0;
			break;
        case 3:
			if (seen == PUTFIELD)
				saState = 4;
			else
                saState = 0;
			break;
		}
		boolean selfAssignment = false;
        if (pendingGetField != null) {
			if (seen != PUTFIELD && seen != PUTSTATIC) 
			readFields.add(pendingGetField);
			else if ( XFactory.createReferencedXField(this).equals(pendingGetField) && (saState == 4 || seen == PUTSTATIC) ) 
                selfAssignment = true;
			else 
				readFields.add(pendingGetField);
			pendingGetField = null;
        }
		if (saState == 4) saState = 0;

		opcodeStack.mergeJumps(this);
		if (seen == INVOKESTATIC && getClassConstantOperand().equals("java/util/concurrent/atomic/AtomicReferenceFieldUpdater") && getNameConstantOperand().equals("newUpdater")) {
		   String fieldName = (String) opcodeStack.getStackItem(0).getConstant();
		   String fieldSignature = (String) opcodeStack.getStackItem(1).getConstant();
            String  fieldClass = (String) opcodeStack.getStackItem(2).getConstant();
			if (fieldName != null && fieldSignature != null && fieldClass != null) {
			   XField f = XFactory.createXField(fieldClass.replace('/','.'), fieldName, "L"+fieldSignature+";", false);
			   reflectiveFields.add(f);
             }

		}
		if (seen == INVOKESTATIC && getClassConstantOperand().equals("java/util/concurrent/atomic/AtomicIntegerFieldUpdater") && getNameConstantOperand().equals("newUpdater")) {
            String fieldName = (String) opcodeStack.getStackItem(0).getConstant();
			 String  fieldClass = (String) opcodeStack.getStackItem(1).getConstant();
			 if (fieldName != null && fieldClass != null) {
				XField f = XFactory.createXField(fieldClass.replace('/','.'), fieldName, "I", false);
                reflectiveFields.add(f);
			  }

		 }
        if (seen == INVOKESTATIC && getClassConstantOperand().equals("java/util/concurrent/atomic/AtomicLongFieldUpdater") && getNameConstantOperand().equals("newUpdater")) {
			String fieldName = (String) opcodeStack.getStackItem(0).getConstant();
			 String  fieldClass = (String) opcodeStack.getStackItem(1).getConstant();
			 if (fieldName != null && fieldClass != null) {
                XField f = XFactory.createXField(fieldClass.replace('/','.'), fieldName, "J", false);
				reflectiveFields.add(f);
			  }

         }


		if (seen == GETSTATIC) {
			XField f = XFactory.createReferencedXField(this);
					staticFieldsReadInThisMethod.add(f);
			}
		else if (seen == INVOKESTATIC) {
			seenInvokeStatic = true;
			}
		else if (seen == PUTSTATIC 
			&& !getMethod().isStatic()) {
			XField f = XFactory.createReferencedXField(this);
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
				|| seen == INVOKESPECIAL || seen==INVOKESTATIC )  {

			String sig = getSigConstantOperand();
			String invokedClassName = getClassConstantOperand();
			if (invokedClassName.equals(getClassName()) 
					&& (getMethodName().equals("<init>") || getMethodName().equals("<clinit>"))) {

				calledFromConstructors.add(getNameConstantOperand()+":"+sig);
			}
			int pos = PreorderVisitor.getNumberArguments(sig);
			if (opcodeStack.getStackDepth() > pos) {
				OpcodeStack.Item item = opcodeStack.getStackItem(pos);
				boolean superCall = seen == INVOKESPECIAL
				&&  !invokedClassName .equals(getClassName());

				if (DEBUG)
					System.out.println("In " + getFullyQualifiedMethodName()
							+ " saw call on " + item);



				boolean selfCall = item.getRegisterNumber() == 0 
				&& !superCall;
				if (selfCall && getMethodName().equals("<init>")) {
					sawSelfCallInConstructor = true;	
					if (DEBUG)
						System.out.println("Saw self call in " + getFullyQualifiedMethodName()  + " to " + invokedClassName + "." + getNameConstantOperand()
						);
				}
			}
		}

		if ((seen == IFNULL || seen == IFNONNULL) 
			&& opcodeStack.getStackDepth() > 0)  {
			OpcodeStack.Item item = opcodeStack.getStackItem(0);
			XField f = item.getXField();
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
				|| seen == IALOAD || seen == AALOAD || seen == BALOAD || seen == CALOAD || seen == SALOAD
				|| seen == IASTORE || seen == AASTORE  || seen == BASTORE || seen == CASTORE || seen == SASTORE
				|| seen == ARRAYLENGTH)  {
			int pos = 0;
			switch(seen) {
			case ARRAYLENGTH:
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
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				pos = 1;
				break;
			case IASTORE :
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				pos = 2;
				break;
			default: throw new RuntimeException("Impossible");
			}
			if (opcodeStack.getStackDepth() >= pos) {
			OpcodeStack.Item item = opcodeStack.getStackItem(pos);
			XField f = item.getXField();
			if (DEBUG) System.out.println("RRR: " + f + " " + nullTested.contains(f)  + " " + writtenInConstructorFields.contains(f) + " " +  writtenNonNullFields.contains(f));
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
			XField f = XFactory.createReferencedXField(this);
			pendingGetField = f;
			if (getMethodName().equals("readResolve") 
					&& seen == GETFIELD ) {
                writtenFields.add(f);
				writtenNonNullFields.add(f);
			}
			if (DEBUG) System.out.println("get: " + f);
			// readFields.add(f);
			if (!fieldAccess.containsKey(f))
				fieldAccess.put(f, SourceLineAnnotation.fromVisitedInstruction(this));
		} else if ((seen == PUTFIELD || seen == PUTSTATIC) && !selfAssignment) {
			XField f = XFactory.createReferencedXField(this);
			OpcodeStack.Item item = null;
			if (opcodeStack.getStackDepth() > 0) {
				item = opcodeStack.getStackItem(0);
				if (!item.isNull()) nullTested.add(f);
			}
			writtenFields.add(f);
			if (!fieldAccess.containsKey(f))
				fieldAccess.put(f, SourceLineAnnotation.fromVisitedInstruction(this));
			if (previousOpcode != ACONST_NULL || previousPreviousOpcode == GOTO )  {
				writtenNonNullFields.add(f);
				if (DEBUG) System.out.println("put nn: " + f);
			}
			else if (DEBUG) System.out.println("put: " + f);

			if ( getMethod().isStatic() == f.isStatic() && (
					calledFromConstructors.contains(getMethodName()+":"+getMethodSig())
					|| getMethodName().equals("<init>") 
					|| getMethodName().equals("init")  
					|| getMethodName().equals("init")  
					|| getMethodName().equals("initialize") 
					|| getMethodName().equals("<clinit>") 
					|| getMethod().isPrivate())) {
				writtenInConstructorFields.add(f);
				if (previousOpcode != ACONST_NULL || previousPreviousOpcode == GOTO ) 
					assumedNonNull.remove(f);
			} else {
				writtenOutsideOfConstructorFields.add(f);
			}


		}
		opcodeStack.sawOpcode(this, seen);
		previousPreviousOpcode = previousOpcode;
		previousOpcode = seen;
		if (false && DEBUG) {
		System.out.println("After " + OPCODE_NAMES[seen] + " opcode stack is");
		System.out.println(opcodeStack);
		}

	}

	static Pattern dontComplainAbout = Pattern.compile("class[$]");
	static Pattern withinAnonymousClass = Pattern.compile("[$][0-9].*[$]");
	@Override
		 public void report() {
		Set<String> fieldNamesSet = new HashSet<String>();
		for(XField f : writtenNonNullFields)
			fieldNamesSet.add(f.getName());
		if (DEBUG) {
			System.out.println("read fields:" );
			for(XField f : readFields) 
				System.out.println("  " + f);
			if (!containerFields.isEmpty()) {
				System.out.println("ejb3 fields:" );
				for(XField f : containerFields) 
					System.out.println("  " + f);
			}
			if (!reflectiveFields.isEmpty()) {
				System.out.println("reflective fields:" );
				for(XField f : reflectiveFields) 
                    System.out.println("  " + f);
			}


			System.out.println("written fields:" );
			for (XField f : writtenFields) 
				System.out.println("  " + f);
			System.out.println("written nonnull fields:" );
			for (XField f : writtenNonNullFields) 
				System.out.println("  " + f);

			System.out.println("assumed nonnull fields:" );
			for (XField f : assumedNonNull.keySet()) 
				System.out.println("  " + f);
		}
		// Don't report anything about ejb3Fields
		declaredFields.removeAll(containerFields);
		declaredFields.removeAll(reflectiveFields);

		TreeSet<XField> notInitializedInConstructors =
				new TreeSet<XField>(declaredFields);
		notInitializedInConstructors.retainAll(readFields);
		notInitializedInConstructors.retainAll(writtenFields);
		notInitializedInConstructors.retainAll(assumedNonNull.keySet());
		notInitializedInConstructors.removeAll(staticFields);
		notInitializedInConstructors.removeAll(writtenInConstructorFields);
		// notInitializedInConstructors.removeAll(staticFields);

		TreeSet<XField> readOnlyFields =
				new TreeSet<XField>(declaredFields);
		readOnlyFields.removeAll(writtenFields);

		readOnlyFields.retainAll(readFields);

		TreeSet<XField> nullOnlyFields =
			new TreeSet<XField>(declaredFields);
		nullOnlyFields.removeAll(writtenNonNullFields);

		nullOnlyFields.retainAll(readFields);

		Set<XField> writeOnlyFields = declaredFields;
		writeOnlyFields.removeAll(readFields);


		for (XField f : notInitializedInConstructors) {
			String fieldName = f.getName();
			String className = f.getClassName();
			String fieldSignature = f.getSignature();
			if (f.isResolved()
					&& !fieldsOfNativeClasses.contains(f)
					&& (fieldSignature.charAt(0) == 'L' || fieldSignature.charAt(0) == '[')
					) {
				int priority = LOW_PRIORITY;
				if (assumedNonNull.containsKey(f)) 
				bugReporter.reportBug(new BugInstance(this,
						"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
						priority)
						.addClass(className)
						.addField(f));
			}
		}


		for (XField f : readOnlyFields) {
			String fieldName = f.getName();
			String className = f.getClassName();
			String fieldSignature = f.getSignature();
			if (f.isResolved()
					&& !fieldsOfNativeClasses.contains(f)) {
				int priority = NORMAL_PRIORITY;
				if (!(fieldSignature.charAt(0) == 'L' || fieldSignature.charAt(0) == '['))
					priority++;

				bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this,
						"UWF_UNWRITTEN_FIELD",
						priority),f));
			}

		}
		for (XField f : nullOnlyFields) {
			String fieldName = f.getName();
			String className = f.getClassName();
			String fieldSignature = f.getSignature();
			if (DEBUG) {
				System.out.println("Null only: " + f);
				System.out.println("   : " + assumedNonNull.containsKey(f));
				System.out.println("   : " + fieldsOfSerializableOrNativeClassed.contains(f));
				System.out.println("   : " + fieldNamesSet.contains(f.getName()));
				System.out.println("   : " + abstractClasses.contains(f.getClassName()));
				System.out.println("   : " + hasNonAbstractSubClass.contains(f.getClassName()));
				System.out.println("   : " + f.isResolved());
			}
			if (!f.isResolved()) continue;
			if (fieldsOfNativeClasses.contains(f)) continue;
			if (DEBUG) {
				System.out.println("Ready to report");
			}
			int priority = NORMAL_PRIORITY;
			if (abstractClasses.contains(f.getClassName())) {
				priority++;
				if (! hasNonAbstractSubClass.contains(f.getClassName())) priority++;
			}
			// if (fieldNamesSet.contains(f.getName())) priority++;
			if (assumedNonNull.containsKey(f)) {
				priority--;
				for (ProgramPoint p : assumedNonNull.get(f))
					bugReporter.reportBug(new BugInstance(this,
							"NP_UNWRITTEN_FIELD",
							NORMAL_PRIORITY)
							.addClassAndMethod(p.method)
							.addField(f)
							.addSourceLine(p.sourceLine)
					);
			} else {
				if (f.isStatic()) priority++;
				if (finalFields.contains(f)) priority++;
				if (fieldsOfSerializableOrNativeClassed.contains(f)) priority++;
			}
			if (!readOnlyFields.contains(f)) 
				bugReporter.reportBug(
						addClassFieldAndAccess(new BugInstance(this,"UWF_NULL_FIELD",priority), f).lowerPriorityIfDeprecated()
					);
		}

		for (XField f : writeOnlyFields) {
			String fieldName = f.getName();
			String className = f.getClassName();
			int lastDollar =
					Math.max(className.lastIndexOf('$'),
							className.lastIndexOf('+'));
			boolean isAnonymousInnerClass =
					(lastDollar > 0)
							&& (lastDollar < className.length() - 1)
							&& Character.isDigit(className.charAt(lastDollar+1));

			if (DEBUG) {
				System.out.println("Checking write only field " + className
						+ "." + fieldName
						+ "\t" + constantFields.contains(f)
						+ "\t" + f.isStatic()
				);
			}
			if (!f.isResolved()) continue;
			if (dontComplainAbout.matcher(fieldName).find()) continue;
			if (fieldName.startsWith("this$")
					|| fieldName.startsWith("this+")) {
				String outerClassName = className.substring(0, lastDollar);

				try {
					JavaClass outerClass = Repository.lookupClass(outerClassName);
					if (classHasParameter(outerClass)) continue;
				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
				}
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

						boolean b = withinAnonymousClass.matcher(getDottedClassName()).find();
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
						bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this,
								"SS_SHOULD_BE_STATIC",
								NORMAL_PRIORITY), f));
				} else if (fieldsOfSerializableOrNativeClassed.contains(f)) {
					// ignore it
				} else if (!writtenFields.contains(f) && f.isResolved())
					bugReporter.reportBug(new BugInstance(this, "UUF_UNUSED_FIELD", NORMAL_PRIORITY)
							.addClass(className)
							.addField(f).lowerPriorityIfDeprecated());
				else if (f.getName().toLowerCase().indexOf("guardian") < 0) {
					int priority = NORMAL_PRIORITY;
					if (f.isStatic()) priority++;
					if (finalFields.contains(f)) priority++;
					bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this, "URF_UNREAD_FIELD", priority),f));
				}
			}
		}

	}
	/**
	 * @param instance
	 * @return
	 */
	private BugInstance addClassFieldAndAccess(BugInstance instance, XField f) {
		instance.addClass(f.getClassName()).addField(f);
		if (fieldAccess.containsKey(f))
			instance.add(fieldAccess.get(f));
		return instance;
	}

}
