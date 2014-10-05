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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.props.AbstractWarningProperty;
import edu.umd.cs.findbugs.props.PriorityAdjustment;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class Naming extends PreorderVisitor implements Detector {

    public static class NamingProperty extends AbstractWarningProperty {

        private NamingProperty(String name, PriorityAdjustment priorityAdjustment) {
            super(name, priorityAdjustment);
        }

        public static final NamingProperty METHOD_IS_CALLED = new NamingProperty("CONFUSING_METHOD_IS_CALLED",
                PriorityAdjustment.AT_MOST_MEDIUM);

        public static final NamingProperty METHOD_IS_DEPRECATED = new NamingProperty("CONFUSING_METHOD_IS_DEPRECATED",
                PriorityAdjustment.LOWER_PRIORITY);
    }

    String baseClassName;

    boolean classIsPublicOrProtected;

    public static @CheckForNull
    XMethod definedIn(JavaClass clazz, XMethod m) {
        for (Method m2 : clazz.getMethods()) {
            if (m.getName().equals(m2.getName()) && m.getSignature().equals(m2.getSignature()) && m.isStatic() == m2.isStatic()) {
                return XFactory.createXMethod(clazz, m2);
            }
        }
        return null;
    }

    public static boolean confusingMethodNamesWrongCapitalization(XMethod m1, XMethod m2) {
        if (m1.isStatic() != m2.isStatic()) {
            return false;
        }
        if (m1.getClassName().equals(m2.getClassName())) {
            return false;
        }
        if (m1.getName().equals(m2.getName())) {
            return false;
        }
        if (m1.getName().equalsIgnoreCase(m2.getName())
                && removePackageNamesFromSignature(m1.getSignature()).equals(removePackageNamesFromSignature(m2.getSignature()))) {
            return true;
        }
        return false;
    }

    public static boolean confusingMethodNamesWrongPackage(XMethod m1, XMethod m2) {
        if (m1.isStatic() != m2.isStatic()) {
            return false;
        }
        if (m1.getClassName().equals(m2.getClassName())) {
            return false;
        }

        if (!m1.getName().equals(m2.getName())) {
            return false;
        }
        if (m1.getSignature().equals(m2.getSignature())) {
            return false;
        }
        if (removePackageNamesFromSignature(m1.getSignature()).equals(removePackageNamesFromSignature(m2.getSignature()))) {
            return true;
        }
        return false;
    }

    // map of canonicalName -> Set<XMethod>
    HashMap<String, TreeSet<XMethod>> canonicalToXMethod = new HashMap<String, TreeSet<XMethod>>();

    HashSet<String> visited = new HashSet<String>();

    private final BugReporter bugReporter;

    public Naming(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
    }

    private boolean checkSuper(XMethod m, Set<XMethod> others) {
        if (m.isStatic()) {
            return false;
        }
        if ("<init>".equals(m.getName()) || "<clinit>".equals(m.getName())) {
            return false;
        }
        for (XMethod m2 : others) {
            try {
                if ((confusingMethodNamesWrongCapitalization(m, m2) || confusingMethodNamesWrongPackage(m, m2))
                        && Repository.instanceOf(m.getClassName(), m2.getClassName())) {
                    WarningPropertySet<NamingProperty> propertySet = new WarningPropertySet<NamingProperty>();

                    int priority = HIGH_PRIORITY;
                    boolean intentional = false;
                    XMethod m3 = null;
                    try {
                        JavaClass clazz = Repository.lookupClass(m.getClassName());
                        if ((m3 = definedIn(clazz, m2)) != null) {
                            // the method we don't override is also defined in
                            // our class
                            priority = NORMAL_PRIORITY;
                            intentional = true;
                        }
                    } catch (ClassNotFoundException e) {
                        priority++;
                        AnalysisContext.reportMissingClass(e);
                    }

                    XFactory xFactory = AnalysisContext.currentXFactory();
                    if (m3 == null && xFactory.isCalled(m)) {
                        propertySet.addProperty(NamingProperty.METHOD_IS_CALLED);
                    } else if (m.isDeprecated() || m2.isDeprecated()) {
                        propertySet.addProperty(NamingProperty.METHOD_IS_DEPRECATED);
                    }

                    if (!m.getName().equals(m2.getName()) && m.getName().equalsIgnoreCase(m2.getName())) {
                        String pattern = intentional ? "NM_VERY_CONFUSING_INTENTIONAL" : "NM_VERY_CONFUSING";
                        Set<XMethod> overrides = Hierarchy2.findSuperMethods(m);
                        if (!overrides.isEmpty()) {
                            if (intentional || allAbstract(overrides)) {
                                break;
                            }
                            priority++;
                        }
                        BugInstance bug = new BugInstance(this, pattern, priority).addClass(m.getClassName()).addMethod(m)
                                .addClass(m2.getClassName()).describe(ClassAnnotation.SUPERCLASS_ROLE).addMethod(m2)
                                .describe(MethodAnnotation.METHOD_DID_YOU_MEAN_TO_OVERRIDE);
                        if (m3 != null) {
                            bug.addMethod(m3).describe(MethodAnnotation.METHOD_OVERRIDDEN);
                        }
                        propertySet.decorateBugInstance(bug);
                        bugReporter.reportBug(bug);
                    } else if (!m.getSignature().equals(m2.getSignature())
                            && removePackageNamesFromSignature(m.getSignature()).equals(
                                    removePackageNamesFromSignature(m2.getSignature()))) {
                        String pattern = intentional ? "NM_WRONG_PACKAGE_INTENTIONAL" : "NM_WRONG_PACKAGE";
                        Set<XMethod> overrides = Hierarchy2.findSuperMethods(m);
                        if (!overrides.isEmpty()) {
                            if (intentional || allAbstract(overrides)) {
                                break;
                            }
                            priority++;
                        }
                        Iterator<String> s = new SignatureParser(m.getSignature()).parameterSignatureIterator();
                        Iterator<String> s2 = new SignatureParser(m2.getSignature()).parameterSignatureIterator();
                        while (s.hasNext()) {
                            String p = s.next();
                            String p2 = s2.next();
                            if (!p.equals(p2)) {
                                BugInstance bug = new BugInstance(this, pattern, priority).addClass(m.getClassName())
                                        .addMethod(m).addClass(m2.getClassName()).describe(ClassAnnotation.SUPERCLASS_ROLE)
                                        .addMethod(m2).describe(MethodAnnotation.METHOD_DID_YOU_MEAN_TO_OVERRIDE)
                                        .addFoundAndExpectedType(p, p2);
                                if (m3 != null) {
                                    bug.addMethod(m3).describe(MethodAnnotation.METHOD_OVERRIDDEN);
                                }
                                propertySet.decorateBugInstance(bug);
                                bugReporter.reportBug(bug);

                            }
                        }

                        //
                    }
                    return true;
                }
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
        }
        return false;
    }

    private boolean allAbstract(Set<XMethod> overrides) {
        boolean allAbstract = true;
        for (XMethod m4 : overrides) {
            if (!m4.isAbstract()) {
                allAbstract = false;
            }
        }
        return allAbstract;
    }

    private boolean checkNonSuper(XMethod m, Set<XMethod> others) {
        if (m.isStatic()) {
            return false;
        }
        if (m.getName().startsWith("<init>") || m.getName().startsWith("<clinit>")) {
            return false;
        }
        for (XMethod m2 : others) {
            if (confusingMethodNamesWrongCapitalization(m, m2)) {
                XMethod mm1;
                XMethod mm2;
                if (m.compareTo(m2) < 0) {
                    mm1 = m;
                    mm2 = m2;
                } else {
                    mm1 = m2;
                    mm2 = m;
                }
                bugReporter.reportBug(new BugInstance(this, "NM_CONFUSING", LOW_PRIORITY).addClass(mm1.getClassName())
                        .addMethod(mm1).addClass(mm2.getClassName()).addMethod(mm2));
                return true;
            }
        }
        return false;
    }

    @Override
    public void report() {

        for (Map.Entry<String, TreeSet<XMethod>> e : canonicalToXMethod.entrySet()) {
            TreeSet<XMethod> conflictingMethods = e.getValue();
            HashSet<String> trueNames = new HashSet<String>();

            for (XMethod m : conflictingMethods) {
                trueNames.add(m.getName() + m.getSignature());
            }
            if (trueNames.size() <= 1) {
                continue;
            }
            for (Iterator<XMethod> j = conflictingMethods.iterator(); j.hasNext();) {
                if (checkSuper(j.next(), conflictingMethods)) {
                    j.remove();
                }
            }
            for (XMethod conflictingMethod : conflictingMethods) {
                if (checkNonSuper(conflictingMethod, conflictingMethods)) {
                    break;
                }
            }
        }
    }

    public String stripPackageName(String className) {
        if (className.indexOf('.') >= 0) {
            return className.substring(className.lastIndexOf('.') + 1);
        } else if (className.indexOf('/') >= 0) {
            return className.substring(className.lastIndexOf('/') + 1);
        } else {
            return className;
        }
    }

    public boolean sameSimpleName(String class1, String class2) {
        return class1 != null && class2 != null && stripPackageName(class1).equals(stripPackageName(class2));
    }

    @Override
    public void visitJavaClass(JavaClass obj) {
        if (BCELUtil.isSynthetic(obj)) {
            return;
        }
        String name = obj.getClassName();
        if (!visited.add(name)) {
            return;
        }

        String superClassName = obj.getSuperclassName();
        if (!"java.lang.Object".equals(name)) {
            if (sameSimpleName(superClassName, name)) {
                bugReporter.reportBug(new BugInstance(this, "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS", HIGH_PRIORITY).addClass(name)
                        .addClass(superClassName));
            }
            for (String interfaceName : obj.getInterfaceNames()) {
                if (sameSimpleName(interfaceName, name)) {
                    bugReporter.reportBug(new BugInstance(this, "NM_SAME_SIMPLE_NAME_AS_INTERFACE", NORMAL_PRIORITY).addClass(
                            name).addClass(interfaceName));
                }
            }
        }
        if (obj.isInterface()) {
            return;
        }

        if ("java.lang.Object".equals(superClassName) && !visited.contains(superClassName)) {
            try {
                visitJavaClass(obj.getSuperClass());
            } catch (ClassNotFoundException e) {
                // ignore it
            }
        }
        super.visitJavaClass(obj);
    }

    /**
     * Determine whether the class descriptor ultimately inherits from
     * java.lang.Exception
     *
     * @param d
     *            class descriptor we want to check
     * @return true iff the descriptor ultimately inherits from Exception
     */
    private static boolean mightInheritFromException(ClassDescriptor d) {
        while (d != null) {
            try {
                if ("java.lang.Exception".equals(d.getDottedClassName())) {
                    return true;
                }
                XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, d);
                d = classNameAndInfo.getSuperclassDescriptor();
            } catch (CheckedAnalysisException e) {
                return true; // don't know
            }
        }
        return false;
    }

    boolean hasBadMethodNames;

    boolean hasBadFieldNames;

    /**
     * Eclipse uses reflection to initialize NLS message bundles. Classes which
     * using this mechanism are usualy extending org.eclipse.osgi.util.NLS class
     * and contains lots of public static String fields which are used as
     * message constants. Unfortunately these fields often has bad names which
     * does not follow Java code convention, so FB reports tons of warnings for
     * such Eclipse message fields.
     *
     * @see edu.umd.cs.findbugs.detect.MutableStaticFields
     */
    private boolean isEclipseNLS;

    @Override
    public void visit(JavaClass obj) {
        String name = obj.getClassName();
        String[] parts = name.split("[$+.]");
        baseClassName = parts[parts.length - 1];
        for (String p : name.split("[.]")) {
            if (p.length() == 1) {
                return;
            }
        }
        if (name.indexOf("Proto$") >= 0) {
            return;
        }
        classIsPublicOrProtected = obj.isPublic() || obj.isProtected();
        if (Character.isLetter(baseClassName.charAt(0)) && !Character.isUpperCase(baseClassName.charAt(0))
                && baseClassName.indexOf('_') == -1) {
            int priority = classIsPublicOrProtected ? NORMAL_PRIORITY : LOW_PRIORITY;

            bugReporter.reportBug(new BugInstance(this, "NM_CLASS_NAMING_CONVENTION", priority).addClass(this));
        }
        if (name.endsWith("Exception")) {
            // Does it ultimately inherit from Throwable?
            if (!mightInheritFromException(DescriptorFactory.createClassDescriptor(obj))) {
                // It doens't, so the name is misleading
                bugReporter.reportBug(new BugInstance(this, "NM_CLASS_NOT_EXCEPTION", NORMAL_PRIORITY).addClass(this));
            }
        }

        int badFieldNames = 0;
        for (Field f : obj.getFields()) {
            if (f.getName().length() >= 2 && badFieldName(f)) {
                badFieldNames++;
            }
        }
        hasBadFieldNames = badFieldNames > 3 && badFieldNames > obj.getFields().length / 3;
        int badMethodNames = 0;
        for (Method m : obj.getMethods()) {
            if (badMethodName(m.getName())) {
                badMethodNames++;
            }
        }
        hasBadMethodNames = badMethodNames > 3 && badMethodNames > obj.getMethods().length / 3;
        isEclipseNLS = "org.eclipse.osgi.util.NLS".equals(obj.getSuperclassName());
        super.visit(obj);
    }

    @Override
    public void visit(Field obj) {
        if (getFieldName().length() == 1) {
            return;
        }

        if (isEclipseNLS) {
            int flags = obj.getAccessFlags();
            if ((flags & ACC_STATIC) != 0 && ((flags & ACC_PUBLIC) != 0) && "Ljava/lang/String;".equals(getFieldSig())) {
                // ignore "public statis String InstallIUCommandTooltip;"
                // messages from Eclipse NLS bundles
                return;
            }
        }
        if (badFieldName(obj)) {
            bugReporter.reportBug(new BugInstance(this, "NM_FIELD_NAMING_CONVENTION", classIsPublicOrProtected
                    && (obj.isPublic() || obj.isProtected()) && !hasBadFieldNames ? NORMAL_PRIORITY : LOW_PRIORITY)
            .addClass(this).addVisitedField(this));
        }
    }

    private boolean badFieldName(Field obj) {
        String fieldName = obj.getName();
        return !obj.isFinal() && Character.isLetter(fieldName.charAt(0)) && !Character.isLowerCase(fieldName.charAt(0))
                && fieldName.indexOf('_') == -1 && Character.isLetter(fieldName.charAt(1))
                && Character.isLowerCase(fieldName.charAt(1));
    }

    private final static Pattern sigType = Pattern.compile("L([^;]*/)?([^/]+;)");



    private static @CheckForNull
    String getSignatureOfOuterClass(JavaClass obj) {
        for (Field f : obj.getFields()) {
            if (f.getName().startsWith("this$")) {
                return f.getSignature();
            }
        }
        return null;
    }

    private boolean markedAsNotUsable(Method obj) {
        for (Attribute a : obj.getAttributes()) {
            if (a instanceof Deprecated) {
                return true;
            }
        }
        Code code = obj.getCode();
        if (code == null) {
            return false;
        }
        byte[] codeBytes = code.getCode();
        if (codeBytes.length > 1 && codeBytes.length < 10) {
            int lastOpcode = codeBytes[codeBytes.length - 1] & 0xff;
            if (lastOpcode != ATHROW) {
                return false;
            }
            for (int b : codeBytes) {
                if ((b & 0xff) == RETURN) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static @CheckForNull
    Method findVoidConstructor(JavaClass clazz) {
        for (Method m : clazz.getMethods()) {
            if (isVoidConstructor(clazz, m)) {
                return m;
            }
        }
        return null;

    }

    @Override
    public void visit(Method obj) {
        String mName = getMethodName();
        if (mName.length() == 1) {
            return;
        }
        if ("isRequestedSessionIdFromURL".equals(mName) || "isRequestedSessionIdFromUrl".equals(mName)) {
            return;
        }
        String sig = getMethodSig();
        if (mName.equals(baseClassName) && "()V".equals(sig)) {
            Code code = obj.getCode();
            Method realVoidConstructor = findVoidConstructor(getThisClass());
            if (code != null && !markedAsNotUsable(obj)) {
                int priority = NORMAL_PRIORITY;
                if (codeDoesSomething(code)) {
                    priority--;
                } else if (!obj.isPublic() && getThisClass().isPublic()) {
                    priority--;
                }
                boolean instanceMembers = false;
                for (Method m : this.getThisClass().getMethods()) {
                    if (!m.isStatic() && m != obj && !isVoidConstructor(getThisClass(), m)) {
                        instanceMembers = true;
                    }
                }
                for (Field f : this.getThisClass().getFields()) {
                    if (!f.isStatic()) {
                        instanceMembers = true;
                    }
                }
                if (!codeDoesSomething(code) && !instanceMembers && "java/lang/Object".equals(getSuperclassName())) {
                    priority += 2;
                }
                if (hasBadMethodNames) {
                    priority++;
                }
                if (!getXClass().getAnnotations().isEmpty()) {
                    priority++;
                }
                if (realVoidConstructor != null) {
                    priority = LOW_PRIORITY;
                }

                bugReporter.reportBug(new BugInstance(this, "NM_METHOD_CONSTRUCTOR_CONFUSION", priority).addClassAndMethod(this)
                        .lowerPriorityIfDeprecated());
                return;
            }
        } else if (badMethodName(mName)) {
            bugReporter.reportBug(new BugInstance(this, "NM_METHOD_NAMING_CONVENTION", classIsPublicOrProtected
                    && (obj.isPublic() || obj.isProtected()) && !hasBadMethodNames ? NORMAL_PRIORITY : LOW_PRIORITY)
            .addClassAndMethod(this));
        }

        if (obj.isAbstract()) {
            return;
        }
        if (obj.isPrivate()) {
            return;
        }

        if ("equal".equals(mName) && "(Ljava/lang/Object;)Z".equals(sig)) {
            bugReporter.reportBug(new BugInstance(this, "NM_BAD_EQUAL", HIGH_PRIORITY).addClassAndMethod(this)
                    .lowerPriorityIfDeprecated());
            return;
        }
        if ("hashcode".equals(mName) && "()I".equals(sig)) {
            bugReporter.reportBug(new BugInstance(this, "NM_LCASE_HASHCODE", HIGH_PRIORITY).addClassAndMethod(this)
                    .lowerPriorityIfDeprecated());
            return;
        }
        if ("tostring".equals(mName) && "()Ljava/lang/String;".equals(sig)) {
            bugReporter.reportBug(new BugInstance(this, "NM_LCASE_TOSTRING", HIGH_PRIORITY).addClassAndMethod(this)
                    .lowerPriorityIfDeprecated());
            return;
        }

        if (obj.isPrivate() || obj.isStatic() || "<init>".equals(mName)) {
            return;
        }

        String sig2 = removePackageNamesFromSignature(sig);
        String allSmall = mName.toLowerCase() + sig2;

        XMethod xm = getXMethod();
        {
            TreeSet<XMethod> s = canonicalToXMethod.get(allSmall);
            if (s == null) {
                s = new TreeSet<XMethod>();
                canonicalToXMethod.put(allSmall, s);
            }
            s.add(xm);
        }

    }

    private static boolean isVoidConstructor(JavaClass clazz, Method m) {
        String outerClassSignature = getSignatureOfOuterClass(clazz);
        if (outerClassSignature == null) {
            outerClassSignature = "";
        }
        return "<init>".equals(m.getName()) && m.getSignature().equals("(" + outerClassSignature + ")V");
    }

    private boolean badMethodName(String mName) {
        return mName.length() >= 2 && Character.isLetter(mName.charAt(0)) && !Character.isLowerCase(mName.charAt(0))
                && Character.isLetter(mName.charAt(1)) && Character.isLowerCase(mName.charAt(1)) && mName.indexOf('_') == -1;
    }

    private boolean codeDoesSomething(Code code) {
        byte[] codeBytes = code.getCode();
        return codeBytes.length > 1;
    }

    private static String removePackageNamesFromSignature(String sig) {
        int end = sig.indexOf(')');
        Matcher m = sigType.matcher(sig.substring(0, end));
        return m.replaceAll("L$2") + sig.substring(end);
    }

}
