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
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import edu.umd.cs.pugh.visitclass.PreorderVisitor;
import edu.umd.cs.daveho.ba.ClassContext;

public class Naming extends PreorderVisitor implements Detector, Constants2 {
  String baseClassName;

  static class MyMethod {
	String superClass;
	String className;
	String methodName;
	String methodSig;
	MyMethod(String sc, String c, String n, String s) {
		superClass = sc;
		className = c;
		methodName = n;
		methodSig = s;
		}
	public boolean equals(Object o) {
		if (!(o instanceof MyMethod)) return false;
		MyMethod m2 = (MyMethod) o;
		return 
			className.equals(m2.className)
			&& methodName.equals(m2.methodName)
			&& methodSig.equals(m2.methodSig);
			}

	public int hashCode() {
		return className.hashCode()
			+methodName.hashCode()
			+methodSig.hashCode();
		}
	public boolean confusingMethodNames(MyMethod m) {
		return methodName.equalsIgnoreCase(m.methodName)
			& !methodName.equals(m.methodName);
		}
	public String toString() {
		return className + "." + methodName
				+ ":" + methodSig;
		}
	}

 
  // map of canonicalName -> trueMethodName 
  HashMap<String, HashSet<String>> canonicalToTrueMapping
	= new HashMap<String, HashSet<String>>();
  // map of canonicalName -> Set<MyMethod>
  HashMap<String, HashSet<MyMethod>> canonicalToMyMethod
	= new HashMap<String, HashSet<MyMethod>>();

  HashSet<String> reported = new HashSet<String>();
  HashSet<String> visited = new HashSet<String>();

  private BugReporter bugReporter;

  public Naming(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

  private boolean checkSuper(MyMethod m, HashSet<MyMethod> others) {
	for(Iterator<MyMethod> i = others.iterator(); i.hasNext() ; ) {
		MyMethod m2 =  i.next();
		if (m.confusingMethodNames(m2)
				&& m.superClass.equals(m2.className)) {
		  MyMethod m3 = new MyMethod(m.superClass,
					m.className, m2.methodName, m.methodSig);
		  if (others.contains(m3)) continue;
		  bugReporter.reportBug(new BugInstance("NM_VERY_CONFUSING", HIGH_PRIORITY)
			.addClass(m.className)
			.addMethod(m.className, m.methodName, m.methodSig)
			.addClass(m2.className)
			.addMethod(m2.className, m2.methodName, m2.methodSig));
			return true;
		}
	}
	return false;
	}

  private boolean checkNonSuper(MyMethod m, HashSet<MyMethod> others) {
	for(Iterator<MyMethod> i = others.iterator(); i.hasNext() ; ) {
		MyMethod m2 =  i.next();
		if (m.confusingMethodNames(m2)) {
		  bugReporter.reportBug(new BugInstance("NM_CONFUSING", LOW_PRIORITY)
			.addClass(m.className)
			.addMethod(m.className, m.methodName, m.methodSig)
			.addClass(m2.className)
			.addMethod(m2.className, m2.methodName, m2.methodSig));
			return true;
		}
	}
	return false;
	}





  public void report() {
	
	canonicalNameIterator: for(Iterator<Map.Entry<String, HashSet<String>>> i 
		= canonicalToTrueMapping.entrySet().iterator();
	    i.hasNext(); ) {
	   Map.Entry<String, HashSet<String>> e = i.next();
	   HashSet<String> s = e.getValue();
	   if (s.size() <= 1) continue;
	   String allSmall = e.getKey();
	   HashSet<MyMethod> conflictingMethods = canonicalToMyMethod.get(allSmall);
	   for(Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext(); ) {
		if (checkSuper(j.next(), conflictingMethods)) 
			j.remove();
		}
	   for(Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext(); ) {
		if (checkNonSuper(j.next(), conflictingMethods)) 
			continue canonicalNameIterator;
		}
	}
 }

  public void visit(JavaClass obj)     {
	String name = obj.getClassName();
	if (!visited.add(name)) return;
	String[] parts = name.split("[.]");
        baseClassName = parts[parts.length-1];
	// System.out.println("base name of " + name + " is " + baseClassName);
	super.visit(obj);
	try {
	JavaClass supers[] = Repository.getSuperClasses(obj);
	for(int i = 0; i < supers.length; i++)
		visit(supers[i]);
	} catch (ClassNotFoundException e) {
		// ignore it
		}
	}

    public void visit(Method obj) {
	if (methodName.length() == 1
			|| obj.isPrivate()
			|| obj.isStatic()
			) return;
	String trueName = methodName + methodSig;
	String allSmall = methodName.toLowerCase() + methodSig;
	
	MyMethod mm = new MyMethod(betterSuperclassName, 
					betterClassName, methodName, methodSig);
	{
	HashSet<String> s = canonicalToTrueMapping.get(allSmall);
	if (s == null) {
		s = new HashSet<String>();
		canonicalToTrueMapping.put(allSmall,s);
		}
	s.add(trueName);
	}
	{
	HashSet<MyMethod> s = canonicalToMyMethod.get(allSmall);
	if (s == null) {
		s = new HashSet<MyMethod>();
		canonicalToMyMethod.put(allSmall,s);
		}
	s.add(mm);
	}

	if (methodName.equals(baseClassName)) 
		bugReporter.reportBug(new BugInstance("NM_CONFUSING_METHOD_NAME", NORMAL_PRIORITY)
			.addClassAndMethod(this));
	if (methodName.equals("hashcode") && methodSig.equals("()I")) 
		bugReporter.reportBug(new BugInstance("NM_LCASE_HASHCODE", HIGH_PRIORITY)
			.addClassAndMethod(this));
	if (methodName.equals("tostring") && methodSig.equals("()Ljava/lang/String;")) 
		bugReporter.reportBug(new BugInstance("NM_LCASE_TOSTRING", HIGH_PRIORITY)
			.addClassAndMethod(this));
	}


/*	
	classes.put(methodName, betterClassName);
	String old = canonicalNames.put(allSmall, methodName);
	Set<String> all = allDefiningClasses.get(allSmall);
	if (all == null) {
		all = new HashSet<String>();
		allDefiningClasses.put(allSmall,all);
		}
	all.add(betterClassName);
	String oldSig = canonicalSigs.put(allSmall, methodSig);
	if (oldSig != null && !oldSig.equals(methodSig)) {
		System.out.println("Nm puzzle: " + allSmall 
			+"/" + methodSig
			+"/" + oldSig
			);
		}
	if (old != null && !old.equals(methodName) && !reported.contains(allSmall)) {
		reported.add(allSmall);
		String oldClass = classes.get(old);
		if (betterSuperclassName.equals(oldClass)) 
		else 
		  bugReporter.reportBug(new BugInstance( "NM_CONFUSING", LOW_PRIORITY)
			.addClass(betterClassName)
			.addMethod(betterClassName, methodName, methodSig)
			.addClass(classes.get(old))
			.addMethod(classes.get(old), old, oldSig));
		}
	// FIXME: I think that the "baseClassName" field is broken.
	// Need to look at the code that sets it.
	//System.out.println("methodName="+methodName+", baseClassName="+baseClassName);

*/

}
