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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class Naming extends PreorderVisitor implements Detector {
	String baseClassName;
	boolean classIsPublicOrProtected;

	static class MyMethod {
		final String className;
		final String methodName;
		final String methodSig;
		final boolean isStatic;

		MyMethod(String cName, String n, String s, boolean isStatic) {
			className = cName;
			methodName = n;
			methodSig = s;
			this.isStatic = isStatic;
		}

		public String getClassName() {
			return className;
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
			        + methodName.hashCode()
			        + methodSig.hashCode();
		}

		public boolean confusingMethodNames(MyMethod m) {
			if (className.equals(m.className)) return false;
			if (methodName.equalsIgnoreCase(m.methodName)
			        && !methodName.equals(m.methodName)) return true;
			if (methodSig.equals(m.methodSig)) return false;
			if (removePackageNamesFromSignature(methodSig).equals(removePackageNamesFromSignature(m.methodSig))) {
					return true;
			}
			return false;
				
		}

		public String toString() {
			return className
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

	public Naming(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	private boolean checkSuper(MyMethod m, HashSet<MyMethod> others) {
		for (MyMethod m2 : others) {
			try {
				if (m.confusingMethodNames(m2)
						&& Repository.instanceOf(m.className, m2.className)) {
					MyMethod m3 = new MyMethod(m.className, m2.methodName, m2.methodSig, m.isStatic);
					boolean r = others.contains(m3);
					if (r) continue;
					bugReporter.reportBug(new BugInstance(this, "NM_VERY_CONFUSING", HIGH_PRIORITY)
							.addClass(m.getClassName())
							.addMethod(m.getClassName(), m.methodName, m.methodSig, m.isStatic)
							.addClass(m2.getClassName())
							.addMethod(m2.getClassName(), m2.methodName, m2.methodSig, m2.isStatic));
					return true;
				}
			} catch (ClassNotFoundException e) {
			}
		}
		return false;
	}

	private boolean checkNonSuper(MyMethod m, HashSet<MyMethod> others) {
		for (MyMethod m2 : others) {
			if (m.confusingMethodNames(m2)) {
				bugReporter.reportBug(new BugInstance(this, "NM_CONFUSING", LOW_PRIORITY)
						.addClass(m.getClassName())
						.addMethod(m.getClassName(), m.methodName, m.methodSig, m.isStatic)
						.addClass(m2.getClassName())
						.addMethod(m2.getClassName(), m2.methodName, m2.methodSig, m2.isStatic));
				return true;
			}
		}
		return false;
	}


	public void report() {

	canonicalNameIterator:
	for (String allSmall : canonicalToTrueMapping.keySet()) {
		HashSet<String> s = canonicalToTrueMapping.get(allSmall);
		if (s.size() <= 1)
			continue;
		HashSet<MyMethod> conflictingMethods = canonicalToMyMethod.get(allSmall);
		for (Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext();) {
			if (checkSuper(j.next(), conflictingMethods))
				j.remove();
		}
		for (MyMethod conflictingMethod : conflictingMethods) {
			if (checkNonSuper(conflictingMethod, conflictingMethods))
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
			for (JavaClass aSuper : supers) {
				visitJavaClass(aSuper);
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
		if (name.endsWith("Exception") 
		&&  (!obj.getSuperclassName().endsWith("Exception"))
		&&  (!obj.getSuperclassName().endsWith("Error"))
		&&  (!obj.getSuperclassName().endsWith("Throwable"))) {
			bugReporter.reportBug(new BugInstance(this, 
					"NM_CLASS_NOT_EXCEPTION", 
					NORMAL_PRIORITY )
				        .addClass(this));
		}
			
		super.visit(obj);
	}

	public void visit(Field obj) {
		if (getFieldName().length() == 1) return;

		if (!obj.isFinal() 
			&& Character.isLetter(getFieldName().charAt(0))
			&& !Character.isLowerCase(getFieldName().charAt(0))
			&& getFieldName().indexOf("_") == -1
			&& Character.isLetter(getFieldName().charAt(1))
			&& Character.isLowerCase(getFieldName().charAt(1))) {
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
		}
	private static Pattern sigType = Pattern.compile("L([^;]*/)?([^/]+;)");
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
		String sig = getMethodSig();
		if (getMethodName().equals(baseClassName)) {
			bugReporter.reportBug(new BugInstance(this, "NM_METHOD_CONSTRUCTOR_CONFUSION",
			        (sig.equals("()V") 
					&& obj.getCode().getCode().length > 1
					&& !obj.isNative()
					)
			        ? HIGH_PRIORITY : NORMAL_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}

		if (obj.isAbstract()) return;
		if (obj.isPrivate()) return;

		if (getMethodName().equals("equal") && sig.equals("(Ljava/lang/Object;)Z")) {
			bugReporter.reportBug(new BugInstance(this, "NM_BAD_EQUAL", HIGH_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}
		if (getMethodName().equals("hashcode") && sig.equals("()I")) {
			bugReporter.reportBug(new BugInstance(this, "NM_LCASE_HASHCODE", HIGH_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}
		if (getMethodName().equals("tostring") && sig.equals("()Ljava/lang/String;")) {
			bugReporter.reportBug(new BugInstance(this, "NM_LCASE_TOSTRING", HIGH_PRIORITY)
			        .addClassAndMethod(this));
			return;
		}


		if (obj.isPrivate()
		        || obj.isStatic()
		        || getMethodName().equals("<init>")
		)
			return;

		String trueName = getMethodName() + sig;
		String sig2 = removePackageNamesFromSignature(sig);
		String allSmall = getMethodName().toLowerCase() + sig2;
	

		MyMethod mm = new MyMethod(getThisClass().getClassName(), getMethodName(), sig, obj.isStatic());
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

	private static String removePackageNamesFromSignature(String sig) {
		int end = sig.indexOf(")");
		Matcher m = sigType.matcher(sig.substring(0,end));
		return m.replaceAll("L$2") + sig.substring(end);
	}


}
