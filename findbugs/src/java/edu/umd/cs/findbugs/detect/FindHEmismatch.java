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
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindHEmismatch extends BytecodeScanningDetector implements   Constants2 {
   boolean hasFields = false;
   boolean hasHashCode = false;
   boolean hasEqualsObject = false;
   boolean equalsMethodIsInstanceOfEquals = false;
   boolean hasCompareToObject = false;
   boolean hasEqualsSelf = false;
   boolean hasCompareToSelf = false;
   boolean extendsObject = false;
   private BugReporter bugReporter;

   public FindHEmismatch(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

   public void visitAfter(JavaClass obj) {
	if (!obj.isClass()) return;
	if (getDottedClassName().equals("java.lang.Object")) return;
	int accessFlags = obj.getAccessFlags();
	if ((accessFlags & ACC_INTERFACE) != 0) return;
	String whereEqual = getDottedClassName();
	if (!hasEqualsObject)  {
		JavaClass we = Lookup.findSuperImplementor(obj, "equals",
					"(Ljava/lang/Object;)Z", bugReporter);
		if (we == null) whereEqual = "java.lang.Object";
		else whereEqual = we.getClassName();
		}
	boolean usesDefaultEquals = whereEqual.equals("java.lang.Object");
	String whereHashCode = getDottedClassName();
	if (!hasHashCode) {
		JavaClass wh = Lookup.findSuperImplementor(obj, "hashCode",
					"()I", bugReporter);
		if (wh == null) whereHashCode = "java.lang.Object";
		else whereHashCode = wh.getClassName();
		}
	boolean usesDefaultHashCode = whereHashCode.equals("java.lang.Object");
	if (false && (usesDefaultEquals ||  usesDefaultHashCode)) {
		try {
		  if (Repository.implementationOf(obj, "java/util/Set")
		    || Repository.implementationOf(obj, "java/util/List")
		    || Repository.implementationOf(obj, "java/util/Map")) {
		  // System.out.println(getDottedClassName() + " uses default hashCode or equals");
		  }
                } catch (ClassNotFoundException e) {
			// e.printStackTrace();
                }
	}

	if (!hasEqualsObject &&  hasEqualsSelf) {
		
		if (usesDefaultEquals) 
		  bugReporter.reportBug(new BugInstance("EQ_SELF_USE_OBJECT", 
				(usesDefaultHashCode || obj.isAbstract()) 
				? NORMAL_PRIORITY : HIGH_PRIORITY).addClass(getDottedClassName()));
		else {
		  int priority = hasFields ? NORMAL_PRIORITY : LOW_PRIORITY;
		  if (obj.isAbstract()) priority++;
		  if (priority <= LOW_PRIORITY)
		  bugReporter.reportBug(new BugInstance("EQ_SELF_NO_OBJECT", NORMAL_PRIORITY).addClass(getDottedClassName()));
		}
		}
	/*
	System.out.println("Class " + betterClassName);
	System.out.println("usesDefaultEquals: " + usesDefaultEquals);
	System.out.println("hasHashCode: : " + hasHashCode);
	System.out.println("usesDefaultHashCode: " + usesDefaultHashCode);
	System.out.println("hasEquals: : " + hasEqualsObject);
	*/

	if (!hasCompareToObject &&  hasCompareToSelf) {
		if (!extendsObject)
		  bugReporter.reportBug(new BugInstance("CO_SELF_NO_OBJECT", NORMAL_PRIORITY).addClass(getDottedClassName()));
		}

	// if (!hasFields) return;
	if (hasHashCode && !(hasEqualsObject ||  hasEqualsSelf))  { 
		/*
		System.out.println("has hashCode, missing equals");
		System.out.println("equals defined in " + whereEqual);
		System.out.println("extendsObject: " + extendsObject);
		*/
		if (usesDefaultEquals) 
		  bugReporter.reportBug(new BugInstance("HE_HASHCODE_USE_OBJECT_EQUALS", LOW_PRIORITY).addClass(getDottedClassName()));
		else
		  bugReporter.reportBug(new BugInstance("HE_HASHCODE_NO_EQUALS", LOW_PRIORITY). addClass(getDottedClassName()));
		}
	if (!hasHashCode && (hasEqualsObject ||  hasEqualsSelf))  {
		if (usesDefaultHashCode) {
		  int priority = HIGH_PRIORITY;
		  if (equalsMethodIsInstanceOfEquals) priority+= 2;
		  else if (obj.isAbstract() || !hasEqualsObject) priority++;
		  bugReporter.reportBug(new BugInstance("HE_EQUALS_USE_HASHCODE", 
				priority ).addClass(getDottedClassName()));
		  }
		else {
		  int priority = NORMAL_PRIORITY;
		  if (hasFields) priority++;
		  if (obj.isAbstract()) priority++;
		  if (equalsMethodIsInstanceOfEquals) priority++;
		  if (priority <= LOW_PRIORITY)
		   bugReporter.reportBug(
		    new BugInstance("HE_EQUALS_NO_HASHCODE", 
			priority)
		    .addClass(getDottedClassName()));
		}
		}
	}
   public void visit(JavaClass obj) {
	extendsObject = getDottedSuperclassName().equals("java.lang.Object");
	hasFields = false;
	hasHashCode = false;
	hasCompareToObject = false;
	hasCompareToSelf = false;
	hasEqualsObject = false;
	hasEqualsSelf = false;
        equalsMethodIsInstanceOfEquals = false;
	}

    public void visit(Field obj) {
	int accessFlags = obj.getAccessFlags();
	if ((accessFlags & ACC_STATIC) != 0) return;
	hasFields = true;
	}
    public void visit(Method obj) {
	int accessFlags = obj.getAccessFlags();
	if ((accessFlags & ACC_STATIC) != 0) return;
	String name = obj.getName();
	String sig = obj.getSignature();
	if ((accessFlags & ACC_ABSTRACT) != 0) {
		if (name.equals("equals")
			&& sig.equals("(L"+getClassName()+";)Z"))
		  bugReporter.reportBug(new BugInstance("EQ_ABSTRACT_SELF", LOW_PRIORITY).addClass(getDottedClassName()));
		else if (name.equals("compareTo")
			&& sig.equals("(L"+getClassName()+";)I"))
		  bugReporter.reportBug(new BugInstance("CO_ABSTRACT_SELF", LOW_PRIORITY).addClass(getDottedClassName()));
		return;
		}
	boolean sigIsObject = sig.equals("(Ljava/lang/Object;)Z");
	if (name.equals("hashCode")
		&& sig.equals("()I")) {
		hasHashCode = true;
		// System.out.println("Found hashCode for " + betterClassName);
		}
	else if (name.equals("equals")) {
		if (sigIsObject) {
			hasEqualsObject = true; 
			Code code = obj.getCode();
			byte [] codeBytes = code.getCode();
			if (codeBytes.length == 5 &&
				(codeBytes[1] & 0xff) == INSTANCEOF) {
			equalsMethodIsInstanceOfEquals = true;
				return;
				}
			}
		else if (sig.equals("(L"+getClassName()+";)Z"))
				hasEqualsSelf = true;
		}
	else if (name.equals("compareTo")) {
		if (sig.equals("(Ljava/lang/Object;)I")) 
			hasCompareToObject = true;
		else if (sig.equals("(L"+getClassName()+";)I"))
				hasCompareToSelf = true;
		}
	}
}
