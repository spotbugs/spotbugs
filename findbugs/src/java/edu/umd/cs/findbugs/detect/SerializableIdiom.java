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


import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

public class SerializableIdiom extends OpcodeStackDetector
		{

	final static boolean reportTransientFieldOfNonSerializableClass =
		SystemProperties.getBoolean("reportTransientFieldOfNonSerializableClass");

	boolean sawSerialVersionUID;
	boolean isSerializable, implementsSerializableDirectly;
	boolean isExternalizable;
	boolean isGUIClass;
	boolean foundSynthetic;
	boolean seenTransientField;
	boolean foundSynchronizedMethods;
	boolean writeObjectIsSynchronized;
	private BugReporter bugReporter;
	boolean isAbstract;
	private List<BugInstance> fieldWarningList = new LinkedList<BugInstance>();
	private HashMap<String, XField> fieldsThatMightBeAProblem = new HashMap<String, XField>();
	private HashMap<String, XField> transientFields = new HashMap<String, XField>();
	private HashMap<String, Integer> transientFieldsUpdates = new HashMap<String, Integer>();
	private HashSet<String> transientFieldsSetInConstructor = new HashSet<String>();
	private HashSet<String> transientFieldsSetToDefaultValueInConstructor = new HashSet<String>();

	private boolean sawReadExternal;
	private boolean sawWriteExternal;
	private boolean sawReadObject;
	private boolean sawReadResolve;
	private boolean sawWriteObject;
	private boolean superClassImplementsSerializable;
	private boolean hasPublicVoidConstructor;
	private boolean superClassHasVoidConstructor;
	private boolean directlyImplementsExternalizable;
	//private JavaClass serializable;
	//private JavaClass collection;
	//private JavaClass map;
	//private boolean isRemote;

	public SerializableIdiom(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
		 public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
		flush();
	}

	private void flush() {
		if (!isAbstract &&
				!((sawReadExternal && sawWriteExternal) || (sawReadObject && sawWriteObject))) {
			for (BugInstance aFieldWarningList : fieldWarningList)
				bugReporter.reportBug(aFieldWarningList);
		}
		fieldWarningList.clear();
	}

	static final Pattern anonymousInnerClassNamePattern =
			Pattern.compile(".+\\$\\d+");
	boolean isAnonymousInnerClass;
	boolean innerClassHasOuterInstance;
	private boolean isEnum;
	@Override
		 public void visit(JavaClass obj) {
		String superClassname = obj.getSuperclassName();
		// System.out.println("superclass of " + getClassName() + " is " + superClassname);
		isEnum = superClassname.equals("java.lang.Enum");
		if (isEnum) return;
		int flags = obj.getAccessFlags();
		isAbstract = (flags & ACC_ABSTRACT) != 0
				|| (flags & ACC_INTERFACE) != 0;
		isAnonymousInnerClass 
		  = anonymousInnerClassNamePattern
			.matcher(getClassName()).matches();
		innerClassHasOuterInstance = false;
		for(Field f : obj.getFields()) {
			if (f.getName().equals("this$0")) {
				innerClassHasOuterInstance = true;
				break;
			}
		}

		sawSerialVersionUID = false;
		isSerializable = implementsSerializableDirectly = false;
		isExternalizable = false;
		directlyImplementsExternalizable = false;
		isGUIClass = false;
		seenTransientField = false;
		// boolean isEnum = obj.getSuperclassName().equals("java.lang.Enum");
		fieldsThatMightBeAProblem.clear();
		transientFields.clear();
		transientFieldsUpdates.clear();
		transientFieldsSetInConstructor.clear();
		transientFieldsSetToDefaultValueInConstructor.clear();
		//isRemote = false;

		// Does this class directly implement Serializable?
		String[] interface_names = obj.getInterfaceNames();
		for (String interface_name : interface_names) {
			if (interface_name.equals("java.io.Externalizable")) {
				directlyImplementsExternalizable = true;
				isExternalizable = true;
				// System.out.println("Directly implements Externalizable: " + betterClassName);
			} else if (interface_name.equals("java.io.Serializable")) {
				implementsSerializableDirectly = true;
				isSerializable = true;
				break;
			}
		}

		// Does this class indirectly implement Serializable?
		if (!isSerializable) {
				if (Subtypes2.instanceOf(obj, "java.io.Externalizable"))
					isExternalizable = true;
				if (Subtypes2.instanceOf(obj, "java.io.Serializable"))
					isSerializable = true;

		}

		hasPublicVoidConstructor = false;
		superClassHasVoidConstructor = true;
		superClassImplementsSerializable = isSerializable && !implementsSerializableDirectly;
		ClassDescriptor superclassDescriptor = getXClass().getSuperclassDescriptor();
		if (superclassDescriptor != null)
			try {
			XClass superXClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, superclassDescriptor);
			if (superXClass != null) {
				superClassImplementsSerializable 
				= AnalysisContext.currentAnalysisContext().getSubtypes2().isSubtype(superXClass.getClassDescriptor(),
						DescriptorFactory.createClassDescriptor("java/io/Serializable"));
				superClassHasVoidConstructor = false;
				for (XMethod m : superXClass.getXMethods()) {
					if (m.getName().equals("<init>")
							&& m.getSignature().equals("()V")
							&& !m.isPrivate()
							) {
						superClassHasVoidConstructor = true;
						break;
					}
				}
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
		} catch (CheckedAnalysisException e) {
	       bugReporter.logError("huh", e);
        }


		// Is this a GUI  or other class that is rarely serialized?

			isGUIClass = !directlyImplementsExternalizable && !implementsSerializableDirectly && 
				(Subtypes2.instanceOf(obj, "java.lang.Throwable")
						|| Subtypes2.instanceOf(obj, "java.awt.Component")
						|| Subtypes2.instanceOf(obj, "java.awt.Component$AccessibleAWTComponent")
						|| Subtypes2.instanceOf(obj, "java.awt.event.ActionListener")
						|| Subtypes2.instanceOf(obj, "java.util.EventListener"))
					;
	

		foundSynthetic = false;
		foundSynchronizedMethods = false;
		writeObjectIsSynchronized = false;

		sawReadExternal = sawWriteExternal = sawReadObject = sawReadResolve = sawWriteObject = false;
	}

	@Override
		 public void visitAfter(JavaClass obj) {
		if (isEnum) return;
		if (false) {
			System.out.println(getDottedClassName());
			System.out.println("  hasPublicVoidConstructor: " + hasPublicVoidConstructor);
			System.out.println("  superClassHasVoidConstructor: " + superClassHasVoidConstructor);
			System.out.println("  isExternalizable: " + isExternalizable);
			System.out.println("  isSerializable: " + isSerializable);
			System.out.println("  isAbstract: " + isAbstract);
			System.out.println("  superClassImplementsSerializable: " + superClassImplementsSerializable);
		}
		if (isSerializable && !sawReadObject && !sawReadResolve && seenTransientField) {
			for(Map.Entry<String,Integer> e : transientFieldsUpdates.entrySet()) {

					XField fieldX = transientFields.get(e.getKey());
					int priority = NORMAL_PRIORITY;
					if (transientFieldsSetInConstructor.contains(e.getKey()))
						priority--;
					else {
						if (isGUIClass) priority++;
						if (e.getValue() < 3) 
							priority++;
						if (transientFieldsSetToDefaultValueInConstructor.contains(e.getKey()))
							priority++;
					}
					try {
						double isSerializable = DeepSubtypeAnalysis.isDeepSerializable(fieldX.getSignature());
						if (isSerializable < 0.6) priority++;
					} catch (ClassNotFoundException e1) {
						// ignore it
					}

					bugReporter.reportBug(new BugInstance(this, "SE_TRANSIENT_FIELD_NOT_RESTORED",
							priority )
							.addClass(getThisClass())
							.addField(fieldX));

			}

		}
		if (isSerializable && !isExternalizable
				&& !superClassHasVoidConstructor
				&& !superClassImplementsSerializable) {
			int priority = implementsSerializableDirectly|| seenTransientField ? HIGH_PRIORITY : 
				( sawSerialVersionUID ?  NORMAL_PRIORITY : LOW_PRIORITY);
			if (isGUIClass) priority++;
			bugReporter.reportBug(new BugInstance(this, "SE_NO_SUITABLE_CONSTRUCTOR", priority)
					.addClass(getThisClass().getClassName()));
		}
		// Downgrade class-level warnings if it's a GUI class.
		int priority = isGUIClass ? LOW_PRIORITY : NORMAL_PRIORITY;
		if (obj.getClassName().endsWith("_Stub")) priority++;

		if (isExternalizable && !hasPublicVoidConstructor && !isAbstract)
			bugReporter.reportBug(new BugInstance(this, "SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION",
					directlyImplementsExternalizable ?
					HIGH_PRIORITY : NORMAL_PRIORITY)
					.addClass(getThisClass().getClassName()));
		if (!foundSynthetic) priority++;
		if (seenTransientField) priority--;
		if (!isAnonymousInnerClass 
			&& !isExternalizable && !isGUIClass && !obj.isAbstract()
				&& isSerializable && !isAbstract && !sawSerialVersionUID)
			bugReporter.reportBug(new BugInstance(this, "SE_NO_SERIALVERSIONID", priority).addClass(this));

		if (writeObjectIsSynchronized && !foundSynchronizedMethods)
			bugReporter.reportBug(new BugInstance(this, "WS_WRITEOBJECT_SYNC", LOW_PRIORITY).addClass(this));
	}

	@Override
		 public void visit(Method obj) {

		int accessFlags = obj.getAccessFlags();
		boolean isSynchronized = (accessFlags & ACC_SYNCHRONIZED) != 0;
		if (getMethodName().equals("<init>") && getMethodSig().equals("()V")
				&& (accessFlags & ACC_PUBLIC) != 0
		)
			hasPublicVoidConstructor = true;
		if (!getMethodName().equals("<init>")
				&& isSynthetic(obj))
			foundSynthetic = true;
		// System.out.println(methodName + isSynchronized);

		if (getMethodName().equals("readExternal")
				&& getMethodSig().equals("(Ljava/io/ObjectInput;)V")) {
			sawReadExternal = true;
			if (false && !obj.isPrivate())
				System.out.println("Non-private readExternal method in: " + getDottedClassName());
		} else if (getMethodName().equals("writeExternal")
				&& getMethodSig().equals("(Ljava/io/Objectoutput;)V")) {
			sawWriteExternal = true;
			if (false && !obj.isPrivate())
				System.out.println("Non-private writeExternal method in: " + getDottedClassName());
		}
		else if (getMethodName().equals("readResolve")
				&& getMethodSig().startsWith("()")
				&& isSerializable) {
			sawReadResolve = true;
			if (!getMethodSig().equals("()Ljava/lang/Object;"))
				bugReporter.reportBug(new BugInstance(this, "SE_READ_RESOLVE_MUST_RETURN_OBJECT", HIGH_PRIORITY)
						.addClassAndMethod(this));

		}else if (getMethodName().equals("readObject")
				&& getMethodSig().equals("(Ljava/io/ObjectInputStream;)V")
				&& isSerializable) {
			sawReadObject = true;
			if (!obj.isPrivate())
				bugReporter.reportBug(new BugInstance(this, "SE_METHOD_MUST_BE_PRIVATE", HIGH_PRIORITY)
						.addClassAndMethod(this));

		} else if (getMethodName().equals("readObjectNoData")
				&& getMethodSig().equals("()V")
				&& isSerializable) {

			if (!obj.isPrivate())
				bugReporter.reportBug(new BugInstance(this, "SE_METHOD_MUST_BE_PRIVATE", HIGH_PRIORITY)
						.addClassAndMethod(this));

		}else if (getMethodName().equals("writeObject")
				&& getMethodSig().equals("(Ljava/io/ObjectOutputStream;)V")
				&& isSerializable) {
			sawReadObject = true;
			if (!obj.isPrivate())
				bugReporter.reportBug(new BugInstance(this, "SE_METHOD_MUST_BE_PRIVATE", HIGH_PRIORITY)
						.addClassAndMethod(this));
		}

		if (isSynchronized) {
		if (getMethodName().equals("readObject") &&
				getMethodSig().equals("(Ljava/io/ObjectInputStream;)V") &&
				isSerializable)
			bugReporter.reportBug(new BugInstance(this, "RS_READOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
		else if (getMethodName().equals("writeObject")
				&& getMethodSig().equals("(Ljava/io/ObjectOutputStream;)V")
				&& isSerializable)
			writeObjectIsSynchronized = true;
		else
			foundSynchronizedMethods = true;
		}
		super.visit(obj);

	}

	boolean isSynthetic(FieldOrMethod obj) {
		Attribute[] a = obj.getAttributes();
		for (Attribute aA : a)
			if (aA instanceof Synthetic) return true;
		return false;
	}


	@Override
		 public void visit(Code obj) {
		if (isSerializable) {
			super.visit(obj);
		}
	}
	@Override
	public void sawOpcode(int seen) {
		if (seen == PUTFIELD) {
			String nameOfClass = getClassConstantOperand();
			if ( getClassName().equals(nameOfClass))  {
				Item first = stack.getStackItem(0);
				boolean isPutOfDefaultValue = first.isNull() || first.isInitialParameter();
				if (!isPutOfDefaultValue && first.getConstant() != null) {
					Object constant = first.getConstant();
					if (constant instanceof Number && ((Number)constant).intValue() == 0 
							|| constant.equals(Boolean.FALSE))
						isPutOfDefaultValue = true;
				}

				if (isPutOfDefaultValue) {
					String nameOfField = getNameConstantOperand();
					if (getMethodName().equals("<init>")) transientFieldsSetToDefaultValueInConstructor.add(nameOfField);
				} else {
					String nameOfField = getNameConstantOperand();
					if (transientFieldsUpdates.containsKey(nameOfField) ) {
						if (getMethodName().equals("<init>")) transientFieldsSetInConstructor.add(nameOfField);
						else transientFieldsUpdates.put(nameOfField, transientFieldsUpdates.get(nameOfField)+1);
					} else if (fieldsThatMightBeAProblem.containsKey(nameOfField)) {
						try {

							JavaClass classStored = first.getJavaClass();
							double isSerializable = DeepSubtypeAnalysis
							.isDeepSerializable(classStored);
							if (isSerializable <= 0.2) {
								XField f = fieldsThatMightBeAProblem.get(nameOfField);

								String sig = f.getSignature();
								// System.out.println("Field signature: " + sig);
								// System.out.println("Class stored: " +
								// classStored.getClassName());
								String genSig = "L"
									+ classStored.getClassName().replace('.', '/')
									+ ";";
								if (!sig.equals(genSig)) {
									double bias = 0.0;
									if (!getMethodName().equals("<init>")) bias = 1.0;
									int priority = computePriority(isSerializable, bias);

									fieldWarningList.add(new BugInstance(this,
											"SE_BAD_FIELD_STORE", priority).addClass(
													getThisClass().getClassName()).addField(f)
													.addType(genSig).describe("TYPE_FOUND").addSourceLine(this));
								}
							}
						} catch (Exception e) {
							// ignore it
						}
					}
				}
			}

		}

	}
	
	@Override
	public void visit(Field obj) {
		int flags = obj.getAccessFlags();

		if (obj.isTransient()) {
			if (isSerializable) {
				seenTransientField = true;
				transientFields.put(obj.getName(), XFactory.createXField(this));
				transientFieldsUpdates.put(obj.getName(), 0);
			} else if (reportTransientFieldOfNonSerializableClass) {
				bugReporter.reportBug(new BugInstance(this, "SE_TRANSIENT_FIELD_OF_NONSERIALIZABLE_CLASS", NORMAL_PRIORITY)
				.addClass(this)
				.addVisitedField(this));
			}
		}
		else if (getClassName().indexOf("ObjectStreamClass") == -1
				&& isSerializable
				&& !isExternalizable
				&& getFieldSig().indexOf("L") >= 0 && !obj.isTransient() && !obj.isStatic()) {
			try {

				double isSerializable = DeepSubtypeAnalysis.isDeepSerializable(getFieldSig());
				if (isSerializable < 1.0)
					fieldsThatMightBeAProblem.put(obj.getName(), XFactory.createXField(this));
				if (isSerializable < 0.9) {

					// Priority is LOW for GUI classes (unless explicitly marked Serializable),
					// HIGH if the class directly implements Serializable,
					// NORMAL otherwise.
					int priority = computePriority(isSerializable, 0);
					if (priority > NORMAL_PRIORITY
							&& obj.getName().startsWith("this$"))
							priority = NORMAL_PRIORITY;
					else if (innerClassHasOuterInstance) {
						if (isAnonymousInnerClass) priority+=2;
						else priority+=1;
					}
					if (isGUIClass) priority++;
					if (false)
					System.out.println("SE_BAD_FIELD: " + getThisClass().getClassName()
						+" " +  obj.getName()	
						+" " +  isSerializable
						+" " +  implementsSerializableDirectly
						+" " +  sawSerialVersionUID
						+" " +  isGUIClass);
					// Report is queued until after the entire class has been seen.

					if (obj.getName().equals("this$0"))
						fieldWarningList.add(new BugInstance(this, "SE_BAD_FIELD_INNER_CLASS", priority)
							.addClass(getThisClass().getClassName()));
						else if (isSerializable < 0.9) fieldWarningList.add(new BugInstance(this, "SE_BAD_FIELD", priority)
							.addClass(getThisClass().getClassName())
							.addField(getDottedClassName(), obj.getName(), getFieldSig(), false));
				} else if (!isGUIClass && obj.getName().equals("this$0"))
					fieldWarningList.add(new BugInstance(this, "SE_INNER_CLASS",
							implementsSerializableDirectly ? NORMAL_PRIORITY : LOW_PRIORITY)
					.addClass(getThisClass().getClassName()));
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}

		if (!getFieldName().startsWith("this")
				&& isSynthetic(obj))
			foundSynthetic = true;
		if (!getFieldName().equals("serialVersionUID")) return;
		int mask = ACC_STATIC | ACC_FINAL;
		if (!getFieldSig().equals("I")
				&& !getFieldSig().equals("J"))
			return;
		if ((flags & mask) == mask
				&& getFieldSig().equals("I")) {
			bugReporter.reportBug(new BugInstance(this, "SE_NONLONG_SERIALVERSIONID", LOW_PRIORITY)
					.addClass(this)
					.addVisitedField(this));
			sawSerialVersionUID = true;
			return;
		} else if ((flags & ACC_STATIC) == 0) {
			bugReporter.reportBug(new BugInstance(this, "SE_NONSTATIC_SERIALVERSIONID", NORMAL_PRIORITY)
					.addClass(this)
					.addVisitedField(this));
			return;
		} else if ((flags & ACC_FINAL) == 0) {
			bugReporter.reportBug(new BugInstance(this, "SE_NONFINAL_SERIALVERSIONID", NORMAL_PRIORITY)
					.addClass(this)
					.addVisitedField(this));
			return;
		}
		sawSerialVersionUID = true;
	}

	private int computePriority(double isSerializable, double bias) {
		int priority = (int)(1.9+isSerializable*3 + bias);

		if (implementsSerializableDirectly || sawSerialVersionUID || sawReadObject)
			priority--;
		if (!implementsSerializableDirectly && priority == HIGH_PRIORITY)
			priority = NORMAL_PRIORITY;
		return priority;
	}


}
