/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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
import edu.umd.cs.pugh.visitclass.PreorderVisitor;
import edu.umd.cs.pugh.visitclass.Constants2;
import java.util.*;

public class SerializableIdiom extends PreorderVisitor 
	implements Detector, Constants2 {


    boolean sawSerialVersionUID;
    boolean isSerializable, implementsSerializableDirectly;
    boolean foundSynthetic;
    boolean foundSynchronizedMethods;
    boolean writeObjectIsSynchronized;
    private BugReporter bugReporter;
    boolean isAbstract;
    private List<BugInstance> fieldWarningList = new LinkedList<BugInstance>();
    private boolean sawReadExternal;
    private boolean sawWriteExternal;
    private boolean sawReadObject;
    private boolean sawWriteObject;

    public SerializableIdiom(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}
     public void visitClassContext(ClassContext classContext) {
       classContext.getJavaClass().accept(this);
       flush();
       }

    private void flush() {
	if (!isAbstract &&
	    !((sawReadExternal && sawWriteExternal) || (sawReadObject || sawWriteObject))) {
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

        // Does this class directly implement Serializable?
        String [] interface_names = obj.getInterfaceNames();
        for(int i=0; i < interface_names.length; i++) {
            if (interface_names[i].equals("java.io.Serializable")) {
		implementsSerializableDirectly = true;
                isSerializable = true;
		break;
                }
            }

        // Does this class indirectly implement Serializable?
	if (!isSerializable) {
	    try {
	        if (Repository.instanceOf(obj,"java.io.Serializable"))
		    isSerializable = true;
	        if (Repository.instanceOf(obj,"java.rmi.Remote"))
		    isSerializable = false;
	    } catch (ClassNotFoundException e) {
		bugReporter.reportMissingClass(e);
	    }
	}
	foundSynthetic = false;
	foundSynchronizedMethods = false;
	writeObjectIsSynchronized = false;

	sawReadExternal = sawWriteExternal = sawReadObject = sawWriteObject = false;
	}

	public void visitAfter(JavaClass obj) {
	if (foundSynthetic 
		&& isSerializable && !isAbstract && !sawSerialVersionUID)
		bugReporter.reportBug(new BugInstance("SE_NO_SERIALVERSIONID", NORMAL_PRIORITY).addClass(this));

	if (writeObjectIsSynchronized && !foundSynchronizedMethods)
		bugReporter.reportBug(new BugInstance("WS_WRITEOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
        }

    public void visit(Method obj) {
	int accessFlags = obj.getAccessFlags();
        boolean isSynchronized = (accessFlags & ACC_SYNCHRONIZED) != 0;
	if (!methodName.equals("<init>") 
		&& isSynthetic(obj)) foundSynthetic = true;
	// System.out.println(methodName + isSynchronized);

	if (methodName.equals("readExternal"))
		sawReadExternal = true;
	else if (methodName.equals("writeExternal"))
		sawWriteExternal = true;
	else if (methodName.equals("readObject"))
		sawReadObject = true;
	else if (methodName.equals("writeObject"))
		sawWriteObject = true;

	if (!isSynchronized) return;
	if (methodName.equals("readObject")) 
		bugReporter.reportBug(new BugInstance("RS_READOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
	else if (methodName.equals("writeObject")) 
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

	if (isSerializable && fieldSig.startsWith("L") && !obj.isTransient() && !obj.isStatic()) {
		try {
			String fieldClassName = fieldSig.substring(1, fieldSig.length() - 1).replace('/', '.');
			if (!fieldClassName.equals("java.lang.Object") &&
			    !Repository.instanceOf(fieldClassName, "java.io.Serializable"))
				fieldWarningList.add(new BugInstance("SE_BAD_FIELD",
					implementsSerializableDirectly ? HIGH_PRIORITY : NORMAL_PRIORITY)
					.addClass(thisClass.getClassName())
					.addField(fieldClassName, obj.getName(), fieldSig, false)
					.addUnknownSourceLine(thisClass.getClassName(), thisClass.getSourceFileName()));
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
		}
	}

	if (!fieldName.startsWith("this") 
		&& isSynthetic(obj)) foundSynthetic = true;
	if (!fieldName.equals("serialVersionUID")) return;
	int mask = ACC_STATIC | ACC_FINAL;
	if ( !fieldSig.equals("I")
			 && !fieldSig.equals("J")) return;
	if ((flags & mask) == mask 
		&& fieldSig.equals("I")) {
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
