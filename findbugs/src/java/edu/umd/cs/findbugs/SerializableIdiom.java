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

package edu.umd.cs.findbugs;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.Constants2;

public class SerializableIdiom extends BetterVisitor implements Detector, Constants2 {


    boolean sawSerialVersionUID;
    boolean isSerializable;
    int synchronizedMethods;
    boolean writeObjectIsSynchronized;
    private BugReporter bugReporter;

    public SerializableIdiom(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

    public void report() {
		}
    public void visitJavaClass(JavaClass obj)     {      
	int flags = obj.getAccessFlags();
	boolean isAbstract = (flags & ACC_ABSTRACT) != 0 
			   || (flags & ACC_INTERFACE) != 0;
	super.visitJavaClass(obj);
        sawSerialVersionUID = false;
        constant_pool.accept(this);
               isSerializable = false;
               String [] interface_names = obj.getInterfaceNames();
               for(int i=0; i < interface_names.length; i++) {
                       if (interface_names[i].equals("java.io.Serializable")) {
                               isSerializable = true;
                       }
               }

	isSerializable = isSerializable
		|| Repository.instanceOf(obj,"java.io.Serializable");
        Field[] fields = obj.getFields();
        for(int i = 0; i < fields.length; i++) fields[i].accept(this);
	if (isSerializable && !isAbstract && !sawSerialVersionUID)
		bugReporter.reportBug(new BugInstance("SE_NO_SERIALVERSIONID", NORMAL_PRIORITY).addClass(this));

	synchronizedMethods = 0;
	writeObjectIsSynchronized = false;
        Method[] methods = obj.getMethods();
        for(int i = 0; i < methods.length; i++) methods[i].accept(this);
	if (writeObjectIsSynchronized && synchronizedMethods == 0)
		bugReporter.reportBug(new BugInstance("WS_WRITEOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
        }

    public void visit(Method obj) {
	int accessFlags = obj.getAccessFlags();
        boolean isSynchronized = (accessFlags & ACC_SYNCHRONIZED) != 0;
	// System.out.println(methodName + isSynchronized);
	if (!isSynchronized) return;
	if (methodName.equals("readObject")) 
		bugReporter.reportBug(new BugInstance("RS_READOBJECT_SYNC", NORMAL_PRIORITY).addClass(this));
	else if (methodName.equals("writeObject")) 
		writeObjectIsSynchronized = true;
	else synchronizedMethods++;

	}
		
	
    public void visit(Field obj) {
	// System.out.println("Saw " + betterClassName + "." + fieldName);
        super.visit(obj);

	if (!fieldName.equals("serialVersionUID")) return;
	// System.out.println("Saw " + betterClassName + "." + fieldName);
	int flags = obj.getAccessFlags();
	if ((flags & ACC_STATIC) == 0) {
		bugReporter.reportBug(new BugInstance("SE_NONSTATIC_SERIALVERSIONID", NORMAL_PRIORITY)
			.addClass(this)
			.addVisitedField(this));
		return;
		}
	sawSerialVersionUID = true;
	}


}
