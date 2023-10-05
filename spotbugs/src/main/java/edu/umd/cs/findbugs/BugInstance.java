/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.meta.When;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner.UnreachableCodeException;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.bcp.FieldVariable;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * <p>An instance of a bug pattern. A BugInstance consists of several parts:</p>
 * <ul>
 * <li>the type, which is a string indicating what kind of bug it is; used as a
 * key for the FindBugsMessages resource bundle</li>
 * <li>the priority; how likely this instance is to actually be a bug</li>
 * <li>a list of <em>annotations</em></li>
 * </ul>
 * <p>
 * The annotations describe classes, methods, fields, source locations, and
 * other relevant context information about the bug instance. Every BugInstance
 * must have at least one ClassAnnotation, which describes the class in which
 * the instance was found. This is the "primary class annotation".
 * </p>
 * <p>
 * BugInstance objects are built up by calling a string of <code>add</code>
 * methods. (These methods all "return this", so they can be chained). Some of
 * the add methods are specialized to get information automatically from a
 * BetterVisitor or DismantleBytecode object.</p>
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class BugInstance implements Comparable<BugInstance>, XMLWriteable, Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String type;

    private int priority;

    private final ArrayList<BugAnnotation> annotationList;

    private int cachedHashCode;

    private BugProperty propertyListHead, propertyListTail;

    private String oldInstanceHash;

    private String instanceHash;

    private int instanceOccurrenceNum;

    private int instanceOccurrenceMax;

    @CheckForNull
    private DetectorFactory detectorFactory;

    /*
     * The following fields are used for tracking Bug instances across multiple
     * versions of software. They are meaningless in a BugCollection for just
     * one version of software.
     */
    private long firstVersion;

    private long lastVersion;

    private boolean introducedByChangeOfExistingClass;

    private boolean removedByChangeOfPersistingClass;

    /**
     * This value is used to indicate that the cached hashcode is invalid, and
     * should be recomputed.
     */
    private static final int INVALID_HASH_CODE = 0;

    private static final String ELEMENT_NAME = "BugInstance";

    /**
     * This value is used to indicate whether BugInstances should be
     * reprioritized very low, when the BugPattern is marked as experimental
     */
    private static boolean adjustExperimental;

    private static Set<String> missingBugTypes = Collections.synchronizedSet(new HashSet<String>());

    public static class NoSuchBugPattern extends IllegalArgumentException {
        public final String type;

        public NoSuchBugPattern(String type) {
            super("Can't find definition of bug type " + type);
            this.type = type;
        }
    }

    /**
     * Constructor.
     *
     * @param type
     *            the bug type
     * @param priority
     *            the bug priority
     */
    public BugInstance(String type, int priority) {
        this.type = type.intern();
        this.priority = priority;
        lastVersion = -1;
        annotationList = new ArrayList<>(4);
        cachedHashCode = INVALID_HASH_CODE;

        BugPattern p = DetectorFactoryCollection.instance().lookupBugPattern(type);
        if (p == null) {
            if (missingBugTypes.add(type)) {
                String msg = "Can't find definition of bug type " + type;
                AnalysisContext.logError(msg, new NoSuchBugPattern(type));
            }
        } else {
            this.priority += p.getPriorityAdjustment();
        }
        if (adjustExperimental && isExperimental()) {
            this.priority = Priorities.EXP_PRIORITY;
        }
        boundPriority();
    }

    public static DateFormat firstSeenXMLFormat() {
        return new SimpleDateFormat("M/d/yy h:mm a", Locale.ENGLISH);
    }

    /*
    private boolean isFakeBugType(String type) {
        return "MISSING".equals(type) || "FOUND".equals(type);
    }
     */

    private void boundPriority() {
        priority = boundedPriority(priority);
    }

    @Override
    public Object clone() {
        BugInstance dup;
        try {
            dup = (BugInstance) super.clone();

            // Do deep copying of mutable objects
            for (int i = 0; i < dup.annotationList.size(); ++i) {
                dup.annotationList.set(i, (BugAnnotation) dup.annotationList.get(i).clone());
            }
            dup.propertyListHead = dup.propertyListTail = null;
            for (Iterator<BugProperty> i = propertyIterator(); i.hasNext();) {
                dup.addProperty((BugProperty) i.next().clone());
            }

            return dup;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Create a new BugInstance. This is the constructor that should be used by
     * Detectors.
     *
     * @param detector
     *            the Detector that is reporting the BugInstance
     * @param type
     *            the bug type
     * @param priority
     *            the bug priority
     */
    public BugInstance(Detector detector, String type, int priority) {
        this(type, priority);
        if (detector != null) {
            // Adjust priority if required
            String detectorName = detector.getClass().getName();
            adjustForDetector(detectorName);
        }

    }

    /**
     * @param detectorName
     */
    public void adjustForDetector(String detectorName) {
        DetectorFactory factory = DetectorFactoryCollection.instance().getFactoryByClassName(detectorName);
        detectorFactory = factory;
        if (factory != null) {
            this.priority += factory.getPriorityAdjustment();
            boundPriority();
            BugPattern bugPattern = getBugPattern();
            if (SystemProperties.ASSERTIONS_ENABLED && !"EXPERIMENTAL".equals(bugPattern.getCategory())
                    && !factory.getReportedBugPatterns().contains(bugPattern)) {
                AnalysisContext.logError(factory.getShortName() + " doesn't note that it reports "
                        + bugPattern + " in category " + bugPattern.getCategory());
            }
        }
    }

    /**
     * Create a new BugInstance. This is the constructor that should be used by
     * Detectors.
     *
     * @param detector
     *            the Detector2 that is reporting the BugInstance
     * @param type
     *            the bug type
     * @param priority
     *            the bug priority
     */
    public BugInstance(Detector2 detector, String type, int priority) {
        this(type, priority);

        if (detector != null) {
            // Adjust priority if required
            String detectorName = detector.getDetectorClassName();
            adjustForDetector(detectorName);
        }

    }

    public static void setAdjustExperimental(boolean adjust) {
        adjustExperimental = adjust;
    }

    /*
     * ----------------------------------------------------------------------
     * Accessors
     * ----------------------------------------------------------------------
     */

    /**
     * Get the bug pattern name (e.g., IL_INFINITE_RECURSIVE_LOOP)
     */
    public String getType() {
        return type;
    }

    /**
     * Get the BugPattern.
     */
    public @Nonnull BugPattern getBugPattern() {
        BugPattern result = DetectorFactoryCollection.instance().lookupBugPattern(getType());
        if (result != null) {
            return result;
        }
        AnalysisContext.logError("Unable to find description of bug pattern " + getType());
        result = DetectorFactoryCollection.instance().lookupBugPattern("UNKNOWN");
        if (result != null) {
            return result;
        }
        return BugPattern.REALLY_UNKNOWN;
    }

    /**
     * Get the bug priority.
     */
    public int getPriority() {
        return priority;
    }

    public int getBugRank() {
        return BugRanker.findRank(this);
    }

    public BugRankCategory getBugRankCategory() {
        return BugRankCategory.getRank(getBugRank());
    }

    /**
     * Get a string describing the bug priority and type. e.g.
     * "High Priority Correctness"
     *
     * @return a string describing the bug priority and type
     */
    public String getPriorityTypeString() {
        String priorityString = getPriorityString();
        BugPattern bugPattern = this.getBugPattern();
        // then get the category and put everything together
        String categoryString = I18N.instance().getBugCategoryDescription(bugPattern.getCategory());
        return priorityString + " Confidence " + categoryString;
        // TODO: internationalize the word "Confidence"
    }

    public String getPriorityTypeAbbreviation() {
        String priorityString = getPriorityAbbreviation();
        return priorityString + " " + getCategoryAbbrev();
    }

    public String getCategoryAbbrev() {
        BugPattern bugPattern = getBugPattern();
        return bugPattern.getCategoryAbbrev();
    }

    public String getPriorityString() {
        // first, get the priority
        int value = this.getPriority();
        String priorityString;
        if (value == Priorities.HIGH_PRIORITY) {
            priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_high", "High");
        } else if (value == Priorities.NORMAL_PRIORITY) {
            priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_normal", "Medium");
        } else if (value == Priorities.LOW_PRIORITY) {
            priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_low", "Low");
        } else if (value == Priorities.EXP_PRIORITY) {
            priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_experimental", "Experimental");
        } else {
            priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_ignore", "Ignore"); // This
        }
        // probably shouldn't ever happen, but what the hell, let's be complete
        return priorityString;
    }

    public String getPriorityAbbreviation() {
        return getPriorityString().substring(0, 1);
    }

    /**
     * Set the bug priority.
     */
    public void setPriority(int p) {
        priority = boundedPriority(p);
    }

    private int boundedPriority(int p) {
        return Math.max(Priorities.HIGH_PRIORITY, Math.min(Priorities.IGNORE_PRIORITY, p));
    }

    public void raisePriority() {
        priority = boundedPriority(priority - 1);

    }

    public void lowerPriority() {
        priority = boundedPriority(priority + 1);
    }

    public void lowerPriorityALot() {
        priority = boundedPriority(priority + 2);
    }

    /**
     * Is this bug instance the result of an experimental detector?
     */
    public boolean isExperimental() {
        BugPattern pattern = getBugPattern();
        return pattern.isExperimental();
    }

    /**
     * Get the primary class annotation, which indicates where the bug occurs.
     */
    public ClassAnnotation getPrimaryClass() {
        return findPrimaryAnnotationOfType(ClassAnnotation.class);
    }

    /**
     * Get the primary type annotation, which indicates where the bug occurs.
     */
    @CheckForNull
    public TypeAnnotation getPrimaryType() {
        return findPrimaryAnnotationOfType(TypeAnnotation.class);
    }

    /**
     * Get the primary method annotation, which indicates where the bug occurs.
     */
    @CheckForNull
    public MethodAnnotation getPrimaryMethod() {
        return findPrimaryAnnotationOfType(MethodAnnotation.class);
    }

    /**
     * Get the primary field annotation, which indicates where the bug occurs.
     */
    @CheckForNull
    public FieldAnnotation getPrimaryField() {
        return findPrimaryAnnotationOfType(FieldAnnotation.class);
    }

    @Nonnull
    public BugInstance lowerPriorityIfDeprecated() {
        MethodAnnotation m = getPrimaryMethod();
        if (m != null && XFactory.createXMethod(m).isDeprecated()) {
            lowerPriority();
        }
        FieldAnnotation f = getPrimaryField();
        if (f != null && XFactory.createXField(f).isDeprecated()) {
            lowerPriority();
        }
        return this;
    }

    /**
     * Find the first BugAnnotation in the list of annotations that is the same
     * type or a subtype as the given Class parameter.
     *
     * @param cls
     *            the Class parameter
     * @return the first matching BugAnnotation of the given type, or null if
     *         there is no such BugAnnotation
     */
    @CheckForNull
    private <T extends BugAnnotation> T findPrimaryAnnotationOfType(Class<T> cls) {
        T firstMatch = null;
        for (Iterator<BugAnnotation> i = annotationIterator(); i.hasNext();) {
            BugAnnotation annotation = i.next();
            if (cls.isAssignableFrom(annotation.getClass())) {
                if (annotation.getDescription().endsWith("DEFAULT")) {
                    return cls.cast(annotation);
                } else if (firstMatch == null) {
                    firstMatch = cls.cast(annotation);
                }
            }
        }
        return firstMatch;
    }

    public LocalVariableAnnotation getPrimaryLocalVariableAnnotation() {
        for (BugAnnotation annotation : annotationList) {
            if (annotation instanceof LocalVariableAnnotation) {
                return (LocalVariableAnnotation) annotation;
            }
        }
        return null;
    }

    /**
     * Get the primary source line annotation. There is guaranteed to be one
     * (unless some Detector constructed an invalid BugInstance).
     *
     * @return the source line annotation
     */
    @Nonnull
    public SourceLineAnnotation getPrimarySourceLineAnnotation() {
        // Highest priority: return the first top level source line annotation
        for (BugAnnotation annotation : annotationList) {
            if (annotation instanceof SourceLineAnnotation
                    && SourceLineAnnotation.DEFAULT_ROLE.equals(annotation.getDescription())
                    && !((SourceLineAnnotation) annotation).isUnknown()) {
                return (SourceLineAnnotation) annotation;
            }
        }

        for (BugAnnotation annotation : annotationList) {
            if (annotation instanceof SourceLineAnnotation && !((SourceLineAnnotation) annotation).isUnknown()) {
                return (SourceLineAnnotation) annotation;
            }
        }
        // Next: Try primary method, primary field, primary class
        SourceLineAnnotation srcLine;
        if ((srcLine = inspectPackageMemberSourceLines(getPrimaryMethod())) != null) {
            return srcLine;
        }
        if ((srcLine = inspectPackageMemberSourceLines(getPrimaryField())) != null) {
            return srcLine;
        }
        if ((srcLine = inspectPackageMemberSourceLines(getPrimaryClass())) != null) {
            return srcLine;
        }

        // Last resort: throw exception
        throw new IllegalStateException("BugInstance for " + getType()
                + " must contain at least one class, method, or field annotation");
    }

    public Collection<? extends SourceLineAnnotation> getAnotherInstanceSourceLineAnnotations() {
        // Highest priority: return the first top level source line annotation
        Collection<SourceLineAnnotation> result = new ArrayList<>();
        for (BugAnnotation annotation : annotationList) {
            if (annotation instanceof SourceLineAnnotation
                    && SourceLineAnnotation.ROLE_ANOTHER_INSTANCE.equals(annotation.getDescription())
                    && !((SourceLineAnnotation) annotation).isUnknown()) {
                result.add((SourceLineAnnotation) annotation);
            }
        }

        return result;
    }


    public String getInstanceKey() {
        String newValue = getInstanceKeyNew();
        return newValue;
    }

    private String getInstanceKeyNew() {
        StringBuilder buf = new StringBuilder(type);
        for (BugAnnotation annotation : annotationList) {
            if (annotation.isSignificant() || annotation instanceof IntAnnotation
                    || annotation instanceof LocalVariableAnnotation) {
                buf.append(":");
                buf.append(annotation.format("hash", null));
            }
        }

        return buf.toString();
    }

    /**
     * If given PackageMemberAnnotation is non-null, return its
     * SourceLineAnnotation.
     *
     * @param packageMember
     *            a PackageMemberAnnotation
     * @return the PackageMemberAnnotation's SourceLineAnnotation, or null if
     *         there is no SourceLineAnnotation
     */
    private SourceLineAnnotation inspectPackageMemberSourceLines(PackageMemberAnnotation packageMember) {
        return (packageMember != null) ? packageMember.getSourceLines() : null;
    }

    /**
     * Get an Iterator over all bug annotations.
     */
    public Iterator<BugAnnotation> annotationIterator() {
        return annotationList.iterator();
    }

    /**
     * Get an Iterator over all bug annotations.
     */
    public List<? extends BugAnnotation> getAnnotations() {
        return annotationList;
    }

    /** Get the first bug annotation with the specified class and role; return null if no
     * such annotation exists;
     */
    public @CheckForNull <A extends BugAnnotation> A getAnnotationWithRole(Class<A> c, String role) {
        for (BugAnnotation a : annotationList) {
            if (c.isInstance(a) && Objects.equals(role, a.getDescription())) {
                return c.cast(a);
            }
        }
        return null;
    }

    /**
     * Get the abbreviation of this bug instance's BugPattern. This is the same
     * abbreviation used by the BugCode which the BugPattern is a particular
     * species of.
     */
    public String getAbbrev() {
        BugPattern pattern = getBugPattern();
        return pattern.getAbbrev();
    }

    /*
     * ----------------------------------------------------------------------
     * Property accessors
     * ----------------------------------------------------------------------
     */

    private class BugPropertyIterator implements Iterator<BugProperty> {
        private BugProperty prev, cur;

        private boolean removed;

        @Override
        public boolean hasNext() {
            return findNext() != null;
        }

        @Override
        public BugProperty next() {
            BugProperty next = findNext();
            if (next == null) {
                throw new NoSuchElementException();
            }
            prev = cur;
            cur = next;
            removed = false;
            return cur;
        }

        @Override
        public void remove() {
            if (cur == null || removed) {
                throw new IllegalStateException();
            }
            if (prev == null) {
                propertyListHead = cur.getNext();
            } else {
                prev.setNext(cur.getNext());
            }
            if (cur == propertyListTail) {
                propertyListTail = prev;
            }
            removed = true;
        }

        private BugProperty findNext() {
            return cur == null ? propertyListHead : cur.getNext();
        }
    }

    /**
     * Get value of given property.
     *
     * @param name
     *            name of the property to get
     * @return the value of the named property, or null if the property has not
     *         been set
     */
    public String getProperty(String name) {
        BugProperty prop = lookupProperty(name);
        return prop != null ? prop.getValue() : null;
    }

    /**
     * Get value of given property, returning given default value if the
     * property has not been set.
     *
     * @param name
     *            name of the property to get
     * @param defaultValue
     *            default value to return if propery is not set
     * @return the value of the named property, or the default value if the
     *         property has not been set
     */
    public String getProperty(String name, String defaultValue) {
        String value = getProperty(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Get an Iterator over the properties defined in this BugInstance.
     *
     * @return Iterator over properties
     */
    public Iterator<BugProperty> propertyIterator() {
        return new BugPropertyIterator();
    }

    /**
     * Set value of given property.
     *
     * @param name
     *            name of the property to set
     * @param value
     *            the value of the property
     * @return this object, so calls can be chained
     */
    @Nonnull
    public BugInstance setProperty(String name, String value) {
        BugProperty prop = lookupProperty(name);
        if (prop != null) {
            prop.setValue(value);
        } else {
            prop = new BugProperty(name, value);
            addProperty(prop);
        }
        return this;
    }

    /**
     * Look up a property by name.
     *
     * @param name
     *            name of the property to look for
     * @return the BugProperty with the given name, or null if the property has
     *         not been set
     */
    public BugProperty lookupProperty(String name) {
        BugProperty prop = propertyListHead;

        while (prop != null) {
            if (prop.getName().equals(name)) {
                break;
            }
            prop = prop.getNext();
        }

        return prop;
    }

    /**
     * Delete property with given name.
     *
     * @param name
     *            name of the property to delete
     * @return true if a property with that name was deleted, or false if there
     *         is no such property
     */
    public boolean deleteProperty(String name) {
        BugProperty prev = null;
        BugProperty prop = propertyListHead;

        while (prop != null) {
            if (prop.getName().equals(name)) {
                break;
            }
            prev = prop;
            prop = prop.getNext();
        }

        if (prop != null) {
            if (prev != null) {
                // Deleted node in interior or at tail of list
                prev.setNext(prop.getNext());
            } else {
                // Deleted node at head of list
                propertyListHead = prop.getNext();
            }

            if (prop.getNext() == null) {
                // Deleted node at end of list
                propertyListTail = prev;
            }

            return true;
        } else {
            // No such property
            return false;
        }
    }

    private void addProperty(BugProperty prop) {
        if (propertyListTail != null) {
            propertyListTail.setNext(prop);
            propertyListTail = prop;
        } else {
            propertyListHead = propertyListTail = prop;
        }
        prop.setNext(null);
    }

    /*
     * ----------------------------------------------------------------------
     * Generic BugAnnotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add a Collection of BugAnnotations.
     *
     * @param annotationCollection
     *            Collection of BugAnnotations
     */
    @Nonnull
    public BugInstance addAnnotations(Collection<? extends BugAnnotation> annotationCollection) {
        for (BugAnnotation annotation : annotationCollection) {
            add(annotation);
        }
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Combined annotation adders
     * ----------------------------------------------------------------------
     */
    @Nonnull
    public BugInstance addClassAndMethod(MethodDescriptor methodDescriptor) {
        addClass(ClassName.toDottedClassName(methodDescriptor.getSlashedClassName()));
        add(MethodAnnotation.fromMethodDescriptor(methodDescriptor));
        return this;
    }

    public BugInstance addClassAndMethod(XMethod xMethod) {
        return addClassAndMethod(xMethod.getMethodDescriptor());
    }

    /**
     * Add a class annotation and a method annotation for the class and method
     * which the given visitor is currently visiting.
     *
     * @param visitor
     *            the BetterVisitor
     * @return this object
     */
    @Nonnull
    public BugInstance addClassAndMethod(PreorderVisitor visitor) {
        addClass(visitor);
        XMethod m = visitor.getXMethod();
        addMethod(visitor);
        if (!MemberUtils.isUserGenerated(m)) {
            foundInAutogeneratedMethod();
        }
        return this;
    }

    private void foundInAutogeneratedMethod() {
        if (annotationList.size() != 2) {
            return;
        }
        priority += 2;
        setProperty("FOUND_IN_SYNTHETIC_METHOD", "true");
        if (SystemProperties.ASSERTIONS_ENABLED && AnalysisContext.analyzingApplicationClass() && priority <= 3) {
            AnalysisContext.logError("Adding error " + getBugPattern().getType() + " to synthetic method " + getPrimaryMethod());
        }
    }

    /**
     * Add class and method annotations for given method.
     *
     * @param methodAnnotation
     *            the method
     * @return this object
     */
    @Nonnull
    public BugInstance addClassAndMethod(MethodAnnotation methodAnnotation) {
        addClass(methodAnnotation.getClassName());
        addMethod(methodAnnotation);
        return this;
    }

    /**
     * Add class and method annotations for given method.
     *
     * @param methodGen
     *            the method
     * @param sourceFile
     *            source file the method is defined in
     * @return this object
     */
    @Nonnull
    public BugInstance addClassAndMethod(MethodGen methodGen, String sourceFile) {
        addClass(methodGen.getClassName());
        addMethod(methodGen, sourceFile);
        if (!MemberUtils.isUserGenerated(methodGen)) {
            foundInAutogeneratedMethod();
        }
        return this;
    }

    /**
     * Add class and method annotations for given class and method.
     *
     * @param javaClass
     *            the class
     * @param method
     *            the method
     * @return this object
     */
    @Nonnull
    public BugInstance addClassAndMethod(JavaClass javaClass, Method method) {
        addClass(javaClass.getClassName());
        addMethod(javaClass, method);

        if (!MemberUtils.isUserGenerated(method)) {
            foundInAutogeneratedMethod();
        }
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Class annotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add a class annotation. If this is the first class annotation added, it
     * becomes the primary class annotation.
     *
     * @param className
     *            the name of the class
     * @param sourceFileName
     *            the source file of the class
     * @return this object
     */
    @Nonnull
    public BugInstance addClass(String className, String sourceFileName) {
        ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFileName);
        add(classAnnotation);
        return this;
    }

    /**
     * Add a class annotation. If this is the first class annotation added, it
     * becomes the primary class annotation.
     *
     * @param className
     *            the name of the class
     * @return this object
     */
    @Nonnull
    public BugInstance addClass(@SlashedClassName(when = When.UNKNOWN) String className) {
        ClassAnnotation classAnnotation = new ClassAnnotation(ClassName.toDottedClassName(className));
        add(classAnnotation);
        return this;
    }

    /**
     * Add a class annotation for the classNode.
     *
     * @param classNode
     *            the ASM visitor
     * @return this object
     */
    @Nonnull
    public BugInstance addClass(ClassNode classNode) {
        String dottedClassName = ClassName.toDottedClassName(classNode.name);
        ClassAnnotation classAnnotation = new ClassAnnotation(dottedClassName);
        add(classAnnotation);
        return this;
    }

    /**
     * Add a class annotation. If this is the first class annotation added, it
     * becomes the primary class annotation.
     *
     * @param classDescriptor
     *            the class to add
     * @return this object
     */
    @Nonnull
    public BugInstance addClass(ClassDescriptor classDescriptor) {
        add(ClassAnnotation.fromClassDescriptor(classDescriptor));
        return this;
    }

    /**
     * Add a class annotation. If this is the first class annotation added, it
     * becomes the primary class annotation.
     *
     * @param jclass
     *            the JavaClass object for the class
     * @return this object
     */
    @Nonnull
    public BugInstance addClass(JavaClass jclass) {
        addClass(jclass.getClassName());
        return this;
    }

    /**
     * Add a class annotation for the class that the visitor is currently
     * visiting.
     *
     * @param visitor
     *            the BetterVisitor
     * @return this object
     */
    @Nonnull
    public BugInstance addClass(PreorderVisitor visitor) {
        String className = visitor.getDottedClassName();
        addClass(className);
        return this;
    }

    /**
     * Add a class annotation for the superclass of the class the visitor is
     * currently visiting.
     *
     * @param visitor
     *            the BetterVisitor
     * @return this object
     */
    @Nonnull
    public BugInstance addSuperclass(PreorderVisitor visitor) {
        String className = ClassName.toDottedClassName(visitor.getSuperclassName());
        addClass(className);
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Type annotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add a type annotation. Handy for referring to array types.
     *
     * <p>
     * For information on type descriptors, <br>
     * see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.
     * html#14152 <br>
     * or http://www.murrayc.com/learning/java/java_classfileformat.shtml#
     * TypeDescriptors
     *
     * @param typeDescriptor
     *            a jvm type descriptor, such as "[I"
     * @return this object
     */
    @Nonnull
    public BugInstance addType(String typeDescriptor) {
        TypeAnnotation typeAnnotation = new TypeAnnotation(typeDescriptor);
        add(typeAnnotation);
        return this;
    }

    @Nonnull
    public BugInstance addType(Type type) {
        TypeAnnotation typeAnnotation = new TypeAnnotation(type);
        add(typeAnnotation);
        return this;
    }

    @Nonnull
    public BugInstance addFoundAndExpectedType(Type foundType, Type expectedType) {

        add(new TypeAnnotation(foundType, TypeAnnotation.FOUND_ROLE));
        add(new TypeAnnotation(expectedType, TypeAnnotation.EXPECTED_ROLE));
        return this;
    }

    @Nonnull
    public BugInstance addFoundAndExpectedType(String foundType, String expectedType) {
        add(new TypeAnnotation(foundType, TypeAnnotation.FOUND_ROLE));
        add(new TypeAnnotation(expectedType, TypeAnnotation.EXPECTED_ROLE));
        return this;
    }

    @Nonnull
    public BugInstance addEqualsMethodUsed(ClassDescriptor expectedClass) {
        try {
            Set<XMethod> targets = Hierarchy2.resolveVirtualMethodCallTargets(expectedClass, "equals", "(Ljava/lang/Object;)Z",
                    false, false);
            addEqualsMethodUsed(targets);
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return this;
    }

    @Nonnull
    public BugInstance addEqualsMethodUsed(@CheckForNull Collection<XMethod> equalsMethods) {
        if (equalsMethods == null) {
            return this;
        }
        if (equalsMethods.size() < 5) {
            for (XMethod m : equalsMethods) {
                addMethod(m).describe(MethodAnnotation.METHOD_EQUALS_USED);
            }
        } else {
            addMethod(equalsMethods.iterator().next()).describe(MethodAnnotation.METHOD_EQUALS_USED);
        }
        return this;
    }

    @Nonnull
    public BugInstance addTypeOfNamedClass(@DottedClassName String typeName) {
        TypeAnnotation typeAnnotation = new TypeAnnotation("L" + typeName.replace('.', '/') + ";");
        add(typeAnnotation);
        return this;
    }

    @Nonnull
    public BugInstance addType(ClassDescriptor c) {
        TypeAnnotation typeAnnotation = new TypeAnnotation(c.getSignature());
        add(typeAnnotation);
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Field annotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add a field annotation.
     *
     * @param className
     *            name of the class containing the field
     * @param fieldName
     *            the name of the field
     * @param fieldSig
     *            type signature of the field
     * @param isStatic
     *            whether or not the field is static
     * @return this object
     */
    @Nonnull
    public BugInstance addField(String className, String fieldName, String fieldSig, boolean isStatic) {
        addField(new FieldAnnotation(className, fieldName, fieldSig, isStatic));
        return this;
    }

    /**
     * Add a field annotation.
     *
     * @param className
     *            name of the class containing the field
     * @param fieldName
     *            the name of the field
     * @param fieldSig
     *            type signature of the field
     * @param accessFlags
     *            access flags for the field
     * @return this object
     */
    @Nonnull
    public BugInstance addField(String className, String fieldName, String fieldSig, int accessFlags) {
        addField(new FieldAnnotation(className, fieldName, fieldSig, accessFlags));
        return this;
    }

    @Nonnull
    public BugInstance addField(PreorderVisitor visitor) {
        FieldAnnotation fieldAnnotation = FieldAnnotation.fromVisitedField(visitor);
        return addField(fieldAnnotation);
    }

    /**
     * Add a field annotation
     *
     * @param fieldAnnotation
     *            the field annotation
     * @return this object
     */
    @Nonnull
    public BugInstance addField(FieldAnnotation fieldAnnotation) {
        add(fieldAnnotation);
        return this;
    }

    /**
     * Add a field annotation for a FieldVariable matched in a ByteCodePattern.
     *
     * @param field
     *            the FieldVariable
     * @return this object
     */
    @Nonnull
    public BugInstance addField(FieldVariable field) {
        return addField(field.getClassName(), field.getFieldName(), field.getFieldSig(), field.isStatic());
    }

    /**
     * Add a field annotation for an XField.
     *
     * @param xfield
     *            the XField
     * @return this object
     */
    @Nonnull
    public BugInstance addOptionalField(@CheckForNull XField xfield) {
        if (xfield == null) {
            return this;
        }
        return addField(xfield.getClassName(), xfield.getName(), xfield.getSignature(), xfield.isStatic());
    }

    /**
     * Add a field annotation for an XField.
     *
     * @param xfield
     *            the XField
     * @return this object
     */
    @Nonnull
    public BugInstance addField(XField xfield) {
        return addField(xfield.getClassName(), xfield.getName(), xfield.getSignature(), xfield.isStatic());
    }

    /**
     * Add a field annotation for a FieldDescriptor.
     *
     * @param fieldDescriptor
     *            the FieldDescriptor
     * @return this object
     */
    @Nonnull
    public BugInstance addField(FieldDescriptor fieldDescriptor) {
        FieldAnnotation fieldAnnotation = FieldAnnotation.fromFieldDescriptor(fieldDescriptor);
        add(fieldAnnotation);
        return this;
    }

    /**
     * Add a field annotation for the field which has just been accessed by the
     * method currently being visited by given visitor. Assumes that a
     * getfield/putfield or getstatic/putstatic has just been seen.
     *
     * @param visitor
     *            the DismantleBytecode object
     * @return this object
     */
    @Nonnull
    public BugInstance addReferencedField(DismantleBytecode visitor) {
        FieldAnnotation f = FieldAnnotation.fromReferencedField(visitor);
        addField(f);
        return this;
    }

    /**
     * Add a field annotation for the field referenced by the FieldAnnotation
     * parameter
     */
    @Nonnull
    public BugInstance addReferencedField(FieldAnnotation fa) {
        addField(fa);
        return this;
    }

    /**
     * Add a field annotation for the field which is being visited by given
     * visitor.
     *
     * @param visitor
     *            the visitor
     * @return this object
     */
    @Nonnull
    public BugInstance addVisitedField(PreorderVisitor visitor) {
        FieldAnnotation f = FieldAnnotation.fromVisitedField(visitor);
        addField(f);
        return this;
    }

    /**
     * Local variable adders
     */
    @Nonnull
    public BugInstance addOptionalLocalVariable(DismantleBytecode dbc, OpcodeStack.Item item) {
        int register = item.getRegisterNumber();

        if (register >= 0) {
            this.add(LocalVariableAnnotation.getLocalVariableAnnotation(dbc.getMethod(), register, dbc.getPC() - 1, dbc.getPC()));
        }
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Method annotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add a method annotation. If this is the first method annotation added, it
     * becomes the primary method annotation.
     *
     * @param className
     *            name of the class containing the method
     * @param methodName
     *            name of the method
     * @param methodSig
     *            type signature of the method
     * @param isStatic
     *            true if the method is static, false otherwise
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(String className, String methodName, String methodSig, boolean isStatic) {
        addMethod(MethodAnnotation.fromForeignMethod(className, methodName, methodSig, isStatic));
        return this;
    }

    /**
     * Add a method annotation. If this is the first method annotation added, it
     * becomes the primary method annotation.
     *
     * @param className
     *            name of the class containing the method
     * @param methodName
     *            name of the method
     * @param methodSig
     *            type signature of the method
     * @param accessFlags
     *            accessFlags for the method
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(@SlashedClassName String className, String methodName, String methodSig, int accessFlags) {
        addMethod(MethodAnnotation.fromForeignMethod(className, methodName, methodSig, accessFlags));
        return this;
    }

    /**
     * Add a method annotation. If this is the first method annotation added, it
     * becomes the primary method annotation. If the method has source line
     * information, then a SourceLineAnnotation is added to the method.
     *
     * @param methodGen
     *            the MethodGen object for the method
     * @param sourceFile
     *            source file method is defined in
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(MethodGen methodGen, String sourceFile) {
        String className = methodGen.getClassName();
        MethodAnnotation methodAnnotation = new MethodAnnotation(className, methodGen.getName(), methodGen.getSignature(),
                methodGen.isStatic());
        addMethod(methodAnnotation);
        addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(methodGen, sourceFile));
        return this;
    }

    /**
     * Add a method annotation. If this is the first method annotation added, it
     * becomes the primary method annotation. If the method has source line
     * information, then a SourceLineAnnotation is added to the method.
     *
     * @param javaClass
     *            the class the method is defined in
     * @param method
     *            the method
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(JavaClass javaClass, Method method) {
        MethodAnnotation methodAnnotation = new MethodAnnotation(javaClass.getClassName(), method.getName(),
                method.getSignature(), method.isStatic());
        SourceLineAnnotation methodSourceLines = SourceLineAnnotation.forEntireMethod(javaClass, method);
        methodAnnotation.setSourceLines(methodSourceLines);
        addMethod(methodAnnotation);
        return this;
    }

    /**
     * Add a method annotation. If this is the first method annotation added, it
     * becomes the primary method annotation. If the method has source line
     * information, then a SourceLineAnnotation is added to the method.
     *
     * @param classAndMethod
     *            JavaClassAndMethod identifying the method to add
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(JavaClassAndMethod classAndMethod) {
        return addMethod(classAndMethod.getJavaClass(), classAndMethod.getMethod());
    }

    /**
     * Add a method annotation for the method which the given visitor is
     * currently visiting. If the method has source line information, then a
     * SourceLineAnnotation is added to the method.
     *
     * @param visitor
     *            the BetterVisitor
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(PreorderVisitor visitor) {
        MethodAnnotation methodAnnotation = MethodAnnotation.fromVisitedMethod(visitor);
        addMethod(methodAnnotation);
        addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(visitor));
        return this;
    }

    /**
     * Add a method annotation for the method which has been called by the
     * method currently being visited by given visitor. Assumes that the visitor
     * has just looked at an invoke instruction of some kind.
     *
     * @param visitor
     *            the DismantleBytecode object
     * @return this object
     */
    @Nonnull
    public BugInstance addCalledMethod(DismantleBytecode visitor) {
        return addMethod(MethodAnnotation.fromCalledMethod(visitor)).describe(MethodAnnotation.METHOD_CALLED);
    }

    @Nonnull
    public BugInstance addCalledMethod(XMethod m) {
        return addMethod(m).describe(MethodAnnotation.METHOD_CALLED);
    }

    /**
     * Add a method annotation.
     *
     * @param className
     *            name of class containing called method
     * @param methodName
     *            name of called method
     * @param methodSig
     *            signature of called method
     * @param isStatic
     *            true if called method is static, false if not
     * @return this object
     */
    @Nonnull
    public BugInstance addCalledMethod(String className, String methodName, String methodSig, boolean isStatic) {
        return addMethod(MethodAnnotation.fromCalledMethod(className, methodName, methodSig, isStatic)).describe(
                MethodAnnotation.METHOD_CALLED);
    }

    /**
     * Add a method annotation for the method which is called by given
     * instruction.
     *
     * @param cpg
     *            the constant pool for the method containing the call
     * @param inv
     *            the InvokeInstruction
     * @return this object
     */
    @Nonnull
    public BugInstance addCalledMethod(ConstantPoolGen cpg, InvokeInstruction inv) {
        String className = inv.getClassName(cpg);
        String methodName = inv.getMethodName(cpg);
        String methodSig = inv.getSignature(cpg);
        addMethod(className, methodName, methodSig, inv.getOpcode() == Const.INVOKESTATIC);
        describe(MethodAnnotation.METHOD_CALLED);
        return this;
    }

    /**
     * Add a method annotation for the method which is called by given
     * instruction.
     *
     * @param methodGen
     *            the method containing the call
     * @param inv
     *            the InvokeInstruction
     * @return this object
     */
    @Nonnull
    public BugInstance addCalledMethod(MethodGen methodGen, InvokeInstruction inv) {
        ConstantPoolGen cpg = methodGen.getConstantPool();
        return addCalledMethod(cpg, inv);
    }

    /**
     * Add a MethodAnnotation from an XMethod.
     *
     * @param xmethod
     *            the XMethod
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(XMethod xmethod) {
        addMethod(MethodAnnotation.fromXMethod(xmethod));
        return this;
    }

    /**
     * Add a MethodAnnotation from an MethodDescriptor.
     *
     * @param method
     *            the method
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(MethodDescriptor method) {
        addMethod(MethodAnnotation.fromMethodDescriptor(method));
        return this;
    }

    /**
     * Add a method annotation. If this is the first method annotation added, it
     * becomes the primary method annotation.
     *
     * @param methodAnnotation
     *            the method annotation
     * @return this object
     */
    @Nonnull
    public BugInstance addMethod(MethodAnnotation methodAnnotation) {
        add(methodAnnotation);
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Integer annotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add an integer annotation.
     *
     * @param value
     *            the integer value
     * @return this object
     */
    @Nonnull
    public BugInstance addInt(int value) {
        add(new IntAnnotation(value));
        return this;
    }

    /**
     * Add an annotation about a parameter
     *
     * @param index parameter index, starting from 0
     *
     * @param role the role used to describe the parameter
     */
    @Nonnull
    public BugInstance addParameterAnnotation(int index, String role) {
        return addInt(index + 1).describe(role);
    }

    /**
     * Add a String annotation.
     *
     * @param value
     *            the String value
     * @return this object
     */
    @Nonnull
    public BugInstance addString(String value) {
        add(StringAnnotation.fromRawString(value));
        return this;
    }

    /**
     * Add a String annotation.
     *
     * @param c
     *            the char value
     * @return this object
     */
    @Nonnull
    public BugInstance addString(char c) {
        add(StringAnnotation.fromRawString(Character.toString(c)));
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Source line annotation adders
     * ----------------------------------------------------------------------
     */

    /**
     * Add a source line annotation.
     *
     * @param sourceLine
     *            the source line annotation
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLine(SourceLineAnnotation sourceLine) {
        add(sourceLine);
        return this;
    }

    /**
     * Add a source line annotation for instruction whose PC is given in the
     * method that the given visitor is currently visiting. Note that if the
     * method does not have line number information, then no source line
     * annotation will be added.
     *
     * @param visitor
     *            a BytecodeScanningDetector that is currently visiting the
     *            method
     * @param pc
     *            bytecode offset of the instruction
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLine(BytecodeScanningDetector visitor, int pc) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(visitor.getClassContext(),
                visitor, pc);
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /**
     * Add a source line annotation for instruction whose PC is given in the
     * method that the given visitor is currently visiting. Note that if the
     * method does not have line number information, then no source line
     * annotation will be added.
     *
     * @param classContext
     *            the ClassContext
     * @param visitor
     *            a PreorderVisitor that is currently visiting the method
     * @param pc
     *            bytecode offset of the instruction
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLine(ClassContext classContext, PreorderVisitor visitor, int pc) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, visitor, pc);
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /**
     * Add a source line annotation for the given instruction in the given
     * method. Note that if the method does not have line number information,
     * then no source line annotation will be added.
     *
     * @param classContext
     *            the ClassContext
     * @param methodGen
     *            the method being visited
     * @param sourceFile
     *            source file the method is defined in
     * @param handle
     *            the InstructionHandle containing the visited instruction
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLine(ClassContext classContext, MethodGen methodGen, String sourceFile,
            @Nonnull InstructionHandle handle) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                sourceFile, handle);
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /**
     * Add a source line annotation describing a range of instructions.
     *
     * @param classContext
     *            the ClassContext
     * @param methodGen
     *            the method
     * @param sourceFile
     *            source file the method is defined in
     * @param start
     *            the start instruction in the range
     * @param end
     *            the end instruction in the range (inclusive)
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLine(ClassContext classContext, MethodGen methodGen, String sourceFile, InstructionHandle start,
            InstructionHandle end) {
        // Make sure start and end are really in the right order.
        if (start.getPosition() > end.getPosition()) {
            InstructionHandle tmp = start;
            start = end;
            end = tmp;
        }
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstructionRange(classContext, methodGen,
                sourceFile, start, end);
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /**
     * Add source line annotation for given Location in a method.
     *
     * @param classContext
     *            the ClassContext
     * @param method
     *            the Method
     * @param location
     *            the Location in the method
     * @return this BugInstance
     */
    @Nonnull
    public BugInstance addSourceLine(ClassContext classContext, Method method, Location location) {
        return addSourceLine(classContext, method, location.getHandle());
    }

    /**
     * Add source line annotation for given Location in a method.
     *
     * @param methodDescriptor
     *            the method
     * @param location
     *            the Location in the method
     * @return this BugInstance
     */
    @Nonnull
    public BugInstance addSourceLine(MethodDescriptor methodDescriptor, Location location) {
        try {
            IAnalysisCache analysisCache = Global.getAnalysisCache();
            ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
            Method method = analysisCache.getMethodAnalysis(Method.class, methodDescriptor);
            return addSourceLine(classContext, method, location);
        } catch (CheckedAnalysisException e) {
            return addSourceLine(SourceLineAnnotation.createReallyUnknown(methodDescriptor.getClassDescriptor()
                    .toDottedClassName()));
        }
    }

    /**
     * Add source line annotation for given Location in a method.
     *
     * @param classContext
     *            the ClassContext
     * @param method
     *            the Method
     * @param handle
     *            InstructionHandle of an instruction in the method
     * @return this BugInstance
     */
    @Nonnull
    public BugInstance addSourceLine(ClassContext classContext, Method method, InstructionHandle handle) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, method,
                handle.getPosition());
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /**
     * Add a source line annotation describing the source line numbers for a
     * range of instructions in the method being visited by the given visitor.
     * Note that if the method does not have line number information, then no
     * source line annotation will be added.
     *
     * @param visitor
     *            a BetterVisitor which is visiting the method
     * @param startPC
     *            the bytecode offset of the start instruction in the range
     * @param endPC
     *            the bytecode offset of the end instruction in the range
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLineRange(BytecodeScanningDetector visitor, int startPC, int endPC) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstructionRange(visitor.getClassContext(),
                visitor, startPC, endPC);
        requireNonNull(sourceLineAnnotation);
        add(sourceLineAnnotation);
        return this;
    }

    /**
     * Add a source line annotation describing the source line numbers for a
     * range of instructions in the method being visited by the given visitor.
     * Note that if the method does not have line number information, then no
     * source line annotation will be added.
     *
     * @param classContext
     *            the ClassContext
     * @param visitor
     *            a BetterVisitor which is visiting the method
     * @param startPC
     *            the bytecode offset of the start instruction in the range
     * @param endPC
     *            the bytecode offset of the end instruction in the range
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLineRange(ClassContext classContext, PreorderVisitor visitor, int startPC, int endPC) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstructionRange(classContext, visitor,
                startPC, endPC);
        requireNonNull(sourceLineAnnotation);
        add(sourceLineAnnotation);
        return this;
    }

    /**
     * Add a source line annotation for instruction currently being visited by
     * given visitor. Note that if the method does not have line number
     * information, then no source line annotation will be added.
     *
     * @param visitor
     *            a BytecodeScanningDetector visitor that is currently visiting
     *            the instruction
     * @return this object
     */
    @Nonnull
    public BugInstance addSourceLine(BytecodeScanningDetector visitor) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(visitor);
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /**
     * Add a non-specific source line annotation. This will result in the entire
     * source file being displayed.
     *
     * @param className
     *            the class name
     * @param sourceFile
     *            the source file name
     * @return this object
     */
    @Nonnull
    public BugInstance addUnknownSourceLine(String className, String sourceFile) {
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.createUnknown(className, sourceFile);
        if (sourceLineAnnotation != null) {
            add(sourceLineAnnotation);
        }
        return this;
    }

    /*
     * ----------------------------------------------------------------------
     * Formatting support
     * ----------------------------------------------------------------------
     */

    /**
     * Format a string describing this bug instance.
     *
     * @return the description
     */
    @Nonnull
    public String getMessageWithoutPrefix() {
        BugPattern bugPattern = getBugPattern();
        String pattern, shortPattern;

        pattern = getLongDescription();
        shortPattern = bugPattern.getShortDescription();
        try {
            FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
            return format.format(annotationList.toArray(new BugAnnotation[0]), getPrimaryClass());
        } catch (RuntimeException e) {
            AnalysisContext.logError("Error generating bug msg ", e);
            return shortPattern + " [Error generating customized description]";
        }
    }

    String getLongDescription() {
        return getBugPattern().getLongDescription().replaceAll("BUG_PATTERN", type);
    }

    public String getAbridgedMessage() {
        BugPattern bugPattern = getBugPattern();

        String pattern = getLongDescription().replaceAll(" in \\{1\\}", "");
        String shortPattern = bugPattern.getShortDescription();

        try {
            FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
            return format.format(annotationList.toArray(new BugAnnotation[0]), getPrimaryClass(), true);
        } catch (RuntimeException e) {
            AnalysisContext.logError("Error generating bug msg ", e);
            return shortPattern + " [Error3 generating customized description]";
        }
    }

    /**
     * Format a string describing this bug instance.
     *
     * @return the description
     */
    public String getMessage() {
        BugPattern bugPattern = getBugPattern();
        String pattern = bugPattern.getAbbrev() + ": " + getLongDescription();
        FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
        try {
            return format.format(annotationList.toArray(new BugAnnotation[0]), getPrimaryClass());
        } catch (RuntimeException e) {
            AnalysisContext.logError("Error generating bug msg ", e);
            return bugPattern.getShortDescription() + " [Error generating customized description]";
        }
    }

    /**
     * Format a string describing this bug pattern, with the priority and type
     * at the beginning. e.g.
     * "(High Priority Correctness) Guaranteed null pointer dereference..."
     */
    public String getMessageWithPriorityType() {
        return "(" + this.getPriorityTypeString() + ") " + this.getMessage();
    }

    public String getMessageWithPriorityTypeAbbreviation() {
        return this.getPriorityTypeAbbreviation() + " " + this.getMessage();
    }

    /**
     * Add a description to the most recently added bug annotation.
     *
     * @param description
     *            the description to add
     * @return this object
     */
    @Nonnull
    public BugInstance describe(String description) {
        annotationList.get(annotationList.size() - 1).setDescription(description);
        return this;
    }

    /**
     * Convert to String. This method returns the "short" message describing the
     * bug, as opposed to the longer format returned by getMessage(). The short
     * format is appropriate for the tree view in a GUI, where the annotations
     * are listed separately as part of the overall bug instance.
     */
    @Override
    public String toString() {
        return I18N.instance().getShortMessage(type);
    }

    /*
     * ----------------------------------------------------------------------
     * XML Conversion support
     * ----------------------------------------------------------------------
     */

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, null, false);
    }

    public int getCWEid() {
        BugPattern pattern = getBugPattern();

        int cweid = pattern.getCWEid();
        if (cweid != 0) {
            return cweid;
        }
        BugCode bugCode = pattern.getBugCode();
        return bugCode.getCWEid();
    }

    public void writeXML(XMLOutput xmlOutput, BugCollection bugCollection, boolean addMessages) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("type", type).addAttribute("priority",
                String.valueOf(priority));

        // Always add the rank attribute.
        attributeList.addAttribute("rank", Integer.toString(getBugRank()));

        BugPattern pattern = getBugPattern();

        // The bug abbreviation and pattern category are
        // emitted into the XML for informational purposes only.
        // (The information is redundant, but might be useful
        // for processing tools that want to make sense of
        // bug instances without looking at the plugin descriptor.)
        attributeList.addAttribute("abbrev", pattern.getAbbrev());
        attributeList.addAttribute("category", pattern.getCategory());

        if (addMessages) {
            // Add a uid attribute, if we have a unique id.

            attributeList.addAttribute("instanceHash", getInstanceHash());
            attributeList.addAttribute("instanceOccurrenceNum", Integer.toString(getInstanceOccurrenceNum()));
            attributeList.addAttribute("instanceOccurrenceMax", Integer.toString(getInstanceOccurrenceMax()));

            int cweid = getCWEid();
            if (cweid != 0) {
                attributeList.addAttribute("cweid", Integer.toString(cweid));
            }
        } else if (oldInstanceHash != null && !isInstanceHashConsistent()) {
            attributeList.addAttribute("oldInstanceHash", oldInstanceHash);
        }
        if (firstVersion > 0) {
            attributeList.addAttribute("first", Long.toString(firstVersion));
        }
        if (lastVersion >= 0) {
            attributeList.addAttribute("last", Long.toString(lastVersion));
        }
        if (introducedByChangeOfExistingClass) {
            attributeList.addAttribute("introducedByChange", "true");
        }
        if (removedByChangeOfPersistingClass) {
            attributeList.addAttribute("removedByChange", "true");
        }

        xmlOutput.openTag(ELEMENT_NAME, attributeList);

        if (addMessages) {
            BugPattern bugPattern = getBugPattern();

            xmlOutput.openTag("ShortMessage");
            xmlOutput.writeText(bugPattern.getShortDescription());
            xmlOutput.closeTag("ShortMessage");

            xmlOutput.openTag("LongMessage");
            if (FindBugsDisplayFeatures.isAbridgedMessages()) {
                xmlOutput.writeText(this.getAbridgedMessage());
            } else {
                xmlOutput.writeText(this.getMessageWithoutPrefix());
            }
            xmlOutput.closeTag("LongMessage");
        }

        Map<BugAnnotation, Void> primaryAnnotations;

        if (addMessages) {
            primaryAnnotations = new IdentityHashMap<>();
            primaryAnnotations.put(getPrimarySourceLineAnnotation(), null);
            primaryAnnotations.put(getPrimaryClass(), null);
            primaryAnnotations.put(getPrimaryField(), null);
            primaryAnnotations.put(getPrimaryMethod(), null);
        } else {
            primaryAnnotations = Collections.<BugAnnotation, Void>emptyMap();
        }

        boolean foundSourceAnnotation = false;
        for (BugAnnotation annotation : annotationList) {
            if (annotation instanceof SourceLineAnnotation) {
                foundSourceAnnotation = true;
            }
            annotation.writeXML(xmlOutput, addMessages, primaryAnnotations.containsKey(annotation));
        }
        if (!foundSourceAnnotation && addMessages) {
            SourceLineAnnotation synth = getPrimarySourceLineAnnotation();
            synth.setSynthetic(true);
            synth.writeXML(xmlOutput, addMessages, false);
        }

        if (propertyListHead != null) {
            List<BugProperty> props = new ArrayList<>();
            for (BugProperty prop = propertyListHead; prop != null; prop = prop.getNext()) {
                props.add(prop);
            }
            Collections.sort(props, (o1, o2) -> o1.getName().compareTo(o2.getName()));
            for (BugProperty prop : props) {
                prop.writeXML(xmlOutput);
            }
        }

        xmlOutput.closeTag(ELEMENT_NAME);
    }

    private int ageInDays(BugCollection bugCollection, long firstSeen) {
        long age = bugCollection.getAnalysisTimestamp() - firstSeen;
        if (age < 0) {
            age = 0;
        }
        int ageInDays = (int) (age / 1000 / 3600 / 24);
        return ageInDays;
    }

    /*
     * ----------------------------------------------------------------------
     * Implementation
     * ----------------------------------------------------------------------
     */

    public BugInstance addOptionalAnnotation(@CheckForNull BugAnnotation annotation) {
        if (annotation == null) {
            return this;
        }
        return add(annotation);
    }

    public BugInstance addOptionalAnnotation(@CheckForNull BugAnnotation annotation, String role) {
        if (annotation == null) {
            return this;
        }
        return add(annotation).describe(role);
    }

    private void addJavaAnnotationNames(BugAnnotation annotation) {
        try {
            IAnalysisCache analysisCache = Global.getAnalysisCache();
            if (analysisCache == null) { // Note: IAnalysisCache is not available during testing or spotbugs:gui
                return;
            }
            PackageMemberAnnotation pma = (PackageMemberAnnotation) annotation;
            ClassDescriptor classDescriptor = pma.getClassDescriptor();
            ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, classDescriptor);
            JavaClass javaClass = classContext.getJavaClass();
            AnnotationEntry[] annotationEntries = javaClass.getAnnotationEntries();
            List<String> javaAnnotationNames = Arrays.asList(annotationEntries).stream().map((AnnotationEntry ae) -> {
                // map annotation entry type to dotted class name, for example
                // Lorg/immutables/value/Generated; -> org.immutables.value.Generated
                String annotationType = ae.getAnnotationType().substring(1).replace("/", ".").replace(";", "");
                return annotationType;
            }).collect(Collectors.toList());
            pma.setJavaAnnotationNames(javaAnnotationNames);
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    public BugInstance add(@Nonnull BugAnnotation annotation) {
        requireNonNull(annotation, "Missing BugAnnotation!");

        // The java annotations for the class were not stored before,
        // thus we add a post-hook to lookup the java annotations for
        // bug annotation types that have a class descriptor. Then
        // post-analysis matcher can easily filter based on the java
        // annotations (without having to look at the bytecode again).
        if (annotation instanceof PackageMemberAnnotation) {
            addJavaAnnotationNames(annotation);
        }

        // Add to list
        annotationList.add(annotation);

        // This object is being modified, so the cached hashcode
        // must be invalidated
        cachedHashCode = INVALID_HASH_CODE;
        return this;
    }

    public BugInstance addSomeSourceForTopTwoStackValues(ClassContext classContext, Method method, Location location) {
        int pc = location.getHandle().getPosition();
        try {
            OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method, pc);
            BugAnnotation a1 = getSomeSource(classContext, method, location, stack, 1);
            BugAnnotation a0 = getSomeSource(classContext, method, location, stack, 0);
            addOptionalUniqueAnnotations(a0, a1);
        } catch (UnreachableCodeException e) {
            if (SystemProperties.ASSERTIONS_ENABLED) {
                AnalysisContext.logError(e.getMessage(), e);
            }
            assert true;
        }
        return this;

    }

    public BugInstance addSourceForTopStackValue(ClassContext classContext, Method method, Location location) {
        BugAnnotation b = getSourceForTopStackValue(classContext, method, location);
        return this.addOptionalAnnotation(b);
    }

    public static @CheckForNull BugAnnotation getSourceForTopStackValue(ClassContext classContext, Method method, Location location) {
        return getSourceForStackValue(classContext, method, location, 0);
    }

    public static @CheckForNull BugAnnotation getSourceForStackValue(ClassContext classContext, Method method, Location location, int depth) {
        try {
            int pc = location.getHandle().getPosition();
            OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), method, pc);
            BugAnnotation a0 = getSomeSource(classContext, method, location, stack, depth);
            return a0;
        } catch (UnreachableCodeException e) {
            if (SystemProperties.ASSERTIONS_ENABLED) {
                AnalysisContext.logError(e.getMessage(), e);
            }
            return null;
        }
    }

    public static @CheckForNull BugAnnotation getSomeSource(ClassContext classContext, Method method, Location location, OpcodeStack stack,
            int stackPos) {
        if (stack.isTop()) {
            return null;
        }
        int pc = location.getHandle().getPosition();

        try {
            BugAnnotation result = ValueNumberSourceInfo.getFromValueNumber(classContext, method, location, stackPos);
            if (result != null) {
                return result;
            }
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("Couldn't find value source", e);
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("Couldn't find value source", e);
        }

        return getValueSource(stack.getStackItem(stackPos), method, pc);

    }

    public static @CheckForNull BugAnnotation getValueSource(OpcodeStack.Item item, Method method, int pc) {
        LocalVariableAnnotation lv = LocalVariableAnnotation.getLocalVariableAnnotation(method, item, pc);
        if (lv != null && lv.isNamed()) {
            return lv;
        }

        BugAnnotation a = getFieldOrMethodValueSource(item);
        if (a != null) {
            return a;
        }
        Object c = item.getConstant();
        if (c instanceof String) {
            a = new StringAnnotation((String) c);
            a.setDescription(StringAnnotation.STRING_CONSTANT_ROLE);
            return a;
        }
        if (c instanceof Integer && !item.isArray()) {
            a = new IntAnnotation((Integer) c);
            a.setDescription(IntAnnotation.INT_VALUE);
            return a;
        }
        return null;

    }

    public BugInstance addValueSource(@CheckForNull OpcodeStack.Item item, DismantleBytecode dbc) {
        if (item != null) {
            addValueSource(item, dbc.getMethod(), dbc.getPC());
        }
        return this;
    }

    public BugInstance addValueSource(OpcodeStack.Item item, Method method, int pc) {
        addOptionalAnnotation(getValueSource(item, method, pc));
        return this;
    }

    public BugInstance addFieldOrMethodValueSource(OpcodeStack.Item item) {
        addOptionalAnnotation(getFieldOrMethodValueSource(item));
        return this;
    }

    public BugInstance addOptionalUniqueAnnotations(BugAnnotation... annotations) {
        HashSet<BugAnnotation> added = new HashSet<>();
        for (BugAnnotation a : annotations) {
            if (a != null && added.add(a)) {
                add(a);
            }
        }
        return this;
    }

    public boolean tryAddingOptionalUniqueAnnotations(BugAnnotation... annotations) {
        HashSet<BugAnnotation> added = new HashSet<>();
        for (BugAnnotation a : annotations) {
            if (a != null && added.add(a)) {
                add(a);
            }
        }
        return !added.isEmpty();
    }

    public BugInstance addOptionalUniqueAnnotationsWithFallback(BugAnnotation fallback, BugAnnotation... annotations) {
        HashSet<BugAnnotation> added = new HashSet<>();
        for (BugAnnotation a : annotations) {
            if (a != null && added.add(a)) {
                add(a);
            }
        }
        if (added.isEmpty()) {
            add(fallback);
        }
        return this;
    }

    public static @CheckForNull BugAnnotation getFieldOrMethodValueSource(@CheckForNull OpcodeStack.Item item) {
        if (item == null) {
            return null;
        }
        XField xField = item.getXField();
        if (xField != null) {
            FieldAnnotation a = FieldAnnotation.fromXField(xField);
            a.setDescription(FieldAnnotation.LOADED_FROM_ROLE);
            return a;
        }

        XMethod xMethod = item.getReturnValueOf();
        if (xMethod != null) {
            MethodAnnotation a = MethodAnnotation.fromXMethod(xMethod);
            a.setDescription(MethodAnnotation.METHOD_RETURN_VALUE_OF);
            return a;
        }
        return null;

    }

    private void addSourceLinesForMethod(MethodAnnotation methodAnnotation, SourceLineAnnotation sourceLineAnnotation) {
        if (sourceLineAnnotation != null) {
            // Note: we don't add the source line annotation directly to
            // the bug instance. Instead, we stash it in the MethodAnnotation.
            // It is much more useful there, and it would just be distracting
            // if it were displayed in the UI, since it would compete for
            // attention
            // with the actual bug location source line annotation (which is
            // much
            // more important and interesting).
            methodAnnotation.setSourceLines(sourceLineAnnotation);
        }
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == INVALID_HASH_CODE) {
            int hashcode = type.hashCode() + priority;
            Iterator<BugAnnotation> i = annotationIterator();
            while (i.hasNext()) {
                hashcode += i.next().hashCode();
            }
            if (hashcode == INVALID_HASH_CODE) {
                hashcode = INVALID_HASH_CODE + 1;
            }
            cachedHashCode = hashcode;
        }

        return cachedHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BugInstance)) {
            return false;
        }
        BugInstance other = (BugInstance) o;
        if (!type.equals(other.type) || priority != other.priority) {
            return false;
        }
        if (annotationList.size() != other.annotationList.size()) {
            return false;
        }
        int numAnnotations = annotationList.size();
        for (int i = 0; i < numAnnotations; ++i) {
            BugAnnotation lhs = annotationList.get(i);
            BugAnnotation rhs = other.annotationList.get(i);
            if (!lhs.equals(rhs)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int compareTo(BugInstance other) {
        int cmp;
        cmp = type.compareTo(other.type);
        if (cmp != 0) {
            return cmp;
        }
        cmp = priority - other.priority;
        if (cmp != 0) {
            return cmp;
        }

        // Compare BugAnnotations lexicographically
        int pfxLen = Math.min(annotationList.size(), other.annotationList.size());
        for (int i = 0; i < pfxLen; ++i) {
            BugAnnotation lhs = annotationList.get(i);
            BugAnnotation rhs = other.annotationList.get(i);
            cmp = lhs.compareTo(rhs);
            if (cmp != 0) {
                return cmp;
            }
        }

        // All elements in prefix were the same,
        // so use number of elements to decide
        return annotationList.size() - other.annotationList.size();
    }

    public void setFirstVersion(long firstVersion) {
        if (lastVersion >= 0 && firstVersion > lastVersion) {
            throw new IllegalArgumentException(firstVersion + ".." + lastVersion);
        }
        this.firstVersion = firstVersion;
    }

    public void clearHistory() {
        setFirstVersion(0);
        setLastVersion(-1);
        setIntroducedByChangeOfExistingClass(false);
        setRemovedByChangeOfPersistingClass(false);
    }

    public long getFirstVersion() {
        return firstVersion;
    }

    public void setHistory(BugInstance from) {
        long first = from.getFirstVersion();
        long last = from.getLastVersion();
        if (first > 0 && last >= 0 && first > last) {

            throw new IllegalArgumentException("from has version range " + first + "..." + last + " in " + from.getBugPattern()
                    + "\n" + from.getMessage());
        }
        setFirstVersion(first);
        setLastVersion(last);
        this.removedByChangeOfPersistingClass = from.removedByChangeOfPersistingClass;
        this.introducedByChangeOfExistingClass = from.introducedByChangeOfExistingClass;
    }

    public void setLastVersion(long lastVersion) {
        if (lastVersion >= 0 && firstVersion > lastVersion) {
            throw new IllegalArgumentException(firstVersion + ".." + lastVersion);
        }
        this.lastVersion = lastVersion;
    }

    /** Mark the bug instance is being alive (still present in the last version) */
    public void setLive() {
        this.lastVersion = -1;
    }

    public long getLastVersion() {
        return lastVersion;
    }

    public boolean isDead() {
        return lastVersion != -1;
    }

    public void setIntroducedByChangeOfExistingClass(boolean introducedByChangeOfExistingClass) {
        this.introducedByChangeOfExistingClass = introducedByChangeOfExistingClass;
    }

    public boolean isIntroducedByChangeOfExistingClass() {
        return introducedByChangeOfExistingClass;
    }

    public void setRemovedByChangeOfPersistingClass(boolean removedByChangeOfPersistingClass) {
        this.removedByChangeOfPersistingClass = removedByChangeOfPersistingClass;
    }

    public boolean isRemovedByChangeOfPersistingClass() {
        return removedByChangeOfPersistingClass;
    }

    public void setInstanceHash(String instanceHash) {
        this.instanceHash = instanceHash;
    }

    public void setOldInstanceHash(String oldInstanceHash) {
        this.oldInstanceHash = oldInstanceHash;
    }

    public String getInstanceHash() {
        String hash = instanceHash;
        if (hash != null) {
            return hash;
        }

        MessageDigest digest = Util.getMD5Digest();
        String key = getInstanceKey();
        byte[] data;
        data = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        hash = new BigInteger(1, data).toString(16);
        instanceHash = hash;
        return hash;
    }

    public boolean isInstanceHashConsistent() {
        return oldInstanceHash == null || getInstanceHash().equals(oldInstanceHash);
    }

    public void setInstanceOccurrenceNum(int instanceOccurrenceNum) {
        this.instanceOccurrenceNum = instanceOccurrenceNum;
    }

    public int getInstanceOccurrenceNum() {
        return instanceOccurrenceNum;
    }

    public void setInstanceOccurrenceMax(int instanceOccurrenceMax) {
        this.instanceOccurrenceMax = instanceOccurrenceMax;
    }

    public int getInstanceOccurrenceMax() {
        return instanceOccurrenceMax;
    }

    @CheckForNull
    public DetectorFactory getDetectorFactory() {
        return detectorFactory;
    }

    private void optionalAdd(Collection<BugAnnotation> c, BugAnnotation a) {
        if (a != null) {
            c.add(a);
        }
    }

    public List<BugAnnotation> getAnnotationsForMessage(boolean showContext) {
        ArrayList<BugAnnotation> result = new ArrayList<>();

        HashSet<BugAnnotation> primaryAnnotations = new HashSet<>();

        // This ensures the order of the primary annotations of the bug
        FieldAnnotation primeField = getPrimaryField();
        MethodAnnotation primeMethod = getPrimaryMethod();
        ClassAnnotation primeClass = getPrimaryClass();

        SourceLineAnnotation primarySourceLineAnnotation = getPrimarySourceLineAnnotation();
        optionalAdd(primaryAnnotations, primarySourceLineAnnotation);
        optionalAdd(primaryAnnotations, primeMethod);
        optionalAdd(primaryAnnotations, primeField);
        optionalAdd(primaryAnnotations, primeClass);

        if ((showContext || !SourceLineAnnotation.DEFAULT_ROLE.equals(primarySourceLineAnnotation.getDescription()))) {
            result.add(primarySourceLineAnnotation);
        }

        if (primeMethod != null && (showContext || !MethodAnnotation.DEFAULT_ROLE.equals(primeMethod.getDescription()))) {
            result.add(primeMethod);
        }

        optionalAdd(result, primeField);

        String fieldClass = "";
        String methodClass = "";
        if (primeField != null) {
            fieldClass = primeField.getClassName();
        }
        if (primeMethod != null) {
            methodClass = primeMethod.getClassName();
        }
        if (showContext && (primaryAnnotations.size() < 2)
                || (!(primeClass.getClassName().equals(fieldClass) || primeClass.getClassName().equals(methodClass)))) {
            optionalAdd(result, primeClass);
        }

        for (BugAnnotation b : getAnnotations()) {
            if (primaryAnnotations.contains(b)) {
                continue;
            }
            if (b instanceof LocalVariableAnnotation && !((LocalVariableAnnotation) b).isNamed()) {
                continue;
            }
            if (b instanceof SourceLineAnnotation && ((SourceLineAnnotation) b).isUnknown()) {
                continue;
            }
            result.add(b);
        }
        return result;
    }
}
