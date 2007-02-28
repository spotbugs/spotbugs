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

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.bcp.FieldVariable;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * An instance of a bug pattern.
 * A BugInstance consists of several parts:
 * <p/>
 * <ul>
 * <li> the type, which is a string indicating what kind of bug it is;
 * used as a key for the FindBugsMessages resource bundle
 * <li> the priority; how likely this instance is to actually be a bug
 * <li> a list of <em>annotations</em>
 * </ul>
 * <p/>
 * The annotations describe classes, methods, fields, source locations,
 * and other relevant context information about the bug instance.
 * Every BugInstance must have at least one ClassAnnotation, which
 * describes the class in which the instance was found.  This is the
 * "primary class annotation".
 * <p/>
 * <p> BugInstance objects are built up by calling a string of <code>add</code>
 * methods.  (These methods all "return this", so they can be chained).
 * Some of the add methods are specialized to get information automatically from
 * a BetterVisitor or DismantleBytecode object.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class BugInstance implements Comparable<BugInstance>, XMLWriteableWithMessages, Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	
	private String type;
	private int priority;
	private ArrayList<BugAnnotation> annotationList;
	private int cachedHashCode;
	private @CheckForNull  BugDesignation userDesignation;
	private BugProperty propertyListHead, propertyListTail;
	private String uniqueId;
	private String oldInstanceHash;
	private String instanceHash;
	private int instanceOccurrenceNum;
	private int instanceOccurrenceMax;
	
	
	/*
	 * The following fields are used for tracking Bug instances across multiple versions of software.
	 * They are meaningless in a BugCollection for just one version of software. 
	 */
	private long firstVersion = 0;
	private long lastVersion = -1;
	private boolean introducedByChangeOfExistingClass;
	private boolean removedByChangeOfPersistingClass;
	
	/**
	 * This value is used to indicate that the cached hashcode
	 * is invalid, and should be recomputed.
	 */
	private static final int INVALID_HASH_CODE = 0;
	
	/**
	 * This value is used to indicate whether BugInstances should be reprioritized very low,
	 * when the BugPattern is marked as experimental
	 */
	private static boolean adjustExperimental = false;
	
	/**
	 * Constructor.
	 *
	 * @param type     the bug type
	 * @param priority the bug priority
	 */
	public BugInstance(String type, int priority) {
		this.type = type;
		this.priority = priority < Detector.HIGH_PRIORITY 
			? Detector.HIGH_PRIORITY : priority;
		annotationList = new ArrayList<BugAnnotation>(4);
		cachedHashCode = INVALID_HASH_CODE;
		
		if (adjustExperimental && isExperimental())
			this.priority = Detector.EXP_PRIORITY;
	}
	
	//@Override
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
			for (Iterator<BugProperty> i = propertyIterator(); i.hasNext(); ) {
				dup.addProperty((BugProperty) i.next().clone());
			}

			return dup;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Create a new BugInstance.
	 * This is the constructor that should be used by Detectors.
	 * 
	 * @param detector the Detector that is reporting the BugInstance
	 * @param type     the bug type
	 * @param priority the bug priority
	 */
	public BugInstance(Detector detector, String type, int priority) {
		this(type, priority);
		
		if (detector != null) {
			// Adjust priority if required
			DetectorFactory factory =
				DetectorFactoryCollection.instance().getFactoryByClassName(detector.getClass().getName());
			if (factory != null) {
				this.priority += factory.getPriorityAdjustment();
				if (this.priority < 0)
					this.priority = 0;
			}
		}
		
		if (adjustExperimental && isExperimental())
			this.priority = Detector.EXP_PRIORITY;
	}
		
	public static void setAdjustExperimental(boolean adjust) {
		adjustExperimental = adjust;
	}
	
	/* ----------------------------------------------------------------------
	 * Accessors
	 * ---------------------------------------------------------------------- */

	/**
	 * Get the bug type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the BugPattern.
	 */
	public BugPattern getBugPattern() {
		return I18N.instance().lookupBugPattern(getType());
	}

	/**
	 * Get the bug priority.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Get a string describing the bug priority and type.
	 * e.g. "High Priority Correctness"
	 * @return a string describing the bug priority and type
	 */
	public String getPriorityTypeString()
	{
		String priorityString = getPriorityString();
		//then get the category and put everything together
		String categoryString = I18N.instance().getBugCategoryDescription(this.getBugPattern().getCategory());
		return priorityString + " Priority " + categoryString;
		//TODO: internationalize the word "Priority"
	}

    public String getPriorityTypeAbbreviation()
    {
        String priorityString = getPriorityAbbreviation();
         return priorityString + " " + getBugPattern().getCategoryAbbrev();
       
    }

    public String getPriorityString() {
        //first, get the priority
		int value = this.getPriority();
		String priorityString;
		if (value == Detector.HIGH_PRIORITY)
			priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_high", "High");
		else if (value == Detector.NORMAL_PRIORITY)
			priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_normal", "Normal");
		else if (value == Detector.LOW_PRIORITY)
			priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_low", "Low");
		else if (value == Detector.EXP_PRIORITY)
			priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_experimental", "Experimental");
		else
			priorityString = edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_ignore", "Ignore"); // This probably shouldn't ever happen, but what the hell, let's be complete
        return priorityString;
    }
	
    public String getPriorityAbbreviation() {
        return getPriorityString().substring(0,1);
    }
	/**
	 * Set the bug priority.
	 */
	public void setPriority(int p) {
		priority = Math.max(Detector.HIGH_PRIORITY, Math.min(Detector.IGNORE_PRIORITY, p));
	}
    public void raisePriority() {
        priority = Math.max(Detector.HIGH_PRIORITY, Math.min(Detector.IGNORE_PRIORITY, priority-1));
    }
    public void lowerPriority() {
        priority = Math.max(Detector.HIGH_PRIORITY, Math.min(Detector.IGNORE_PRIORITY, priority+1));
    }

    public void lowerPriorityALot() {
        priority = Math.max(Detector.HIGH_PRIORITY, Math.min(Detector.IGNORE_PRIORITY, priority+2));
    }

	/**
	 * Is this bug instance the result of an experimental detector?
	 */
	public boolean isExperimental() {
		BugPattern pattern = I18N.instance().lookupBugPattern(type);
		return (pattern != null) && pattern.isExperimental();
	}

	/**
	 * Get the primary class annotation, which indicates where the bug occurs.
	 */
	public ClassAnnotation getPrimaryClass() {
		return (ClassAnnotation) findAnnotationOfType(ClassAnnotation.class);
	}

	/**
	 * Get the primary method annotation, which indicates where the bug occurs.
	 */
	public MethodAnnotation getPrimaryMethod() {
		return (MethodAnnotation) findAnnotationOfType(MethodAnnotation.class);
	}
	/**
	 * Get the primary method annotation, which indicates where the bug occurs.
	 */
	public FieldAnnotation getPrimaryField() {
		return (FieldAnnotation) findAnnotationOfType(FieldAnnotation.class);
	}

	
	public BugInstance lowerPriorityIfDeprecated() {
		MethodAnnotation m = getPrimaryMethod();
		if (m != null && AnalysisContext.currentXFactory().getDeprecated().contains(XFactory.createXMethod(m)))
				priority++;
		FieldAnnotation f = getPrimaryField();
		if (f != null && AnalysisContext.currentXFactory().getDeprecated().contains(XFactory.createXField(f)))
			priority++;
		return this;
	}
	/**
	 * Find the first BugAnnotation in the list of annotations
	 * that is the same type or a subtype as the given Class parameter.
	 * 
	 * @param cls the Class parameter
	 * @return the first matching BugAnnotation of the given type,
	 *         or null if there is no such BugAnnotation
	 */
	private BugAnnotation findAnnotationOfType(Class<? extends BugAnnotation> cls) {
		for (Iterator<BugAnnotation> i = annotationIterator(); i.hasNext();) {
			BugAnnotation annotation = i.next();
			if (cls.isAssignableFrom(annotation.getClass()))
				return annotation;
		}
		return null;
	}
	
	public LocalVariableAnnotation getPrimaryLocalVariableAnnotation() {
		for (BugAnnotation annotation : annotationList) 
			if (annotation instanceof LocalVariableAnnotation)
				return (LocalVariableAnnotation) annotation;
		return null;
	}
	/**
	 * Get the primary source line annotation.
	 * There is guaranteed to be one (unless some Detector constructed
	 * an invalid BugInstance).
	 *
	 * @return the source line annotation
	 */
	public SourceLineAnnotation getPrimarySourceLineAnnotation() {
		// Highest priority: return the first top level source line annotation
		for (BugAnnotation annotation : annotationList) {
			if (annotation instanceof SourceLineAnnotation)
				return (SourceLineAnnotation) annotation;
		}
		
		// Next: Try primary method, primary field, primary class
		SourceLineAnnotation srcLine;
		if ((srcLine = inspectPackageMemberSourceLines(getPrimaryMethod())) != null)
			return srcLine;
		if ((srcLine = inspectPackageMemberSourceLines(getPrimaryField())) != null)
			return srcLine;
		if ((srcLine = inspectPackageMemberSourceLines(getPrimaryClass())) != null)
			return srcLine;
		
		// Last resort: throw exception
		throw new IllegalStateException("BugInstance must contain at least one class, method, or field annotation");
	}

	public String getInstanceKey() {
		StringBuffer buf = new StringBuffer(type);
		for (BugAnnotation annotation : annotationList) {
			if (annotation instanceof SourceLineAnnotation) {
				// do nothing
			} else {
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
	 * Get the abbreviation of this bug instance's BugPattern.
	 * This is the same abbreviation used by the BugCode which
	 * the BugPattern is a particular species of.
	 */
	public String getAbbrev() {
		BugPattern pattern = I18N.instance().lookupBugPattern(getType());
		return pattern != null ? pattern.getAbbrev() : "<unknown bug pattern>";
	}

	/** set the user designation object. This will clobber any
	 *  existing annotationText (or any other BugDesignation field). */
	public void setUserDesignation(BugDesignation bd) {
		userDesignation = bd;
	}
	/** return the user designation object, which may be null.
	 * 
	 *  A previous calls to getSafeUserDesignation(), setAnnotationText(),
	 *  or setUserDesignation() will ensure it will be non-null
	 *  [barring an intervening setUserDesignation(null)].
	 *  @see #getNonnullUserDesignation() */
	@Nullable public BugDesignation getUserDesignation() {
		return userDesignation;
	}
	/** return the user designation object, creating one if
	 *  necessary. So calling
	 *  <code>getSafeUserDesignation().setDesignation("HARMLESS")</code>
	 *  will always work without the possibility of a NullPointerException.
	 *  @see #getUserDesignation() */
	@NonNull public BugDesignation getNonnullUserDesignation() {
		if (userDesignation == null)
			userDesignation = new BugDesignation();
		return userDesignation;
	}
	

	/** Get the user designation key.
	 *  E.g., "MOSTLY_HARMLESS", "CRITICAL", "NOT_A_BUG", etc.
	 *
	 *  If the user designation object is null,returns UNCLASSIFIED.
	 *
	 *  To set the user designation key, call
	 *  <code>getSafeUserDesignation().setDesignation("HARMLESS")</code>.
	 * 
	 *  @see I18N#getUserDesignation(String key)
	 *  @return the user designation key
	 */
	@NonNull public String getUserDesignationKey() {
		BugDesignation userDesignation = this.userDesignation;
		if (userDesignation == null) return BugDesignation.UNCLASSIFIED;
		return userDesignation.getDesignationKey();
	}

	/**
	 * Set the user annotation text.
	 *
	 * @param annotationText the user annotation text
	 */
	public void setAnnotationText(String annotationText) {
		getNonnullUserDesignation().setAnnotationText(annotationText);
	}

	/**
	 * Get the user annotation text.
	 *
	 * @return the user annotation text
	 */
	@NonNull public String getAnnotationText() {
		BugDesignation userDesignation = this.userDesignation;
		if (userDesignation == null) return "";		
		String s = userDesignation.getAnnotationText();
		if (s == null) return "";
		return s;
	}

	/**
	 * Determine whether or not the annotation text contains
	 * the given word.
	 *
	 * @param word the word
	 * @return true if the annotation text contains the word, false otherwise
	 */
	public boolean annotationTextContainsWord(String word) {
		return getTextAnnotationWords().contains(word);
	}

	/**
	 * Get set of words in the text annotation.
	 */
	public Set<String> getTextAnnotationWords() {
		HashSet<String> result = new HashSet<String>();

		StringTokenizer tok = new StringTokenizer(getAnnotationText(), " \t\r\n\f.,:;-");
		while (tok.hasMoreTokens()) {
			result.add(tok.nextToken());
		}
		return result;
	}
	
	/**
	 * Get the BugInstance's unique id.
	 * 
	 * @return the unique id, or null if no unique id has been assigned
	 * 
	 * Deprecated, since it isn't persistent
	 */
	@Deprecated
	public String getUniqueId() {
		return uniqueId;
	}
	
	/**
	 * Set the unique id of the BugInstance.
	 * 
	 * @param uniqueId the unique id
	 * 
	 *   * Deprecated, since it isn't persistent
	 */
	@Deprecated
	 void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	
	
	/* ----------------------------------------------------------------------
	 * Property accessors
	 * ---------------------------------------------------------------------- */
	
	private class BugPropertyIterator implements Iterator<BugProperty> {
		private BugProperty prev, cur;
		private boolean removed;
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return findNext() != null;
		}
		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public BugProperty next() {
			BugProperty next = findNext();
			if (next == null)
				throw new NoSuchElementException();
			prev = cur;
			cur = next;
			removed = false;
			return cur;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			if (cur == null || removed)
				throw new IllegalStateException();
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
	 * @param name name of the property to get
	 * @return the value of the named property, or null if
	 *         the property has not been set
	 */
	public String getProperty(String name) {
		BugProperty prop = lookupProperty(name);
		return prop != null ? prop.getValue() : null;
	}
	
	/**
	 * Get value of given property, returning given default
	 * value if the property has not been set.
	 * 
	 * @param name         name of the property to get
	 * @param defaultValue default value to return if propery is not set
	 * @return the value of the named property, or the default
	 *         value if the property has not been set
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
	 * @param name  name of the property to set
	 * @param value the value of the property
	 * @return this object, so calls can be chained
	 */
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
	 * @param name name of the property to look for
	 * @return the BugProperty with the given name,
	 *         or null if the property has not been set
	 */
	public BugProperty lookupProperty(String name) {
		BugProperty prop = propertyListHead;
		
		while (prop != null) {
			if (prop.getName().equals(name))
				break;
			prop = prop.getNext();
		}
		
		return prop;
	}
	
	/**
	 * Delete property with given name.
	 * 
	 * @param name name of the property to delete
	 * @return true if a property with that name was deleted,
	 *         or false if there is no such property
	 */
	public boolean deleteProperty(String name) {
		BugProperty prev = null;
		BugProperty prop = propertyListHead;
		
		while (prop != null) {
			if (prop.getName().equals(name))
				break;
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
	
	/* ----------------------------------------------------------------------
	 * Generic BugAnnotation adders
	 * ---------------------------------------------------------------------- */
	
	/**
	 * Add a Collection of BugAnnotations.
	 * 
	 * @param annotationCollection Collection of BugAnnotations
	 */
	public BugInstance addAnnotations(Collection<? extends BugAnnotation> annotationCollection) {
		for (BugAnnotation annotation : annotationCollection) {
			add(annotation);
		}
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Combined annotation adders
	 * ---------------------------------------------------------------------- */
	
	public BugInstance addClassAndMethod(MethodDescriptor methodDescriptor) {
		addClass(methodDescriptor.getClassName());
		add(MethodAnnotation.fromMethodDescriptor(methodDescriptor));
		return this;
	}

	/**
	 * Add a class annotation and a method annotation for the class and method
	 * which the given visitor is currently visiting.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addClassAndMethod(PreorderVisitor visitor) {
		addClass(visitor);
		addMethod(visitor);
		return this;
	}

	/**
	 * Add class and method annotations for given method.
	 *
	 * @param methodAnnotation  the method
	 * @return this object
	 */
	public BugInstance addClassAndMethod(MethodAnnotation methodAnnotation) {
		addClass(methodAnnotation.getClassName());
		addMethod(methodAnnotation);
		return this;
	}

	/**
	 * Add class and method annotations for given method.
	 *
	 * @param methodGen  the method
	 * @param sourceFile source file the method is defined in
	 * @return this object
	 */
	public BugInstance addClassAndMethod(MethodGen methodGen, String sourceFile) {
		addClass(methodGen.getClassName());
		addMethod(methodGen, sourceFile);
		return this;
	}
	
	
	/**
	 * Add class and method annotations for given class and method.
	 *  
	 * @param javaClass the class
	 * @param method    the method
	 * @return this object
	 */
	public BugInstance addClassAndMethod(JavaClass javaClass, Method method) {
		addClass(javaClass.getClassName());
		addMethod(javaClass, method);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Class annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 *
	 * @param className the name of the class
	 * @param sourceFileName the source file of the class
	 * @return this object
	 * @deprecated use addClass(String) instead
	 */
	public BugInstance addClass(String className, String sourceFileName) {
		ClassAnnotation classAnnotation = new ClassAnnotation(className);
		add(classAnnotation);
		return this;
	}

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 *
	 * @param className the name of the class
	 * @return this object
	 */
	public BugInstance addClass(String className) {
		className = ClassName.toDottedClassName(className);
		ClassAnnotation classAnnotation = new ClassAnnotation(className);
		add(classAnnotation);
		return this;
	}

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 * 
	 * @param classDescriptor the class to add
	 * @return this object
	 */
	public BugInstance addClass(ClassDescriptor classDescriptor) {
		add(ClassAnnotation.fromClassDescriptor(classDescriptor));
		return this;
	}

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 *
	 * @param jclass the JavaClass object for the class
	 * @return this object
	 */
	public BugInstance addClass(JavaClass jclass) {
		addClass(jclass.getClassName());
		return this;
	}

	/**
	 * Add a class annotation for the class that the visitor is currently visiting.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addClass(PreorderVisitor visitor) {
		String className = visitor.getDottedClassName();
		addClass(className);
		return this;
	}

	/**
	 * Add a class annotation for the superclass of the class the visitor
	 * is currently visiting.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addSuperclass(PreorderVisitor visitor) {
		String className = visitor.getSuperclassName();
		addClass(className);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Type annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a type annotation. Handy for referring to array types.
	 *
	 * <p>For information on type descriptors,
	 * <br>see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#14152
	 * <br>or  http://www.murrayc.com/learning/java/java_classfileformat.shtml#TypeDescriptors
	 * 
	 * @param typeDescriptor a jvm type descriptor, such as "[I"
	 * @return this object
	 */
	public BugInstance addType(String typeDescriptor) {
		TypeAnnotation typeAnnotation = new TypeAnnotation(typeDescriptor);
		add(typeAnnotation);
		return this;
	}
	
    public BugInstance addFoundAndExpectedType(String foundType, String expectedType) {
        add( new TypeAnnotation(foundType)).describe(TypeAnnotation.FOUND_ROLE);
        add( new TypeAnnotation(expectedType)).describe(TypeAnnotation.EXPECTED_ROLE);
        return this;
    }

	public BugInstance addTypeOfNamedClass(String typeName) {
		TypeAnnotation typeAnnotation = new TypeAnnotation("L" + typeName.replace('.','/')+";");
		add(typeAnnotation);
		return this;
	}
	/* ----------------------------------------------------------------------
	 * Field annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a field annotation.
	 *
	 * @param className name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig  type signature of the field
	 * @param isStatic  whether or not the field is static
	 * @return this object
	 */
	public BugInstance addField(String className, String fieldName, String fieldSig, boolean isStatic) {
		addField(new FieldAnnotation(className, fieldName, fieldSig, isStatic));
		return this;
	}

	/**
	 * Add a field annotation
	 *
	 * @param fieldAnnotation the field annotation
	 * @return this object
	 */
	public BugInstance addField(FieldAnnotation fieldAnnotation) {
		add(fieldAnnotation);
		return this;
	}

	/**
	 * Add a field annotation for a FieldVariable matched in a ByteCodePattern.
	 *
	 * @param field the FieldVariable
	 * @return this object
	 */
	public BugInstance addField(FieldVariable field) {
		return addField(field.getClassName(), field.getFieldName(), field.getFieldSig(), field.isStatic());
	}

	/**
	 * Add a field annotation for an XField.
	 *
	 * @param xfield the XField
	 * @return this object
	 */
	public BugInstance addField(XField xfield) {
		return addField(xfield.getClassName(), xfield.getName(), xfield.getSignature(), xfield.isStatic());
	}
	
	/**
	 * Add a field annotation for a FieldDescriptor.
	 * 
	 * @param fieldDescriptor the FieldDescriptor
	 * @return this object
	 */
	public BugInstance addField(FieldDescriptor fieldDescriptor) {
		FieldAnnotation fieldAnnotation = FieldAnnotation.fromFieldDescriptor(fieldDescriptor);
		add(fieldAnnotation);
		return this;
	}

	/**
	 * Add a field annotation for the field which has just been accessed
	 * by the method currently being visited by given visitor.
	 * Assumes that a getfield/putfield or getstatic/putstatic
	 * has just been seen.
	 *
	 * @param visitor the DismantleBytecode object
	 * @return this object
	 */
	public BugInstance addReferencedField(DismantleBytecode visitor) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(visitor);
		addField(f);
		return this;
	}

	/**
	 * Add a field annotation for the field referenced by the FieldAnnotation parameter
	 */
	public BugInstance addReferencedField(FieldAnnotation fa) {
		addField(fa);
		return this;
	}

	/**
	 * Add a field annotation for the field which is being visited by
	 * given visitor.
	 *
	 * @param visitor the visitor
	 * @return this object
	 */
	public BugInstance addVisitedField(PreorderVisitor visitor) {
		FieldAnnotation f = FieldAnnotation.fromVisitedField(visitor);
		addField(f);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Method annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 *
	 * @param className  name of the class containing the method
	 * @param methodName name of the method
	 * @param methodSig  type signature of the method
	 * @param isStatic   true if the method is static, false otherwise
	 * @return this object
	 */
	public BugInstance addMethod(String className, String methodName, String methodSig, boolean isStatic) {
		addMethod(MethodAnnotation.fromForeignMethod(className, methodName, methodSig, isStatic));
		return this;
	}

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 *
	 * @param methodGen  the MethodGen object for the method
	 * @param sourceFile source file method is defined in
	 * @return this object
	 */
	public BugInstance addMethod(MethodGen methodGen, String sourceFile) {
		String className = methodGen.getClassName();
		MethodAnnotation methodAnnotation =
		        new MethodAnnotation(className, methodGen.getName(), methodGen.getSignature(), methodGen.isStatic());
		addMethod(methodAnnotation);
		addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(methodGen, sourceFile));
		return this;
	}
	
	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 *
	 * @param javaClass the class the method is defined in
	 * @param method    the method
	 * @return this object
	 */
	public BugInstance addMethod(JavaClass javaClass, Method method) {
		MethodAnnotation methodAnnotation =
			new MethodAnnotation(javaClass.getClassName(), method.getName(), method.getSignature(), method.isStatic());
		SourceLineAnnotation methodSourceLines = SourceLineAnnotation.forEntireMethod(
				javaClass,
				method);
		methodAnnotation.setSourceLines(methodSourceLines);
		addMethod(methodAnnotation);
		return this;
	}
	
	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 *
	 * @param classAndMethod JavaClassAndMethod identifying the method to add
	 * @return this object
	 */
	public BugInstance addMethod(JavaClassAndMethod classAndMethod) {
		return addMethod(classAndMethod.getJavaClass(), classAndMethod.getMethod());
	}

	/**
	 * Add a method annotation for the method which the given visitor is currently visiting.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addMethod(PreorderVisitor visitor) {
		MethodAnnotation methodAnnotation = MethodAnnotation.fromVisitedMethod(visitor);
		addMethod(methodAnnotation);
		addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(visitor));
		return this;
	}

	/**
	 * Add a method annotation for the method which has been called
	 * by the method currently being visited by given visitor.
	 * Assumes that the visitor has just looked at an invoke instruction
	 * of some kind.
	 *
	 * @param visitor the DismantleBytecode object
	 * @return this object
	 */
	public BugInstance addCalledMethod(DismantleBytecode visitor) {
		return addMethod(MethodAnnotation.fromCalledMethod(visitor)).describe("METHOD_CALLED");
	}

	/**
	 * Add a method annotation.
	 *
	 * @param className  name of class containing called method
	 * @param methodName name of called method
	 * @param methodSig  signature of called method
	 * @param isStatic   true if called method is static, false if not
	 * @return this object
	 */
	public BugInstance addCalledMethod(String className, String methodName, String methodSig, boolean isStatic) {
		return addMethod(MethodAnnotation.fromCalledMethod(className, methodName, methodSig, isStatic)).describe("METHOD_CALLED");
	}

	/**
	 * Add a method annotation for the method which is called by given
	 * instruction.
	 *
	 * @param methodGen the method containing the call
	 * @param inv       the InvokeInstruction
	 * @return this object
	 */
	public BugInstance addCalledMethod(MethodGen methodGen, InvokeInstruction inv) {
		ConstantPoolGen cpg = methodGen.getConstantPool();
		String className = inv.getClassName(cpg);
		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);
		addMethod(className, methodName, methodSig, inv.getOpcode() == Constants.INVOKESTATIC);
		describe("METHOD_CALLED");
		return this;
	}
	
	/**
	 * Add a MethodAnnotation from an XMethod.
	 * 
	 * @param xmethod the XMethod
	 * @return this object
	 */
	public BugInstance addMethod(XMethod xmethod) {
		addMethod(MethodAnnotation.fromXMethod(xmethod));
		return this;
	}

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 *
	 * @param methodAnnotation the method annotation
	 * @return this object
	 */
	public BugInstance addMethod(MethodAnnotation methodAnnotation) {
		add(methodAnnotation);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Integer annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add an integer annotation.
	 *
	 * @param value the integer value
	 * @return this object
	 */
	public BugInstance addInt(int value) {
		add(new IntAnnotation(value));
		return this;
	}

	/**
	 * Add a String annotation.
	 *
	 * @param value the String value
	 * @return this object
	 */
	public BugInstance addString(String value) {
		add(new StringAnnotation(value));
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Source line annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a source line annotation.
	 *
	 * @param sourceLine the source line annotation
	 * @return this object
	 */
	public BugInstance addSourceLine(SourceLineAnnotation sourceLine) {
		add(sourceLine);
		return this;
	}

	/**
	 * Add a source line annotation for instruction whose PC is given
	 * in the method that the given visitor is currently visiting.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param visitor a BytecodeScanningDetector that is currently visiting the method
	 * @param pc      bytecode offset of the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(BytecodeScanningDetector visitor, int pc) {
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstruction(visitor.getClassContext(), visitor, pc);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation for instruction whose PC is given
	 * in the method that the given visitor is currently visiting.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param classContext the ClassContext
	 * @param visitor a PreorderVisitor that is currently visiting the method
	 * @param pc      bytecode offset of the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(ClassContext classContext, PreorderVisitor visitor, int pc) {
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstruction(classContext, visitor, pc);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation for the given instruction in the given method.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param classContext the ClassContext
	 * @param methodGen  the method being visited
	 * @param sourceFile source file the method is defined in
	 * @param handle     the InstructionHandle containing the visited instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(ClassContext classContext, MethodGen methodGen, String sourceFile, @NonNull InstructionHandle handle) {
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, handle);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation describing a range of instructions.
	 *
	 * @param classContext the ClassContext
	 * @param methodGen  the method
	 * @param sourceFile source file the method is defined in
	 * @param start      the start instruction in the range
	 * @param end        the end instruction in the range (inclusive)
	 * @return this object
	 */
	public BugInstance addSourceLine(ClassContext classContext, MethodGen methodGen, String sourceFile, InstructionHandle start, InstructionHandle end) {
		// Make sure start and end are really in the right order.
		if (start.getPosition() > end.getPosition()) {
			InstructionHandle tmp = start;
			start = end;
			end = tmp;
		}
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstructionRange(classContext, methodGen, sourceFile, start, end);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add source line annotation for given Location in a method. 
	 * 
	 * @param classContext the ClassContext
	 * @param method       the Method
	 * @param location     the Location in the method
	 * @return this BugInstance
	 */
	public BugInstance addSourceLine(ClassContext classContext, Method method, Location location) {
		MethodGen methodGen = classContext.getMethodGen(method);
		return addSourceLine(
				classContext,
				methodGen,
				classContext.getJavaClass().getSourceFileName(),
				location.getHandle());
	}

	/**
	 * Add a source line annotation describing the
	 * source line numbers for a range of instructions in the method being
	 * visited by the given visitor.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param startPC the bytecode offset of the start instruction in the range
	 * @param endPC   the bytecode offset of the end instruction in the range
	 * @return this object
	 */
	public BugInstance addSourceLineRange(BytecodeScanningDetector visitor, int startPC, int endPC) {
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstructionRange(visitor.getClassContext(), visitor, startPC, endPC);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation describing the
	 * source line numbers for a range of instructions in the method being
	 * visited by the given visitor.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param classContext the ClassContext
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param startPC the bytecode offset of the start instruction in the range
	 * @param endPC   the bytecode offset of the end instruction in the range
	 * @return this object
	 */
	public BugInstance addSourceLineRange(ClassContext classContext, PreorderVisitor visitor, int startPC, int endPC) {
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstructionRange(classContext, visitor, startPC, endPC);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation for instruction currently being visited
	 * by given visitor.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param visitor a BytecodeScanningDetector visitor that is currently visiting the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(BytecodeScanningDetector visitor) {
		SourceLineAnnotation sourceLineAnnotation =
			SourceLineAnnotation.fromVisitedInstruction(visitor);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a non-specific source line annotation.
	 * This will result in the entire source file being displayed.
	 *
	 * @param className  the class name
	 * @param sourceFile the source file name
	 * @return this object
	 */
	public BugInstance addUnknownSourceLine(String className, String sourceFile) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.createUnknown(className, sourceFile);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Formatting support
	 * ---------------------------------------------------------------------- */

	/**
	 * Format a string describing this bug instance.
	 *
	 * @return the description
	 */
	public String getMessageWithoutPrefix() {
		BugPattern bugPattern = I18N.instance().lookupBugPattern(type);
		String pattern, shortPattern;
		if (bugPattern == null) 
			shortPattern = pattern = "Error: missing bug pattern for key " + type;
		else {
			pattern = bugPattern.getLongDescription();
			shortPattern = bugPattern.getShortDescription();
		}
		try {
			FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
			return format.format(annotationList.toArray(new BugAnnotation[annotationList.size()]), getPrimaryClass());
		} catch (RuntimeException e) {
			AnalysisContext.logError("Error generating bug msg ", e);
			return shortPattern + " [Error generating customized description]";
		}
	}
	/**
	 * Format a string describing this bug instance.
	 *
	 * @return the description
	 */
	public String getMessage() {
		String pattern = I18N.instance().getMessage(type);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		try {
			return format.format(annotationList.toArray(new BugAnnotation[annotationList.size()]), getPrimaryClass());
		} catch (RuntimeException e) {
			AnalysisContext.logError("Error generating bug msg ", e);
			BugPattern bugPattern = I18N.instance().lookupBugPattern(type);
			if (bugPattern == null)
				return "Error: missing bug pattern for key " + type;
			return bugPattern.getShortDescription() + " [Error generating customized description]";
		}
	}
	
	/**
	 * Format a string describing this bug pattern, with the priority and type at the beginning.
	 * e.g. "(High Priority Correctness) Guaranteed null pointer dereference..."
	 */
	public String getMessageWithPriorityType() {
		return "(" + this.getPriorityTypeString() + ") " + this.getMessage();
	}

    public String getMessageWithPriorityTypeAbbreviation() {
        return this.getPriorityTypeAbbreviation() + " "+ this.getMessage();
    }

	/**
	 * Add a description to the most recently added bug annotation.
	 *
	 * @param description the description to add
	 * @return this object
	 */
	public BugInstance describe(String description) {
		annotationList.get(annotationList.size() - 1).setDescription(description);
		return this;
	}

	/**
	 * Convert to String.
	 * This method returns the "short" message describing the bug,
	 * as opposed to the longer format returned by getMessage().
	 * The short format is appropriate for the tree view in a GUI,
	 * where the annotations are listed separately as part of the overall
	 * bug instance.
	 */
	@Override
	public String toString() {
		return I18N.instance().getShortMessage(type);
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		writeXML(xmlOutput, false);
	}
	

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("type", type)
			.addAttribute("priority", String.valueOf(priority));

		BugPattern pattern = getBugPattern();
		if (pattern != null) {
			// The bug abbreviation and pattern category are
			// emitted into the XML for informational purposes only.
			// (The information is redundant, but might be useful
			// for processing tools that want to make sense of
			// bug instances without looking at the plugin descriptor.)
			attributeList.addAttribute("abbrev", pattern.getAbbrev());
			attributeList.addAttribute("category", pattern.getCategory());
		}
		
		
		if (addMessages) {
			//		Add a uid attribute, if we have a unique id.
			if (getUniqueId() != null) {
				attributeList.addAttribute("uid", getUniqueId());
			}
			attributeList.addAttribute("instanceHash", getInstanceHash());
			attributeList.addAttribute("instanceOccurrenceNum", Integer.toString(getInstanceOccurrenceNum()));
			attributeList.addAttribute("instanceOccurrenceMax", Integer.toString(getInstanceOccurrenceMax()));
		
		}
		if (firstVersion > 0) attributeList.addAttribute("first", Long.toString(firstVersion));
		if (lastVersion >= 0) 	attributeList.addAttribute("last", Long.toString(lastVersion));
		if (introducedByChangeOfExistingClass) 
			attributeList.addAttribute("introducedByChange", "true");
		if (removedByChangeOfPersistingClass) 
			attributeList.addAttribute("removedByChange", "true");

		xmlOutput.openTag(ELEMENT_NAME, attributeList);

		if (userDesignation != null) {
			userDesignation.writeXML(xmlOutput);
		}

		if (addMessages) {
			BugPattern bugPattern = getBugPattern();
			
			xmlOutput.openTag("ShortMessage");
			xmlOutput.writeText(bugPattern != null ? bugPattern.getShortDescription() : this.toString());
			xmlOutput.closeTag("ShortMessage");
			
			xmlOutput.openTag("LongMessage");
			xmlOutput.writeText(this.getMessageWithoutPrefix());
			xmlOutput.closeTag("LongMessage");
		}

		boolean foundSourceAnnotation = false;
		for (BugAnnotation annotation : annotationList) {
			if (annotation instanceof SourceLineAnnotation) 
				foundSourceAnnotation = true;
			annotation.writeXML(xmlOutput, addMessages);
		}
		if (!foundSourceAnnotation && addMessages) {
			SourceLineAnnotation synth = getPrimarySourceLineAnnotation();
			if (synth != null) {
				synth.setSynthetic(true);
				synth.writeXML(xmlOutput, addMessages);
			}
		}
		
		if (propertyListHead != null) {
			BugProperty prop = propertyListHead;
			while (prop != null) {
				prop.writeXML(xmlOutput);
				prop = prop.getNext();
			}
		}

		xmlOutput.closeTag(ELEMENT_NAME);
	}

	private static final String ELEMENT_NAME = "BugInstance";
	private static final String USER_ANNOTATION_ELEMENT_NAME = "UserAnnotation";

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

    public BugInstance addOptionalAnnotation(@CheckForNull BugAnnotation annotation) {
        if (annotation == null) return this;
        return add(annotation);
    }
	public BugInstance add(BugAnnotation annotation) {
		if (annotation == null)
			throw new IllegalStateException("Missing BugAnnotation!");

		// Add to list
		annotationList.add(annotation);

		// This object is being modified, so the cached hashcode
		// must be invalidated
		cachedHashCode = INVALID_HASH_CODE;
		return this;
	}

	private void addSourceLinesForMethod(MethodAnnotation methodAnnotation, SourceLineAnnotation sourceLineAnnotation) {
		if (sourceLineAnnotation != null) {
			// Note: we don't add the source line annotation directly to
			// the bug instance.  Instead, we stash it in the MethodAnnotation.
			// It is much more useful there, and it would just be distracting
			// if it were displayed in the UI, since it would compete for attention
			// with the actual bug location source line annotation (which is much
			// more important and interesting).
			methodAnnotation.setSourceLines(sourceLineAnnotation);
		}
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == INVALID_HASH_CODE) {
			int hashcode = type.hashCode() + priority;
			Iterator<BugAnnotation> i = annotationIterator();
			while (i.hasNext())
				hashcode += i.next().hashCode();
			if (hashcode == INVALID_HASH_CODE)
				hashcode = INVALID_HASH_CODE+1;
			cachedHashCode = hashcode;
		}

		return cachedHashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BugInstance))
			return false;
		BugInstance other = (BugInstance) o;
		if (!type.equals(other.type) || priority != other.priority)
			return false;
		if (annotationList.size() != other.annotationList.size())
			return false;
		int numAnnotations = annotationList.size();
		for (int i = 0; i < numAnnotations; ++i) {
			BugAnnotation lhs = annotationList.get(i);
			BugAnnotation rhs = other.annotationList.get(i);
			if (!lhs.equals(rhs))
				return false;
		}

		return true;
	}

	public int compareTo(BugInstance other) {
		int cmp;
		cmp = type.compareTo(other.type);
		if (cmp != 0)
			return cmp;
		cmp = priority - other.priority;
		if (cmp != 0)
			return cmp;

		// Compare BugAnnotations lexicographically
		int pfxLen = Math.min(annotationList.size(), other.annotationList.size());
		for (int i = 0; i < pfxLen; ++i) {
			BugAnnotation lhs = annotationList.get(i);
			BugAnnotation rhs = other.annotationList.get(i);
			cmp = lhs.compareTo(rhs);
			if (cmp != 0)
				return cmp;
		}

		// All elements in prefix were the same,
		// so use number of elements to decide
		return annotationList.size() - other.annotationList.size();
	}

	/**
	 * @param firstVersion The firstVersion to set.
	 */
	public void setFirstVersion(long firstVersion) {
		this.firstVersion = firstVersion;
		if (lastVersion >= 0 && firstVersion > lastVersion) 
			throw new IllegalArgumentException(
				firstVersion + ".." + lastVersion);
	}

	/**
	 * @return Returns the firstVersion.
	 */
	public long getFirstVersion() {
		return firstVersion;
	}

	/**
	 * @param lastVersion The lastVersion to set.
	 */
	public void setLastVersion(long lastVersion) {
		if (lastVersion >= 0 && firstVersion > lastVersion) 
			throw new IllegalArgumentException(
				firstVersion + ".." + lastVersion);
		this.lastVersion = lastVersion;
	}

	/**
	 * @return Returns the lastVersion.
	 */
	public long getLastVersion() {
		return lastVersion;
	}

	/**
	 * @param introducedByChangeOfExistingClass The introducedByChangeOfExistingClass to set.
	 */
	public void setIntroducedByChangeOfExistingClass(boolean introducedByChangeOfExistingClass) {
		this.introducedByChangeOfExistingClass = introducedByChangeOfExistingClass;
	}

	/**
	 * @return Returns the introducedByChangeOfExistingClass.
	 */
	public boolean isIntroducedByChangeOfExistingClass() {
		return introducedByChangeOfExistingClass;
	}

	/**
	 * @param removedByChangeOfPersistingClass The removedByChangeOfPersistingClass to set.
	 */
	public void setRemovedByChangeOfPersistingClass(boolean removedByChangeOfPersistingClass) {
		this.removedByChangeOfPersistingClass = removedByChangeOfPersistingClass;
	}

	/**
	 * @return Returns the removedByChangeOfPersistingClass.
	 */
	public boolean isRemovedByChangeOfPersistingClass() {
		return removedByChangeOfPersistingClass;
	}

	/**
	 * @param instanceHash The instanceHash to set.
	 */
	public void setInstanceHash(String instanceHash) {
		this.instanceHash = instanceHash;
	}
	/**
	 * @param oldInstanceHash The oldInstanceHash to set.
	 */
	public void setOldInstanceHash(String oldInstanceHash) {
		this.oldInstanceHash = oldInstanceHash;
	}
	/**
	 * @return Returns the instanceHash.
	 */
	public String getInstanceHash() {
		if (instanceHash != null) return instanceHash;
			MessageDigest digest = null;
			try { digest = MessageDigest.getInstance("MD5");
			} catch (Exception e2) {
				// OK, we won't digest
			}
			instanceHash = getInstanceKey();
			if (digest != null) {
				byte [] data = digest.digest(instanceHash.getBytes());
				String tmp = new BigInteger(1,data).toString(16);
				instanceHash = tmp;
			}
			return instanceHash;
	}

	public boolean isInstanceHashConsistent() {
		return oldInstanceHash == null || instanceHash.equals(oldInstanceHash);
	}
	/**
	 * @param instanceOccurrenceNum The instanceOccurrenceNum to set.
	 */
	public void setInstanceOccurrenceNum(int instanceOccurrenceNum) {
		this.instanceOccurrenceNum = instanceOccurrenceNum;
	}

	/**
	 * @return Returns the instanceOccurrenceNum.
	 */
	public int getInstanceOccurrenceNum() {
		return instanceOccurrenceNum;
	}

	/**
	 * @param instanceOccurrenceMax The instanceOccurrenceMax to set.
	 */
	public void setInstanceOccurrenceMax(int instanceOccurrenceMax) {
		this.instanceOccurrenceMax = instanceOccurrenceMax;
	}

	/**
	 * @return Returns the instanceOccurrenceMax.
	 */
	public int getInstanceOccurrenceMax() {
		return instanceOccurrenceMax;
	}
}

// vim:ts=4
