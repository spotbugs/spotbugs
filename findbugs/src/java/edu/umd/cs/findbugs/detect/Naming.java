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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Field;

public class Naming extends PreorderVisitor implements Detector, Constants2 {
	String baseClassName;
	boolean classIsPublicOrProtected;

	static class MyMethod {
		final JavaClass clazz;
		final String methodName;
		final String methodSig;

		MyMethod(JavaClass c, String n, String s) {
			clazz = c;
			methodName = n;
			methodSig = s;
		}

		public String getClassName() {
			return clazz.getClassName();
		}

		public boolean equals(Object o) {
			if (!(o instanceof MyMethod)) return false;
			MyMethod m2 = (MyMethod) o;
			return
			        clazz.equals(m2.clazz)
			        && methodName.equals(m2.methodName)
			        && methodSig.equals(m2.methodSig);
		}

		public int hashCode() {
			return clazz.hashCode()
			        + methodName.hashCode()
			        + methodSig.hashCode();
		}

		public boolean confusingMethodNames(MyMethod m) {
			return methodName.equalsIgnoreCase(m.methodName)
			        && !methodName.equals(m.methodName);
		}

		public String toString() {
			return getClassName()
			        + "." + methodName
			        + ":" + methodSig;
		}
	}


	// map of canonicalName -> trueMethodName
	HashMap<String, HashSet<String>> canonicalToTrueMapping
	        = new HashMap<String, HashSet<String>>();
	// map of canonicalName -> Set<MyMethod>
	HashMap<String, HashSet<MyMethod>> canonicalToMyMethod
	        = new HashMap<String, HashSet<MyMethod>>();

	HashSet<String> visited = new HashSet<String>();

	private BugReporter bugReporter;
	//private AnalysisContext analysisContext;

	public Naming(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
		//this.analysisContext = analysisContext;
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	private boolean checkSuper(MyMethod m, HashSet<MyMethod> others) {
		for (Iterator<MyMethod> i = others.iterator(); i.hasNext();) {
			MyMethod m2 = i.next();
			try {
				if (m.confusingMethodNames(m2)
				        && Repository.instanceOf(m.clazz, m2.clazz)) {
					MyMethod m3 = new MyMethod(m.clazz, m2.methodName, m.methodSig);
					boolean r = others.contains(m3);
					if (r) continue;
					bugReporter.reportBug(new BugInstance(this, "NM_VERY_CONFUSING", HIGH_PRIORITY)
					        .addClass(m.getClassName())
					        .addMethod(m.getClassName(), m.methodName, m.methodSig)
					        .addClass(m2.getClassName())
					        .addMethod(m2.getClassName(), m2.methodName, m2.methodSig));
					return true;
				}
			} catch (ClassNotFoundException e) {
			}
		}
		return false;
	}

	private boolean checkNonSuper(MyMethod m, HashSet<MyMethod> others) {
		for (Iterator<MyMethod> i = others.iterator(); i.hasNext();) {
			MyMethod m2 = i.next();
			if (m.confusingMethodNames(m2)) {
				bugReporter.reportBug(new BugInstance(this, "NM_CONFUSING", LOW_PRIORITY)
				        .addClass(m.getClassName())
				        .addMethod(m.getClassName(), m.methodName, m.methodSig)
				        .addClass(m2.getClassName()) .addMethod(m2.getClassName(), m2.methodName, m2.methodSig));
				return true;
			}
		}
		return false;
	}


	public void report() {

	canonicalNameIterator:
		for (Iterator<String> i = canonicalToTrueMapping.keySet().iterator(); i.hasNext(); ) {
			String allSmall = i.next();
			HashSet<String> s = canonicalToTrueMapping.get(allSmall);
			if (s.size() <= 1)
				continue;
			HashSet<MyMethod> conflictingMethods = canonicalToMyMethod.get(allSmall);
			for (Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext();) {
				if (checkSuper(j.next(), conflictingMethods))
					j.remove();
			}
			for (Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext();) {
				if (checkNonSuper(j.next(), conflictingMethods))
					continue canonicalNameIterator;
			}
		}
	}

	public void visitJavaClass(JavaClass obj) {
		if (obj.isInterface()) return;
		String name = obj.getClassName();
		if (!visited.add(name)) return;
		try {
			JavaClass supers[] = Repository.getSuperClasses(obj);
			for (int i = 0; i < supers.length; i++) {
				visitJavaClass(supers[i]);
			}
		} catch (ClassNotFoundException e) {
			// ignore it
		}
		super.visitJavaClass(obj);
	}

	public void visit(JavaClass obj) {
		String name = obj.getClassName();
		String[] parts = name.split("[$+.]");
		baseClassName = parts[parts.length - 1];
		classIsPublicOrProtected = obj.isPublic() || obj.isProtected();
		if (baseClassName.length() == 1) return;
		if(Character.isLetter(baseClassName.charAt(0))
		   && !Character.isUpperCase(baseClassName.charAt(0))
		   && baseClassName.indexOf("_") ==-1 
			)
			bugReporter.reportBug(new BugInstance(this, 
				"NM_CLASS_NAMING_CONVENTION", 
				classIsPublicOrProtected 
				? NORMAL_PRIORITY
				: LOW_PRIORITY
					)
			        .addClass(this));
		super.visit(obj);
	}

	public void visit(Field obj) {
		if (getFieldName().length() == 1) return;

		if (!obj.isFinal() 
			&& Character.isLetter(getFieldName().charAt(0))
			&& !Character.isLowerCase(getFieldName().charAt(0))
			&& getFieldName().indexOf("_") == -1
			&& Character.isLetter(getFieldName().charAt(1))
			&& Character.isLowerCase(getFieldName().charAt(1)))
			bugReporter.reportBug(new BugInstance(this, 
				"NM_FIELD_NAMING_CONVENTION", 
				classIsPublicOrProtected 
				 && (obj.isPublic() || obj.isProtected())  
				? NORMAL_PRIORITY
				: LOW_PRIORITY)
			        .addClass(this)
			        .addVisitedField(this)
				);
		}
	public void visit(Method obj) {
		if (getMethodName().length() == 1) return;

		if (Character.isLetter(getMethodName().charAt(0))
			&& !Character.isLowerCase(getMethodName().charAt(0))
			&& Character.isLetter(getMethodName().charAt(1))
			&& Character.isLowerCase(getMethodName().charAt(1))
			&& getMethodName().indexOf("_") == -1 )
			bugReporter.reportBug(new BugInstance(this, 
				"NM_METHOD_NAMING_CONVENTION", 
				classIsPublicOrProtected 
				 && (obj.isPublic() || obj.isProtected())  
				? NORMAL_PRIORITY
				: LOW_PRIORITY)
			        .addClassAndMethod(this));
		if (getMethodName().equals(baseClassName)) {
			bugReporter.reportBug(new BugInstance(this, "NM_CONFUSING_METHOD_NAME",
			        (getMethodSig().equals("()V") 
					&& obj.getCode().getCode().length > 1
					&& !obj.isNative()
					)
			        ? HIGH_PRIORITY : NORMAL_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}

		if (obj.isAbstract()) return;
		if (obj.isPrivate()) return;

		if (getMethodName().equals("equal") && getMethodSig().equals("(Ljava/lang/Object;)Z")) {
			bugReporter.reportBug(new BugInstance(this, "NM_BAD_EQUAL", HIGH_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}
		if (getMethodName().equals("hashcode") && getMethodSig().equals("()I")) {
			bugReporter.reportBug(new BugInstance(this, "NM_LCASE_HASHCODE", HIGH_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}
		if (getMethodName().equals("tostring") && getMethodSig().equals("()Ljava/lang/String;")) {
			bugReporter.reportBug(new BugInstance(this, "NM_LCASE_TOSTRING", HIGH_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}


		if (obj.isPrivate()
		        || obj.isStatic()
		)
			return;

		String trueName = getMethodName() + getMethodSig();
		String allSmall = getMethodName().toLowerCase() + getMethodSig();

		MyMethod mm = new MyMethod(getThisClass(), getMethodName(), getMethodSig());
		{
			HashSet<String> s = canonicalToTrueMapping.get(allSmall);
			if (s == null) {
				s = new HashSet<String>();
				canonicalToTrueMapping.put(allSmall, s);
			}
			s.add(trueName);
		}
		{
			HashSet<MyMethod> s = canonicalToMyMethod.get(allSmall);
			if (s == null) {
				s = new HashSet<MyMethod>();
				canonicalToMyMethod.put(allSmall, s);
			}
			s.add(mm);
		}

	}


}
