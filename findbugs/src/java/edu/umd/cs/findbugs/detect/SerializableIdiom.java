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
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.visitclass.Constants2;
import java.util.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;

public class SerializableIdiom extends PreorderVisitor 
	implements Detector, Constants2 {


    boolean sawSerialVersionUID;
    boolean isSerializable, implementsSerializableDirectly;
    boolean isExternalizable;
    boolean isGUIClass;
    boolean foundSynthetic;
    boolean foundSynchronizedMethods;
    boolean writeObjectIsSynchronized;
    private BugReporter bugReporter;
    private AnalysisContext analysisContext;
    boolean isAbstract;
    private List<BugInstance> fieldWarningList = new LinkedList<BugInstance>();
    private boolean sawReadExternal;
    private boolean sawWriteExternal;
    private boolean sawReadObject;
    private boolean sawWriteObject;
    private boolean superClassImplementsSerializable;
    private boolean hasPublicVoidConstructor;
    private boolean superClassHasVoidConstructor;
    private boolean directlyImplementsExternalizable;
    //private boolean isRemote;

    public SerializableIdiom(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void setAnalysisContext(AnalysisContext analysisContext) {
	this.analysisContext = analysisContext;
	}

     public void visitClassContext(ClassContext classContext) {
       classContext.getJavaClass().accept(this);
       flush();
       }

    private void flush() {
	if (!isAbstract &&
	    !((sawReadExternal && sawWriteExternal) || (sawReadObject && sawWriteObject))) {
		Iterator<BugInstance> i = fieldWarningList.iterator();
		while (i.hasNext())
			bugReporter.reportBug(i.next());
	}
	fieldWarningList.clear();
    }
 
    public void report() { }

    public void visit(JavaClass obj)     {      
	int flags = obj.getAccessFlags();
	isAbstract = (flags & ACC_ABSTRACT) != 0 
			   || (flags & ACC_INTERFACE) != 0;
        sawSerialVersionUID = false;
        isSerializable = implementsSerializableDirectly = false;
	isExternalizable = false;
	directlyImplementsExternalizable = false;
	isGUIClass = false;
	//isRemote = false;

        // Does this class directly implement Serializable?
        String [] interface_names = obj.getInterfaceNames();
        for(int i=0; i < interface_names.length; i++) {
            if (interface_names[i].equals("java.io.Externalizable")) {
		directlyImplementsExternalizable = true;
                isExternalizable = true;
		// System.out.println("Directly implements Externalizable: " + betterClassName);
		}
            else if (interface_names[i].equals("java.io.Serializable")) {
		implementsSerializableDirectly = true;
                isSerializable = true;
		break;
                }
            }

        // Does this class indirectly implement Serializable?
	if (!isSerializable) {
	    try {
	        if (Repository.instanceOf(obj,"java.io.Externalizable"))
		    isExternalizable = true;
	        if (Repository.instanceOf(obj,"java.io.Serializable"))
		    isSerializable = true;
/*
	        if (Repository.instanceOf(obj,"java.rmi.Remote")) {
		    isRemote = true;
		    }
*/
	    } catch (ClassNotFoundException e) {
		bugReporter.reportMissingClass(e);
	    }
	}

        hasPublicVoidConstructor = false;
        superClassHasVoidConstructor = true;
        superClassImplementsSerializable = isSerializable && !implementsSerializableDirectly;
	try {
	    JavaClass superClass = obj.getSuperClass();
	    if (superClass != null) {
		Method [] superClassMethods = superClass.getMethods();
		superClassImplementsSerializable = Repository.instanceOf(superClass,
					"java.io.Serializable");
	        superClassHasVoidConstructor = false;
		for(int i = 0; i < superClassMethods.length; i++) {
			Method m = superClassMethods[i];
			/*
			if (!m.isPrivate())
			System.out.println("Supercase of " + className 
				+ " has an accessible method named " + m.getName()
				+ " with sig " + m.getSignature());
			*/
			if (m.getName().equals("<init>")
				 && m.getSignature().equals("()V")
				 && !m.isPrivate()
				) {
			  // System.out.println("  super has void constructor");
			  superClassHasVoidConstructor = true;
			  }
			}
		}
	} catch (ClassNotFoundException e) {
	    bugReporter.reportMissingClass(e);
	}


	// Is this a GUI class?
	try {
	    isGUIClass = Repository.instanceOf(obj, "java.awt.Component");
	} catch (ClassNotFoundException e) {
	    bugReporter.reportMissingClass(e);
	}

	foundSynthetic = false;
	foundSynchronizedMethods = false;
	writeObjectIsSynchronized = false;

	sawReadExternal = sawWriteExternal = sawReadObject = sawWriteObject = false;
	}

	public void visitAfter(JavaClass obj) {
	if (false) {
	System.out.println(getDottedClassName());
	System.out.println("  hasPublicVoidConstructor: " + hasPublicVoidConstructor);
	System.out.println("  superClassHasVoidConstructor: " + superClassHasVoidConstructor);
	System.out.println("  isExternalizable: " + isExternalizable);
	System.out.println("  isSerializable: " + isSerializable);
	System.out.println("  isAbstract: " + isAbstract);
	System.out.println("  superClassImplementsSerializable: " + superClassImplementsSerializable);
	}
	if (isSerializable && !isExternalizable
		&& !superClassHasVoidConstructor 
		&& !superClassImplementsSerializable)
		bugReporter.reportBug(new BugInstance("SE_NO_SUITABLE_CONSTRUCTOR", 
			( implementsSerializableDirectly || sawSerialVersionUID)  
					? HIGH_PRIORITY : NORMAL_PRIORITY)
			.addClass(getThisClass().getClassName()));
	// Downgrade class-level warnings if it's a GUI class.
	int priority = isGUIClass ? LOW_PRIORITY : NORMAL_PRIORITY;
	if (obj.getClassName().endsWith("_Stub")) priority++;

	if (isExternalizable && !hasPublicVoidConstructor && !isAbstract)
		bugReporter.reportBug(new BugInstance("SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION", 
					directlyImplementsExternalizable ?
					HIGH_PRIORITY : NORMAL_PRIORITY)
			.addClass(getThisClass().getClassName()));
	if (foundSynthetic  && !isExternalizable && !isGUIClass
		&& isSerializable && !isAbstract && !sawSerialVersionUID)
		bugReporter.reportBug(new BugInstance("SE_NO_SERIALVERSIONID", priority).addClass(this));

	if (writeObjectIsSynchronized && !foundSynchronizedMethods)
		bugReporter.reportBug(new BugInstance("WS_WRITEOBJECT_SYNC", LOW_PRIORITY).addClass(this));
        }

    public void visit(Method obj) {
	int accessFlags = obj.getAccessFlags();
        boolean isSynchronized = (accessFlags & ACC_SYNCHRONIZED) != 0;
	if (getMethodName().equals("<init>") && getMethodSig().equals("()V")
			&& (accessFlags & ACC_PUBLIC) != 0
			) 
		hasPublicVoidConstructor = true;
	if (!getMethodName().equals("<init>")
		&& isSynthetic(obj)) foundSynthetic = true;
	// System.out.println(methodName + isSynchronized);

	if (getMethodName().equals("readExternal"))
		sawReadExternal = true;
	else if (getMethodName().equals("writeExternal"))
		sawWriteExternal = true;
	else if (getMethodName().equals("readObject"))
		sawReadObject = true;
	else if (getMethodName().equals("writeObject"))
		sawWriteObject = true;

	if (!isSynchronized) return;
	if (getMethodName().equals("readObject") &&
		getMethodSig().equals("(Ljava/io/ObjectInputStream;)V") &&
		isSerializable) 
		bugReporter.reportBug(new BugInstance("RS_READOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
	else if (getMethodName().equals("writeObject"))
		writeObjectIsSynchronized = true;
	else foundSynchronizedMethods = true;

	}
     boolean isSynthetic(FieldOrMethod obj) {
	Attribute [] a = obj.getAttributes();
	for(int i = 0; i < a.length; i++)
		if (a[i] instanceof Synthetic) return true;
	return false;
	}

		
	
    public void visit(Field obj) {
	int flags = obj.getAccessFlags();

	if (getClassName().indexOf("ObjectStreamClass") == -1
	    && isSerializable 
		&& !isExternalizable
		&& getFieldSig().indexOf("L")  >= 0 && !obj.isTransient() && !obj.isStatic()) {
		try {
			String fieldTypeClassName = getFieldSig().substring(getFieldSig().indexOf("L")+1, getFieldSig().length() - 1).replace('/', '.');
			JavaClass fieldTypeClass = Repository.lookupClass(fieldTypeClassName);

			if (!fieldTypeClassName.equals("java.lang.Object") &&
			    !(Repository.instanceOf(fieldTypeClass, "java.io.Serializable")
				|| Repository.instanceOf(fieldTypeClass, "java.io.Externalizable"))) {
				

				// Priority is LOW for GUI classes (unless explicitly marked Serializable),
				// HIGH if the class directly implements Serializable,
				// NORMAL otherwise.
				int priority = NORMAL_PRIORITY;
				if (implementsSerializableDirectly || sawSerialVersionUID)
					priority--;
				if (isGUIClass)
				    priority++;

				// Lower the priority for fields which are of an interface
				// or abstract type, since the user may know that all subtypes of
				// the interface will be Serializable.
				if (fieldTypeClass.isInterface() 
					|| fieldTypeClass.isAbstract()) {
				    priority = Math.max(LOW_PRIORITY, priority + 1);
			            if (Repository.instanceOf(fieldTypeClass, 
						"java.util.Collection"))
					    return;
				    }
				// Report is queued until after the entire class has been seen.
				fieldWarningList.add(new BugInstance("SE_BAD_FIELD", priority)
					.addClass(getThisClass().getClassName())
					.addField(getDottedClassName(), obj.getName(), getFieldSig(), false));
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
		}
	}

	if (!getFieldName().startsWith("this")
		&& isSynthetic(obj)) foundSynthetic = true;
	if (!getFieldName().equals("serialVersionUID")) return;
	int mask = ACC_STATIC | ACC_FINAL;
	if ( !getFieldSig().equals("I")
			 && !getFieldSig().equals("J")) return;
	if ((flags & mask) == mask 
		&& getFieldSig().equals("I")) {
		bugReporter.reportBug(new BugInstance("SE_NONLONG_SERIALVERSIONID", LOW_PRIORITY)
			.addClass(this)
			.addVisitedField(this));
		sawSerialVersionUID = true;
		return;
		}
	else if ((flags & ACC_STATIC) == 0) {
		bugReporter.reportBug(new BugInstance("SE_NONSTATIC_SERIALVERSIONID", NORMAL_PRIORITY)
			.addClass(this)
			.addVisitedField(this));
		return;
		}
	else if ((flags & ACC_FINAL) == 0) {
		bugReporter.reportBug(new BugInstance("SE_NONFINAL_SERIALVERSIONID", NORMAL_PRIORITY)
			.addClass(this)
			.addVisitedField(this));
		return;
		}
	sawSerialVersionUID = true;
	}


}
