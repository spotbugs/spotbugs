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
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class Naming extends PreorderVisitor implements Detector {
	String baseClassName;
	boolean classIsPublicOrProtected;

	public static boolean definedIn(JavaClass clazz, XMethod m) {
		for(Method m2 : clazz.getMethods()) 
			if (m.getName().equals(m2.getName()) && m.getSignature().equals(m2.getSignature()) && m.isStatic() == m2.isStatic()) return true;
		return false;
	}

	public static  boolean confusingMethodNames(XMethod m1, XMethod m2) {
		if (m1.isStatic() != m2.isStatic()) return false;
		if (m1.getClassName().equals(m2.getClassName())) return false;
		
		if (m1.getName().equalsIgnoreCase(m2.getName())
		        && !m1.getName().equals(m2.getName())
		        && m1.getSignature().equals(m2.getSignature())) return true;
		if (m1.getSignature().equals(m2.getSignature())) return false;
		if (removePackageNamesFromSignature(m1.getSignature()).equals(removePackageNamesFromSignature(m2.getSignature()))) 
				return true;
		return false;		
	}

	// map of canonicalName -> trueMethodName
	HashMap<String, HashSet<String>> canonicalToTrueMapping
	        = new HashMap<String, HashSet<String>>();
	// map of canonicalName -> Set<XMethod>
	HashMap<String, HashSet<XMethod>> canonicalToXMethod
	        = new HashMap<String, HashSet<XMethod>>();

	HashSet<String> visited = new HashSet<String>();

	private BugReporter bugReporter;

	public Naming(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	private boolean checkSuper(XMethod m, HashSet<XMethod> others) {
        if (m.isStatic()) return false;
        if (m.getName().equals("<init>") || m.getName().equals("<clinit>")) return false;
		for (XMethod m2 : others) {
			try {
				if (confusingMethodNames(m, m2)
						&& Repository.instanceOf(m.getClassName(), m2.getClassName())) {
					int priority = HIGH_PRIORITY;
					try {
					JavaClass clazz = Repository.lookupClass(m.getClassName());
					if (definedIn(clazz, m2))
						priority+=2;
					for(JavaClass i : clazz.getAllInterfaces()) 
						if (definedIn(i, m)) 
							priority+=2;
					for(JavaClass s : clazz.getSuperClasses()) 
						if (definedIn(s, m)) 
							priority+=2;
					} catch (ClassNotFoundException e) {
						priority++;
						AnalysisContext.reportMissingClass(e);
					}
					String pattern = "NM_VERY_CONFUSING";
					if (priority == HIGH_PRIORITY && AnalysisContext.currentXFactory().isCalled(m)) priority = NORMAL_PRIORITY;
					else if (priority > NORMAL_PRIORITY && m.getSignature().equals(m2.getSignature())) {
						pattern = "NM_VERY_CONFUSING_INTENTIONAL";
						priority = NORMAL_PRIORITY;
					}
					XFactory xFactory = AnalysisContext.currentXFactory();
					if (xFactory.getDeprecated().contains(m) || xFactory.getDeprecated().contains(m2)) priority++;
					
					bugReporter.reportBug(new BugInstance(this, pattern, priority)
					.addClass(m.getClassName())
					.addMethod(m)
					.addClass(m2.getClassName())
					.addMethod(m2));
					return true;
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
		}
		return false;
	}

	private boolean checkNonSuper(XMethod m, HashSet<XMethod> others) {
        if (m.isStatic()) return false;
        if (m.getName().startsWith("<init>") || m.getName().startsWith("<clinit>")) return false;
		for (XMethod m2 : others) {
			if (confusingMethodNames(m,m2)) {
				bugReporter.reportBug(new BugInstance(this, "NM_CONFUSING", LOW_PRIORITY)
						.addClass(m.getClassName())
						.addMethod(m)
						.addClass(m2.getClassName())
						.addMethod(m2));
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
		HashSet<XMethod> conflictingMethods = canonicalToXMethod.get(allSmall);
		for (Iterator<XMethod> j = conflictingMethods.iterator(); j.hasNext();) {
			if (checkSuper(j.next(), conflictingMethods))
				j.remove();
		}
		for (XMethod conflictingMethod : conflictingMethods) {
			if (checkNonSuper(conflictingMethod, conflictingMethods))
				continue canonicalNameIterator;
		}
	}
	}

	@Override
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

	@Override
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

	@Override
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
	private final static Pattern sigType = Pattern.compile("L([^;]*/)?([^/]+;)");
	private boolean isInnerClass(JavaClass obj) {
		for(Field f : obj.getFields())
			if (f.getName().startsWith("this$")) return true;
		return false;
	}
	private boolean markedAsNotUsable(Method obj) {
		for(Attribute a : obj.getAttributes())
			if (a instanceof Deprecated) return true;
		Code code = obj.getCode();
		if (code == null) return false;
		byte [] codeBytes = code.getCode();
		if (codeBytes.length > 1 && codeBytes.length < 10) {
			int lastOpcode = codeBytes[codeBytes.length-1] & 0xff;
			if (lastOpcode != ATHROW) return false;
			for(int b : codeBytes) 
				if ((b & 0xff) == RETURN) return false;
			return true;
			}
		return false;
	}
    private static @CheckForNull Method findVoidConstructor(JavaClass clazz) {
        for(Method m : clazz.getMethods()) 
            if (m.getName().equals("<init>") && m.getSignature().equals("()V")) return m;
        return null;
        
    }
	@Override
         public void visit(Method obj) {
		String mName = getMethodName();
		if (mName.length() == 1) return;
		if (Character.isLetter(mName.charAt(0))
			&& !Character.isLowerCase(mName.charAt(0))
			&& Character.isLetter(mName.charAt(1))
			&& Character.isLowerCase(mName.charAt(1))
			&& mName.indexOf("_") == -1 )
			bugReporter.reportBug(new BugInstance(this, 
				"NM_METHOD_NAMING_CONVENTION", 
				classIsPublicOrProtected 
				 && (obj.isPublic() || obj.isProtected())  
				? NORMAL_PRIORITY
				: LOW_PRIORITY)
			        .addClassAndMethod(this));
		String sig = getMethodSig();
		if (mName.equals(baseClassName) && sig.equals("()V")) {
			Code code = obj.getCode();
            Method realVoidConstructor = findVoidConstructor(getThisClass());
			if (code != null && !markedAsNotUsable(obj)) {
				int priority = NORMAL_PRIORITY;
				if (codeDoesSomething(code))
					priority--;
                else if (!obj.isPublic() && getThisClass().isPublic()) 
					priority--;
                if (realVoidConstructor == null) priority++;
                
				bugReporter.reportBug( new BugInstance(this, "NM_METHOD_CONSTRUCTOR_CONFUSION", priority).addClassAndMethod(this).lowerPriorityIfDeprecated());
				return;
			}
		}

		if (obj.isAbstract()) return;
		if (obj.isPrivate()) return;

		if (mName.equals("equal") && sig.equals("(Ljava/lang/Object;)Z")) {
			bugReporter.reportBug(new BugInstance(this, "NM_BAD_EQUAL", HIGH_PRIORITY)
			        .addClassAndMethod(this).lowerPriorityIfDeprecated());
			return;
		}
		if (mName.equals("hashcode") && sig.equals("()I")) {
			bugReporter.reportBug(new BugInstance(this, "NM_LCASE_HASHCODE", HIGH_PRIORITY)
			        .addClassAndMethod(this).lowerPriorityIfDeprecated());
			return;
		}
		if (mName.equals("tostring") && sig.equals("()Ljava/lang/String;")) {
			bugReporter.reportBug(new BugInstance(this, "NM_LCASE_TOSTRING", HIGH_PRIORITY)
			        .addClassAndMethod(this).lowerPriorityIfDeprecated());
			return;
		}


		if (obj.isPrivate()
		        || obj.isStatic()
		        || mName.equals("<init>")
		)
			return;

		String trueName = mName + sig;
		String sig2 = removePackageNamesFromSignature(sig);
		String allSmall = mName.toLowerCase() + sig2;
	

		XMethod xm = XFactory.createXMethod(this);
		{
			HashSet<String> s = canonicalToTrueMapping.get(allSmall);
			if (s == null) {
				s = new HashSet<String>();
				canonicalToTrueMapping.put(allSmall, s);
			}
			s.add(trueName);
		}
		{
			HashSet<XMethod> s = canonicalToXMethod.get(allSmall);
			if (s == null) {
				s = new HashSet<XMethod>();
				canonicalToXMethod.put(allSmall, s);
			}
			s.add(xm);
		}

	}

    private boolean codeDoesSomething(Code code) {
        byte [] codeBytes = code.getCode();
        return codeBytes.length > 1;
    }

	private static String removePackageNamesFromSignature(String sig) {
		int end = sig.indexOf(")");
		Matcher m = sigType.matcher(sig.substring(0,end));
		return m.replaceAll("L$2") + sig.substring(end);
	}


}
