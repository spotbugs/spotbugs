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


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ProgramPoint;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.Bag;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiMap;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class UnreadFields extends OpcodeStackDetector  {
	private static final boolean DEBUG = SystemProperties.getBoolean("unreadfields.debug");

	public boolean isContainerField(XField f) {
		return containerFields.contains(f);
	}
	Map<XField,Set<ProgramPoint> >
		assumedNonNull = new HashMap<XField,Set<ProgramPoint>>();
	Map<XField,ProgramPoint >
		threadLocalAssignedInConstructor = new HashMap<XField,ProgramPoint>();

	Set<XField> nullTested = new HashSet<XField>();
	Set<XField> containerFields = new TreeSet<XField>();
	MultiMap<XField,String> unknownAnnotation = new MultiMap<XField,String>(LinkedList.class);
	
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
	/**
	 * Only for fields that are either:
	 *   * read only
	 *   * written only
	 *   * written null and read
	 */
	Map<XField,SourceLineAnnotation> fieldAccess = new HashMap<XField, SourceLineAnnotation>();
	Set<XField> writtenNonNullFields = new HashSet<XField>();
	Set<String> calledFromConstructors = new HashSet<String>();
	Set<XField> writtenInConstructorFields = new HashSet<XField>();
	Set<XField> writtenInInitializationFields = new HashSet<XField>();
	Set<XField> writtenOutsideOfInitializationFields = new HashSet<XField>();
	
	Set<XField> readFields = new HashSet<XField>();
	Set<XField> constantFields = new HashSet<XField>();
	Set<String> needsOuterObjectInConstructor = new HashSet<String>();
	Set<String> innerClassCannotBeStatic = new HashSet<String>();
	boolean hasNativeMethods;
	boolean isSerializable;
	boolean sawSelfCallInConstructor;
	private final BugReporter bugReporter;
	private final BugAccumulator bugAccumulator;
	boolean publicOrProtectedConstructor;

	public Set<? extends XField> getReadFields() {
		return readFields;
	}
	public Set<? extends XField> getWrittenFields() {
		return writtenFields;
	}
	
	public boolean isWrittenOutsideOfInitialization(XField f) {
		return writtenOutsideOfInitializationFields.contains(f);
	}

	public boolean isWrittenDuringInitialization(XField f) {
		return writtenInInitializationFields.contains(f);
	}
	public boolean isWrittenInConstructor(XField f) {
		return writtenInConstructorFields.contains(f);
	}

	static final int doNotConsider = ACC_PUBLIC | ACC_PROTECTED;

	ClassDescriptor externalizable = DescriptorFactory.createClassDescriptor(java.io.Externalizable.class);
	ClassDescriptor serializable = DescriptorFactory.createClassDescriptor(java.io.Serializable.class);
	ClassDescriptor remote = DescriptorFactory.createClassDescriptor(java.rmi.Remote.class);
	
	public UnreadFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.bugAccumulator = new BugAccumulator(bugReporter);
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
		boolean superClassIsObject = "java.lang.Object".equals(obj.getSuperclassName());
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
		if ((!superClassIsObject || interface_names.length > 0) && !isSerializable) {
			try {
				Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
				ClassDescriptor desc = DescriptorFactory.createClassDescriptor(obj);
				if (subtypes2.getSubtypes(serializable).contains(desc)
						|| subtypes2.getSubtypes(externalizable).contains(desc)
						|| subtypes2.getSubtypes(remote).contains(desc)) {
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
		if (hasNativeMethods) {
			fieldsOfSerializableOrNativeClassed.addAll(myFields);
			fieldsOfNativeClasses.addAll(myFields);
		}
		if (isSerializable) {
			fieldsOfSerializableOrNativeClassed.addAll(myFields);
		}
		if (sawSelfCallInConstructor) {
			myFields.removeAll(writtenInConstructorFields);
			writtenInInitializationFields.addAll(myFields);
		}
		myFields.clear();
		allMyFields.clear();
		calledFromConstructors.clear();
	}

	@Override
		 public void visit(Field obj) {
		super.visit(obj);
		XField f = XFactory.createXField(this);
		allMyFields.add(f);
		String signature = obj.getSignature();
		int flags = obj.getAccessFlags();
		if ((flags & doNotConsider) == 0
				&& !getFieldName().equals("serialVersionUID")) {

			myFields.add(f);
			if (obj.getName().equals("_jspx_dependants"))
				containerFields.add(f);
		}
		if (isSeleniumWebElement(signature))
			containerFields.add(f);
	}
	/**
     * @param signature
     * @return
     */
    public static boolean isSeleniumWebElement(String signature) {
	    return signature.equals("Lorg/openqa/selenium/RenderedWebElement;") || signature.equals("Lorg/openqa/selenium/WebElement;");
    }

	@Override
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		if (!visitingField()) return;
		if (isInjectionAttribute(annotationClass)) {
			containerFields.add(XFactory.createXField(this));
		}
		if (!annotationClass.startsWith("edu.umd.cs.findbugs") && !annotationClass.startsWith("javax.lang"))
			unknownAnnotation.add(XFactory.createXField(this), annotationClass);

	}
	public static boolean isInjectionAttribute(@DottedClassName String annotationClass) {
		if ( annotationClass.startsWith("javax.annotation.") 
				|| annotationClass.startsWith("javax.ejb")
				|| annotationClass.equals("org.jboss.seam.annotations.In")  
				|| annotationClass.startsWith("javax.persistence")
				|| annotationClass.endsWith("SpringBean")
				|| annotationClass.equals("com.google.inject.Inject")
				|| annotationClass.startsWith("com.google.") 
				   && annotationClass.endsWith(".Bind") && annotationClass.hashCode() == -243168318
				|| annotationClass.startsWith("org.nuxeo.common.xmap.annotation")
				|| annotationClass.startsWith("com.google.gwt.uibinder.client")
				|| annotationClass.startsWith("org.springframework.beans.factory.annotation"))
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

	private int previousOpcode;
	private int previousPreviousOpcode;
	@Override
		 public void visit(Code obj) {

		count_aload_1 = 0;
		previousOpcode = -1;
		previousPreviousOpcode = -1;
		nullTested.clear();
		seenInvokeStatic = false;
		seenMonitorEnter = getMethod().isSynchronized();
		staticFieldsReadInThisMethod.clear();
		super.visit(obj);
		if (getMethodName().equals("<init>") && count_aload_1 > 1
				&& (getClassName().indexOf('$') >= 0
				|| getClassName().indexOf('+') >= 0)) {
			needsOuterObjectInConstructor.add(getDottedClassName());
			// System.out.println(betterClassName + " needs outer object in constructor");
		}
		bugAccumulator.reportAccumulatedBugs();
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
	boolean seenMonitorEnter;

	XField pendingGetField;
	int saState = 0;
	@Override
		 public void sawOpcode(int seen) {
		if (DEBUG) System.out.println(getPC() + ": " + OPCODE_NAMES[seen] + " " + saState);
		if (seen == MONITORENTER)
			seenMonitorEnter = true;
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

		
		if (seen == INVOKESTATIC && getClassConstantOperand().equals("java/util/concurrent/atomic/AtomicReferenceFieldUpdater") && getNameConstantOperand().equals("newUpdater")) {
		   String fieldName = (String) stack.getStackItem(0).getConstant();
		   String fieldSignature = (String) stack.getStackItem(1).getConstant();
			String  fieldClass = (String) stack.getStackItem(2).getConstant();
			if (fieldName != null && fieldSignature != null && fieldClass != null) {
				XField f = XFactory.createXField(fieldClass.replace('/','.'), 
					   fieldName, 
					   ClassName.toSignature(fieldSignature), 
					   false);
			   reflectiveFields.add(f);
			 }

		}
		if (seen == INVOKESTATIC && getClassConstantOperand().equals("java/util/concurrent/atomic/AtomicIntegerFieldUpdater") && getNameConstantOperand().equals("newUpdater")) {
			String fieldName = (String) stack.getStackItem(0).getConstant();
			 String  fieldClass = (String) stack.getStackItem(1).getConstant();
			 if (fieldName != null && fieldClass != null) {
				XField f = XFactory.createXField(fieldClass.replace('/','.'), fieldName, "I", false);
				reflectiveFields.add(f);
			  }

		 }
		if (seen == INVOKESTATIC && getClassConstantOperand().equals("java/util/concurrent/atomic/AtomicLongFieldUpdater") && getNameConstantOperand().equals("newUpdater")) {
			String fieldName = (String) stack.getStackItem(0).getConstant();
			 String  fieldClass = (String) stack.getStackItem(1).getConstant();
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
					if (f.getName().indexOf("class$") != 0) {
				int priority = LOW_PRIORITY;				
				if (!publicOrProtectedConstructor)
					priority--;
				if (seenMonitorEnter)
					priority++;
				if (!seenInvokeStatic 
					 && staticFieldsReadInThisMethod.isEmpty())
					priority--;
				if (getThisClass().isPublic() 
					&& getMethod().isPublic())
					priority--;
				if (getThisClass().isPrivate() 
					|| getMethod().isPrivate())
					priority++;
				if (getClassName().indexOf('$') != -1 || getMethod().isSynthetic() || f.isSynthetic() || f.getName().indexOf('$') >= 0)
					priority++;

				// Decrease priority for boolean fileds used to control debug/test settings
				if(f.getName().indexOf("DEBUG") >= 0 || f.getName().indexOf("VERBOSE") >= 0
						&& f.getSignature().equals("Z")){
					priority ++;
					priority ++;
				}
				// Eclipse bundles which implements start/stop *very* often assigns static instances there
				if (getMethodName().equals("start") || getMethodName().equals("stop")
				        && getMethodSig().equals("(Lorg/osgi/framework/BundleContext;)V")) {
					try {
                    	JavaClass bundleClass = Repository.lookupClass("org.osgi.framework.BundleActivator");
                    	if(getThisClass().instanceOf(bundleClass)){
                    		priority ++;
                    	}
                    	if(f.isReferenceType()){
                    		FieldDescriptor fieldInfo = f.getFieldDescriptor();
                    		String dottedClass = DeepSubtypeAnalysis.getComponentClass(fieldInfo.getSignature());
                    		JavaClass fieldClass = Repository.lookupClass(dottedClass);
                    		if(fieldClass != null && fieldClass.instanceOf(bundleClass)){
                    			// the code "plugin = this;" unfortunately exists in the
                    			// template for new Eclipse plugin classes, so nearly every one
                    			// plugin has this pattern => decrease to very low prio
                    			priority ++;
                    		}
                    	}
                    } catch (ClassNotFoundException e) {
                    	bugReporter.reportMissingClass(e);
                    }
				}				
				bugAccumulator.accumulateBug(
						new BugInstance(this, 
						"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
						priority)
						.addClassAndMethod(this)
						.addField(f), 
					this);
						
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
			if (stack.getStackDepth() > pos) {
				OpcodeStack.Item item = stack.getStackItem(pos);
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
			&& stack.getStackDepth() > 0)  {
			OpcodeStack.Item item = stack.getStackItem(0);
			XField f = item.getXField();
			if (f != null) {
				nullTested.add(f);
				if (DEBUG)
				System.out.println(f + " null checked in " +
					getFullyQualifiedMethodName());
				}
			}
		
		if ((seen == IF_ACMPEQ || seen == IF_ACMPNE) 
				&& stack.getStackDepth() >= 2)  {
				OpcodeStack.Item item0 = stack.getStackItem(0);
				OpcodeStack.Item item1 = stack.getStackItem(1);
				XField field1 = item1.getXField();
				if (item0.isNull() && field1 != null)
					nullTested.add(field1);
                else {
	                XField field0 = item0.getXField();
	                if (item1.isNull() && field0 != null)
	                	nullTested.add(field0);
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
			if (stack.getStackDepth() >= pos) {
			OpcodeStack.Item item = stack.getStackItem(pos);
			XField f = item.getXField();
			if (DEBUG) System.out.println("RRR: " + f + " " + nullTested.contains(f)  + " " + writtenInConstructorFields.contains(f) + " " +  writtenNonNullFields.contains(f));
			if (f != null && !nullTested.contains(f) 
					&& ! ((writtenInConstructorFields.contains(f) || writtenInInitializationFields.contains(f))
						 && writtenNonNullFields.contains(f))
					) {
				ProgramPoint p = new ProgramPoint(this);
				Set <ProgramPoint> s = assumedNonNull.get(f);
				if (s == null)
					s = Collections.singleton(p);
				else 
					s = Util.addTo(s, p);
				assumedNonNull.put(f,s);
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
			if (writtenFields.contains(f))	
				fieldAccess.remove(f);
			else if (!fieldAccess.containsKey(f))
				fieldAccess.put(f, SourceLineAnnotation.fromVisitedInstruction(this));
		} else if ((seen == PUTFIELD || seen == PUTSTATIC) && !selfAssignment) {
			XField f = XFactory.createReferencedXField(this);
			OpcodeStack.Item item = null;
			if (stack.getStackDepth() > 0) {
				item = stack.getStackItem(0);
				if (!item.isNull()) 
					nullTested.add(f);
			}
			writtenFields.add(f);
			
			boolean writtingNonNull = previousOpcode != ACONST_NULL || previousPreviousOpcode == GOTO;
			if (writtingNonNull) {
				writtenNonNullFields.add(f);
				if (DEBUG)
					System.out.println("put nn: " + f);
			} else if (DEBUG)
				System.out.println("put: " + f);
			if (writtingNonNull && readFields.contains(f))
				fieldAccess.remove(f);
			else if (!fieldAccess.containsKey(f))
				fieldAccess.put(f, SourceLineAnnotation.fromVisitedInstruction(this));
			
			boolean isConstructor = getMethodName().equals("<init>") || getMethodName().equals("<clinit>");
			if (getMethod().isStatic() == f.isStatic()
			        && (isConstructor || calledFromConstructors.contains(getMethodName() + ":" + getMethodSig())
			                || getMethodName().equals("init") || getMethodName().equals("init")
			                || getMethodName().equals("initialize") || getMethod().isPrivate())) {

				if (isConstructor) {
					writtenInConstructorFields.add(f);
					if (f.getSignature().equals("Ljava/lang/ThreadLocal;") && item != null && item.isNewlyAllocated())
						threadLocalAssignedInConstructor.put(f, new ProgramPoint(this));
				} else
					writtenInInitializationFields.add(f);
				if (writtingNonNull)
					assumedNonNull.remove(f);
			} else {
				writtenOutsideOfInitializationFields.add(f);
			}

		}
		previousPreviousOpcode = previousOpcode;
		previousOpcode = seen;
	}

	public boolean isReflexive(XField f) {
		return reflectiveFields.contains(f);
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
		Set<XField> declaredFields = new HashSet<XField>();
		AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
		XFactory xFactory = AnalysisContext.currentXFactory();
		for(XField f : AnalysisContext.currentXFactory().allFields()) {
			ClassDescriptor classDescriptor = f.getClassDescriptor();
			if (currentAnalysisContext.isApplicationClass(classDescriptor) 
					&& !currentAnalysisContext.isTooBig(classDescriptor) 
					&& !xFactory.isReflectiveClass(classDescriptor)  && !f.isProtected() && !f.isPublic())
				declaredFields.add(f);
		}
		// Don't report anything about ejb3Fields
		HashSet<XField> unknownAnotationAndUnwritten = new HashSet<XField>(unknownAnnotation.keySet());
		unknownAnotationAndUnwritten.removeAll(writtenFields);
		declaredFields.removeAll(unknownAnotationAndUnwritten);
		declaredFields.removeAll(containerFields);
		declaredFields.removeAll(reflectiveFields);
		for(Iterator<XField> i = declaredFields.iterator(); i.hasNext(); ) {
			XField f = i.next();
			if (f.isSynthetic() && !f.getName().startsWith("this$") || f.getName().startsWith("_"))
				i.remove();
		}

		TreeSet<XField> notInitializedInConstructors =
				new TreeSet<XField>(declaredFields);
		notInitializedInConstructors.retainAll(readFields);
		notInitializedInConstructors.retainAll(writtenFields);
		notInitializedInConstructors.retainAll(assumedNonNull.keySet());
		notInitializedInConstructors.removeAll(writtenInConstructorFields);
		notInitializedInConstructors.removeAll(writtenInInitializationFields);
		
		for(Iterator<XField> i = notInitializedInConstructors.iterator(); i.hasNext(); ) {
			if (i.next().isStatic())
				i.remove();
		}

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

		Map<String, Integer> count = new HashMap<String, Integer>();
		Bag<String> nullOnlyFieldNames = new Bag<String>();
		Bag<ClassDescriptor> classContainingNullOnlyFields = new Bag<ClassDescriptor>();
		
		for (XField f : nullOnlyFields) {
			nullOnlyFieldNames.add(f.getName());
			classContainingNullOnlyFields.add(f.getClassDescriptor());
			int increment = 3;
			Collection<ProgramPoint> assumedNonNullAt = assumedNonNull.get(f);
			if (assumedNonNullAt != null)
				increment += assumedNonNullAt.size();
			for(String s : unknownAnnotation.get(f)) {
				Integer value = count.get(s);
				if (value == null) 
					count.put(s,increment);
				else count.put(s,value+increment);
			}	
		}
		Map<XField, Integer> maxCount = new HashMap<XField, Integer>();
		
		
		LinkedList<XField> assumeReflective = new LinkedList<XField>();
		for (XField f : nullOnlyFields) {
			int myMaxCount = 0;
			for(String s : unknownAnnotation.get(f)) {
				Integer value = count.get(s);
				if (value != null && myMaxCount < value) myMaxCount = value;
			}
			if (myMaxCount > 0)
				maxCount.put(f, myMaxCount);
			if (myMaxCount > 15)
				assumeReflective.add(f);
			else if (nullOnlyFieldNames.getCount(f.getName()) > 8)
				assumeReflective.add(f);
			else if (classContainingNullOnlyFields.getCount(f.getClassDescriptor()) > 4)
				assumeReflective.add(f);
			else if (classContainingNullOnlyFields.getCount(f.getClassDescriptor()) > 2 && f.getName().length() == 1)
				assumeReflective.add(f);
				
		}
				
		readOnlyFields.removeAll(assumeReflective);
		nullOnlyFields.removeAll(assumeReflective);
		notInitializedInConstructors.removeAll(assumeReflective);
		
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
				if (maxCount.containsKey(f)) priority++;
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
			if (maxCount.containsKey(f)) priority++;
			if (abstractClasses.contains(f.getClassName())) {
				priority++;
				if (! hasNonAbstractSubClass.contains(f.getClassName())) priority++;
			}
			// if (fieldNamesSet.contains(f.getName())) priority++;
			if (assumedNonNull.containsKey(f)) {
				int npPriority = priority;
			
				Set<ProgramPoint> assumedNonNullAt = assumedNonNull.get(f);
				if (assumedNonNullAt.size() > 14) {
					npPriority+=2;
				} else if (assumedNonNullAt.size() > 6) {
					npPriority++;
				} else {
					priority--;
				}
				for (ProgramPoint p : assumedNonNullAt)
					bugAccumulator.accumulateBug(new BugInstance(this,
							"NP_UNWRITTEN_FIELD",
							npPriority)
							.addClassAndMethod(p.method)
							.addField(f), 
						p.getSourceLineAnnotation());
							
			} else {
				if (f.isStatic()) priority++;
				if (f.isFinal()) priority++;
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
			if (dontComplainAbout.matcher(fieldName).find()) 
				continue;
			if (lastDollar >= 0 && (fieldName.startsWith("this$") || fieldName.startsWith("this+"))) {
				String outerClassName = className.substring(0, lastDollar);

				try {
					JavaClass outerClass = Repository.lookupClass(outerClassName);
					if (classHasParameter(outerClass))
						continue;

					ClassDescriptor cDesc = DescriptorFactory.createClassDescriptorFromDottedClassName(outerClassName);

					XClass outerXClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, cDesc);
					XClass thisClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, f.getClassDescriptor());
					AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
				 	  
					Subtypes2 subtypes2 = analysisContext.getSubtypes2();
					
					for (XField of : outerXClass.getXFields())
						if (!of.isStatic()) {
							String sourceSignature = of.getSourceSignature();
							if (sourceSignature != null && of.getSignature().equals("Ljava/lang/ThreadLocal;")) {
								Type ofType = GenericUtilities.getType(sourceSignature);
								if (ofType instanceof GenericObjectType) {
									GenericObjectType gType = (GenericObjectType) ofType;

									for (ReferenceType r : gType.getParameters()) {
										if (r instanceof ObjectType) {
											ClassDescriptor c = DescriptorFactory.getClassDescriptor((ObjectType) r);
											if (subtypes2.isSubtype(f.getClassDescriptor(), c)) {
												ProgramPoint p = threadLocalAssignedInConstructor.get(of);
												int priority = p == null ? NORMAL_PRIORITY : HIGH_PRIORITY;
												BugInstance bug = new BugInstance(this, "SIC_THREADLOCAL_DEADLY_EMBRACE", priority)
												   .addClass(className).addField(of);
												if (p != null)
													bug.addMethod(p.method).add(p.getSourceLineAnnotation());
												bugReporter.reportBug(bug);
											}
										}
									}
								}
							}

						}

					boolean outerClassIsInnerClass = false;
					for (Field field : outerClass.getFields())
						if (field.getName().equals("this$0"))
							outerClassIsInnerClass = true;
					if (outerClassIsInnerClass)
						continue;
				} catch (ClassNotFoundException e) {
					bugReporter.reportMissingClass(e);
				} catch (CheckedAnalysisException e) {
					bugReporter.logError("Error getting outer XClass for " + outerClassName, e);
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
						
						String bug = "SIC_INNER_SHOULD_BE_STATIC";
						if (isAnonymousInnerClass)
							bug = "SIC_INNER_SHOULD_BE_STATIC_ANON";
						else if (!easyChange)
							bug = "SIC_INNER_SHOULD_BE_STATIC_NEEDS_THIS";

						
						bugReporter.reportBug(new BugInstance(this, bug, priority)
								.addClass(className));
						
					}
				}
			} else if (f.isResolved() ){
				if (constantFields.contains(f)) {
					if (!f.isStatic())
						bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this,
								"SS_SHOULD_BE_STATIC",
								NORMAL_PRIORITY), f));
				} else if (fieldsOfSerializableOrNativeClassed.contains(f)) {
					// ignore it
				} else if (!writtenFields.contains(f))
					bugReporter.reportBug(new BugInstance(this, "UUF_UNUSED_FIELD", NORMAL_PRIORITY)
							.addClass(className)
							.addField(f).lowerPriorityIfDeprecated());
				else if (f.getName().toLowerCase().indexOf("guardian") < 0) {
					int priority = NORMAL_PRIORITY;
					if (f.isStatic()) priority++;
					if (f.isFinal()) priority++;
					bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this, "URF_UNREAD_FIELD", priority),f));
				}
			}
		}
		bugAccumulator.reportAccumulatedBugs();
	}
	/**
	 * @param instance
	 * @return
	 */
	private BugInstance addClassFieldAndAccess(BugInstance instance, XField f) {
		if (writtenNonNullFields.contains(f) && readFields.contains(f)) 
			throw new IllegalArgumentException("No information for fields that are both read and written nonnull");

		instance.addClass(f.getClassName()).addField(f);
		if (fieldAccess.containsKey(f))
			instance.add(fieldAccess.get(f));
		return instance;
	}

}
