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
import java.util.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class UnreadFields extends BytecodeScanningDetector implements   Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("unreadfields.debug");

    Set<FieldAnnotation> declaredFields = new TreeSet<FieldAnnotation>();
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
    private BugReporter bugReporter;

    static final int doNotConsider = ACC_PUBLIC | ACC_PROTECTED
			| ACC_STATIC;

  public UnreadFields(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}


  public void visit(JavaClass obj)     {
	hasNativeMethods = false;
	if (superclassName.indexOf("$") >= 0) {
		// System.out.println("hicfsc: " + betterClassName);
		innerClassCannotBeStatic.add(betterClassName);
		// System.out.println("hicfsc: " + betterSuperclassName);
		innerClassCannotBeStatic.add(betterSuperclassName);
		}
	super.visit(obj);
	}
  public void visitAfter(JavaClass obj)     {
	if (!hasNativeMethods)
		declaredFields.addAll(myFields);
	myFields.clear();
	}


    public void visit(Field obj) {
        super.visit(obj);
	int flags = obj.getAccessFlags();
	if ((flags & doNotConsider) == 0 
			&& !fieldName.equals("serialVersionUID"))  {

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
	if (methodName.equals("<init>") && count_aload_1 > 1 
			&& className.indexOf('$') >= 0) {
		needsOuterObjectInConstructor.add(betterClassName);
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
		}
	else if (seen == GETFIELD) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (DEBUG) System.out.println("get: " + f);
		readFields.add(f);
		if (classConstant.equals(className) && 
			!myFields.contains(f)) {
			superReadFields.add(nameConstant);
			}
		}
	else if (seen == PUTFIELD) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (DEBUG) System.out.println("put: " + f);
		writtenFields.add(f);
		if (classConstant.equals(className) && 
			!myFields.contains(f)) {
			superWrittenFields.add(nameConstant);
			}
		}
	}

public void report() {

	declaredFields.removeAll(readFields);

	for(Iterator<FieldAnnotation> i = declaredFields.iterator(); i.hasNext(); )  {
		FieldAnnotation f = i.next();
		String fieldName = f.getFieldName();
		String className = f.getClassName();
		int lastDollar = className.lastIndexOf('$');
		boolean isAnonymousInnerClass =
			   (lastDollar > 0)
			&& (lastDollar < className.length() - 1)
			&& Character.isDigit(className.charAt(className.length() - 1));
		boolean allUpperCase = 
				fieldName.equals(fieldName.toUpperCase());
		if (superReadFields.contains(f.getFieldName()))  continue;
		if (!fieldName.startsWith("this$"))  {
		  if (constantFields.contains(f) )
		    bugReporter.reportBug(new BugInstance("SS_SHOULD_BE_STATIC", NORMAL_PRIORITY)
			.addClass(className)
			.addField(f));
		  else if (!writtenFields.contains(f) && !superWrittenFields.contains(f.getFieldName()))
		    bugReporter.reportBug(new BugInstance("UUF_UNUSED_FIELD", NORMAL_PRIORITY)
			.addClass(className)
			.addField(f));
		  else
		    bugReporter.reportBug(new BugInstance("URF_UNREAD_FIELD", NORMAL_PRIORITY)
			.addClass(className)
			.addField(f));
		   }
		  else if ( !innerClassCannotBeStatic.contains(className)) {
			boolean easyChange = !needsOuterObjectInConstructor.contains(className);
			if (easyChange || !isAnonymousInnerClass) {
	
			  int priority = LOW_PRIORITY;
			  if (easyChange && !isAnonymousInnerClass) 
				priority = NORMAL_PRIORITY;
 
			   bugReporter.reportBug(new BugInstance("SIC_INNER_SHOULD_BE_STATIC", 
					priority)
				.addClass(className));
			   }
			}
		}

	}	
}
