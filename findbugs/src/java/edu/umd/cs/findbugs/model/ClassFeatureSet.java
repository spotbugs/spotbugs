/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * Features of a class which may be used to identify it if it is renamed
 * or modified.
 * 
 * @author David Hovemeyer
 */
public class ClassFeatureSet implements XMLWriteable {
	public static final String CLASS_NAME_KEY = "Class:";
	public static final String METHOD_NAME_KEY = "Method:";
	public static final String FIELD_NAME_KEY = "Field:";
	
	private String className;
	private Set<String> featureSet;
	
	/**
	 * Constructor.
	 * Creates an empty feature set.
	 */
	public ClassFeatureSet() {
		this.featureSet = new HashSet<String>();
	}
	
	/**
	 * Initialize from given JavaClass.
	 * 
	 * @param javaClass the JavaClass
	 * @return this object
	 */
	public ClassFeatureSet initialize(JavaClass javaClass) {
		this.className = javaClass.getClassName();
		
		addFeature(CLASS_NAME_KEY + javaClass.getClassName());
		
		for (Method method : javaClass.getMethods()) {
			if (!isSynthetic(method)) {
				addFeature(
						METHOD_NAME_KEY + method.getName() +
						transformMethodSignature(method.getSignature()));
			}
		}
		
		for (Field field : javaClass.getFields()) {
			if (!isSynthetic(field)) {
				addFeature(
						FIELD_NAME_KEY + field.getName() +
						transformSignature(field.getSignature()));
			}
		}
		
		return this;
	}
	
	/**
	 * Figure out if a class member (field or method) is synthetic.
	 * 
	 * @param member a field or method
	 * @return true if the member is synthetic
	 */
	private boolean isSynthetic(FieldOrMethod member) {
		if (member.isSynthetic()) // this never works, but worth a try
			return true;
		
		String name = member.getName();
		
		if (name.startsWith("class$"))
			return true;
		
		if (name.startsWith("access$"))
			return true;
		
		return false;
	}

	/**
	 * @return Returns the className.
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * @param className The className to set.
	 */
	public void setClassName(String className) {
		this.className = className;
	}
	
	public int getNumFeatures() {
		return featureSet.size();
	}
	
	public void addFeature(String feature) {
		featureSet.add(feature);
	}
	
	public Iterator<String> featureIterator() {
		return featureSet.iterator();
	}
	
	public boolean hasFeature(String feature) {
		return featureSet.contains(feature);
	}
	
	/**
	 * Transform a class name by stripping its package name.
	 * 
	 * @param className a class name
	 * @return the transformed class name
	 */
	public static String transformClassName(String className) {
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			String pkg = className.substring(0, lastDot);
			if (!isUnlikelyToBeRenamed(pkg)) {
				className = className.substring(lastDot + 1);
			}
		}
		return className;
	}
	
	/**
	 * Return true if classes in the given package is unlikely to be renamed:
	 * e.g., because they are part of a public API.
	 * 
	 * @param pkg the package name
	 * @return true if classes in the package is unlikely to be renamed 
	 */
	public static boolean isUnlikelyToBeRenamed(String pkg) {
		return pkg.startsWith("java.");
	}

	/**
	 * Transform a method signature to allow it to be compared even if
	 * any of its parameter types are moved to another package.
	 * 
	 * @param signature a method signature
	 * @return the transformed signature
	 */
	public static String transformMethodSignature(String signature) {
		StringBuffer buf = new StringBuffer();

		buf.append('(');

		SignatureParser parser = new SignatureParser(signature);
		for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext(); ) {
			String param = i.next();
				param = transformSignature(param);
			buf.append(param);
		}
		
		buf.append(')');
		
		return buf.toString();
	}

	/**
	 * Transform a field or method parameter signature to allow it to be
	 * compared even if it is moved to another package.
	 * 
	 * @param signature the signature
	 * @return the transformed signature
	 */
	public static String transformSignature(String signature) {
		StringBuffer buf = new StringBuffer();
		
		int lastBracket = signature.lastIndexOf('[');
		if (lastBracket > 0) {
			buf.append(signature.substring(0, lastBracket+1));
			signature = signature.substring(lastBracket+1);
		}

		if (signature.startsWith("L")) {
			signature = signature.substring(1, signature.length() - 1).replace('/', '.');
			signature = transformClassName(signature);
			signature = "L" + signature.replace('.', '/') +  ";";
		}
		buf.append(signature);
		
		return buf.toString();
	}
	
	/**
	 * Minimum number of features which must be present in order
	 * to declare two classes similar.
	 */
	public static final int MIN_FEATURES = 5;
	
	/**
	 * Minimum fraction of features which must be shared in order
	 * to declare two classes similar.
	 */
	public static final double MIN_MATCH = 0.60;
	
	public static double similarity(ClassFeatureSet a, ClassFeatureSet b) {
		if (a.getNumFeatures() < MIN_FEATURES || b.getNumFeatures() < MIN_FEATURES)
			return 0.0;
		
		int numMatch = 0;
		int max = Math.max(a.getNumFeatures(), b.getNumFeatures());
		
		for (Iterator<String> i = a.featureIterator(); i.hasNext();) {
			String feature = i.next();
			if (b.hasFeature(feature)) {
				++numMatch;
			}
		}
		
		return ((double) numMatch / (double) max);
		
	}
	
	public boolean similarTo(ClassFeatureSet other) {
		return similarity(this, other) >= MIN_MATCH;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: " + ClassFeatureSet.class.getName() + " <class 1> <class 2>");
			System.exit(1);
		}
		
		JavaClass a = Repository.lookupClass(args[0]);
		JavaClass b = Repository.lookupClass(args[1]);
		
		ClassFeatureSet aFeatures = new ClassFeatureSet().initialize(a);
		ClassFeatureSet bFeatures = new ClassFeatureSet().initialize(b);
		
		System.out.println("Similarity is " + similarity(aFeatures, bFeatures));
		System.out.println("Classes are" + (aFeatures.similarTo(bFeatures) ? "" : " not") + " similar");
	}
	
	public static final String ELEMENT_NAME = "ClassFeatureSet";
	public static final String FEATURE_ELEMENT_NAME = "Feature";

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.xml.XMLWriteable#writeXML(edu.umd.cs.findbugs.xml.XMLOutput)
	 */
	public void writeXML(XMLOutput xmlOutput) throws IOException {
		xmlOutput.openTag(ELEMENT_NAME, new XMLAttributeList().addAttribute("class", className));
		for (Iterator<String> i = featureIterator(); i.hasNext(); ) {
			String feature = i.next();
			xmlOutput.openCloseTag(
					FEATURE_ELEMENT_NAME,
					new XMLAttributeList().addAttribute("value", feature));
		}
		xmlOutput.closeTag(ELEMENT_NAME);
	}
}
