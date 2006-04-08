/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2005 University of Maryland
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

import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.model.ClassFeatureSet;

/**
 * Build a BugCollection based on SAX events.
 * This is intended to replace the old DOM-based parsing
 * of XML bug result files, which was very slow.
 *
 * @author David Hovemeyer
 */
public class SAXBugCollectionHandler extends DefaultHandler {
	private BugCollection bugCollection;
	private Project project;

	private ArrayList<String> elementStack;
	private StringBuffer textBuffer;
	private BugInstance bugInstance;
	private PackageMemberAnnotation packageMemberAnnotation;
	private AnalysisError analysisError;
//	private ClassHash classHash;
	private ClassFeatureSet classFeatureSet;
	private ArrayList<String> stackTrace;
	private int nestingOfIgnoredElements = 0;

	public SAXBugCollectionHandler(BugCollection bugCollection, Project project) {
		this.bugCollection = bugCollection;
		this.project = project;

		this.elementStack = new ArrayList<String>();
		this.textBuffer = new StringBuffer();
		this.stackTrace = new ArrayList<String>();
	}

	Pattern ignoredElement = Pattern.compile("Message|ShortMessage|LongMessage|BugCategory|BugPattern|BugCode");
	
	public boolean discardedElement(String qName) {
		return ignoredElement.matcher(qName).matches();
		
	}

	public void startElement(String uri, String name, String qName, Attributes attributes)
		throws SAXException {
		// URI should always be empty.
		// So, qName is the name of the element.

		if (discardedElement(qName)) {
			nestingOfIgnoredElements++;
		} else if (nestingOfIgnoredElements > 0) {
			// ignore it
		} else if (elementStack.isEmpty()) {
			// We should be parsing the outer BugCollection element.
			if (!qName.equals("BugCollection"))
				throw new SAXException(
						"Invalid top-level element (expected BugCollection, saw " + qName + ")");
			
			// Read and set the sequence number.
			String sequence = attributes.getValue("sequence");
			long seqval = parseLong(sequence, 0L);
			bugCollection.setSequenceNumber(seqval);
			
			// Read and set timestamp.
			String timestamp = attributes.getValue("timestamp");
			long tsval = parseLong(timestamp, -1L);
			bugCollection.setTimestamp(tsval);
			
			// Set release name, if present.
			String releaseName = attributes.getValue("release");
			bugCollection.setReleaseName((releaseName != null) ? releaseName : "");
		} else {
			String outerElement = elementStack.get(elementStack.size() - 1);

			if (outerElement.equals("BugCollection")) {
				// Parsing a top-level element of the BugCollection
				if (qName.equals("Project")) {
					// Project element
					String filename = attributes.getValue(Project.FILENAME_ATTRIBUTE_NAME);
					if (filename != null)
						project.setProjectFileName(filename);
				} else if (qName.equals("BugInstance")) {
					// BugInstance element - get required type and priority attributes
					String type = getRequiredAttribute(attributes, "type", qName);
					String priority = getRequiredAttribute(attributes, "priority", qName);

					try {
						int prio = Integer.parseInt(priority);
						bugInstance = new BugInstance(type, prio);
					} catch (NumberFormatException e) {
						throw new SAXException("BugInstance with invalid priority value \"" +
							priority + "\"", e);
					}
					
					String uniqueId = attributes.getValue("uid");
					if (uniqueId != null) {
						bugInstance.setUniqueId(uniqueId);
					}

					String firstVersion = attributes.getValue("first");
					if (firstVersion != null) {
						bugInstance.setFirstVersion(Long.parseLong(firstVersion));
					}
					String lastVersion = attributes.getValue("last");
					if (lastVersion != null) {
						bugInstance.setLastVersion(Long.parseLong(lastVersion));
					}
					
					if (bugInstance.getLastVersion() >= 0 &&
							bugInstance.getFirstVersion() > bugInstance.getLastVersion())
						throw new IllegalStateException("huh");
					
					String introducedByChange = attributes.getValue("introducedByChange");
					if (introducedByChange != null) {
						bugInstance.setIntroducedByChangeOfExistingClass(TigerSubstitutes.parseBoolean(introducedByChange));
					}
					String removedByChange = attributes.getValue("removedByChange");
					if (removedByChange != null) {
						bugInstance.setRemovedByChangeOfPersistingClass(TigerSubstitutes.parseBoolean(removedByChange));
					}
					
					
				} else if (qName.equals("FindBugsSummary")) {
					String timestamp = getRequiredAttribute(attributes, "timestamp", qName);
					try {
						bugCollection.getProjectStats().setTimestamp(timestamp);
					} catch (java.text.ParseException e) {
						throw new SAXException("Unparseable sequence number: '" + timestamp + "'", e);
					}
				}
			} else if (outerElement.equals("BugInstance")) {
				// Parsing an attribute or property of a BugInstance
				BugAnnotation bugAnnotation = null;
				if (qName.equals("Class")) {
					String className = getRequiredAttribute(attributes, "classname", qName);
					bugAnnotation = packageMemberAnnotation = new ClassAnnotation(className);
				} else if (qName.equals("Method") || qName.equals("Field")) {
					String classname = getRequiredAttribute(attributes, "classname", qName);
					String fieldOrMethodName = getRequiredAttribute(attributes, "name", qName);
					String signature = getRequiredAttribute(attributes, "signature", qName);
					if (qName.equals("Method")) {
						String isStatic = attributes.getValue("isStatic");
						if (isStatic == null) {
							isStatic = "false"; // Hack for old data
						}

						bugAnnotation = packageMemberAnnotation = 
							new MethodAnnotation(classname, fieldOrMethodName, signature, Boolean.valueOf(isStatic));

					} else {
						String isStatic = getRequiredAttribute(attributes, "isStatic", qName);
						bugAnnotation = packageMemberAnnotation = 
							new FieldAnnotation(classname, fieldOrMethodName, signature, Boolean.valueOf(isStatic));
					}
					
				} else if (qName.equals("SourceLine")) {
					SourceLineAnnotation sourceAnnotation = createSourceLineAnnotation(qName, attributes);
					if (!sourceAnnotation.isSynthetic())
						bugAnnotation = sourceAnnotation;
				} else if (qName.equals("Int")) {
					try {
						String value = getRequiredAttribute(attributes, "value", qName);
						bugAnnotation = new IntAnnotation(Integer.parseInt(value));
					} catch (NumberFormatException e) {
						throw new SAXException("Bad integer value in Int");
					}
				} else if (qName.equals("String")) {
						String value = getRequiredAttribute(attributes, "value", qName);
						bugAnnotation = new StringAnnotation(value);
				} else if (qName.equals("LocalVariable")) {
					try {
						String varName = getRequiredAttribute(attributes, "name", qName);
						int register = Integer.parseInt(getRequiredAttribute(attributes, "register", qName));
						int pc = Integer.parseInt(getRequiredAttribute(attributes, "pc", qName));
						bugAnnotation = new LocalVariableAnnotation(varName, register, pc);
					} catch (NumberFormatException e) {
						throw new SAXException("Invalid integer value in attribute of LocalVariable element");
					}
				} else if (qName.equals("Property")) {
					// A BugProperty.
					String propName = getRequiredAttribute(attributes, "name", qName);
					String propValue = getRequiredAttribute(attributes, "value", qName);
					bugInstance.setProperty(propName, propValue);
				} else throw new SAXException("Unknown bug annotation named " + qName);

				if (bugAnnotation != null) {
					String role = attributes.getValue("role");
					if (role != null)
						bugAnnotation.setDescription(role);
					setAnnotationRole(attributes, bugAnnotation);
					bugInstance.add(bugAnnotation);
				}
			} else if (outerElement.equals("Method") || outerElement.equals("Field") || outerElement.equals("Class")) {
				if (qName.equals("SourceLine")) {
					// package member elements can contain nested SourceLine elements.
					packageMemberAnnotation.setSourceLines(createSourceLineAnnotation(qName, attributes));
				}
			} else if (outerElement.equals(BugCollection.ERRORS_ELEMENT_NAME)) {
				if (qName.equals(BugCollection.ANALYSIS_ERROR_ELEMENT_NAME) ||
						qName.equals(BugCollection.ERROR_ELEMENT_NAME)) {
					analysisError = new AnalysisError("Unknown error");
					stackTrace.clear();
				}
			} else if (outerElement.equals("PackageStats")) {
				if (qName.equals("ClassStats")) {
					String className = getRequiredAttribute(attributes, "class", qName);
					Boolean isInterface = Boolean.valueOf(
						getRequiredAttribute(attributes, "interface", qName));
					int size = Integer.valueOf(
						getRequiredAttribute(attributes, "size", qName));
					bugCollection.getProjectStats().addClass(className, isInterface, size);
				}
			} else if (outerElement.equals("ClassFeatures")) {
				if (qName.equals(ClassFeatureSet.ELEMENT_NAME)) {
					String className = getRequiredAttribute(attributes, "class", qName);
					classFeatureSet = new ClassFeatureSet();
					classFeatureSet.setClassName(className);
				}
			} else if (outerElement.equals(ClassFeatureSet.ELEMENT_NAME)) {
				if (qName.equals(ClassFeatureSet.FEATURE_ELEMENT_NAME)) {
					String value = getRequiredAttribute(attributes, "value", qName);
					classFeatureSet.addFeature(value);
				}
			} else if (outerElement.equals(BugCollection.HISTORY_ELEMENT_NAME)) {
				if (qName.equals(AppVersion.ELEMENT_NAME)) {
					try {
						String sequence = getRequiredAttribute(attributes, "sequence", qName);
						String timestamp = attributes.getValue("timestamp");
						String releaseName = attributes.getValue("release");
						String codeSize = attributes.getValue("codeSize");
						String numClasses = attributes.getValue("numClasses");
						AppVersion appVersion = new AppVersion(Long.valueOf(sequence));
						if (timestamp != null)
							appVersion.setTimestamp(Long.valueOf(timestamp));
						if (releaseName != null)
							appVersion.setReleaseName(releaseName);
						if (codeSize != null)
							appVersion.setCodeSize(Integer.parseInt(codeSize));
						if (numClasses != null)
							appVersion.setNumClasses(Integer.parseInt(numClasses));
						
						bugCollection.addAppVersion(appVersion);
					} catch (NumberFormatException e) {
						throw new SAXException("Invalid AppVersion element", e);
					}
				}
			}
		}

		textBuffer.delete(0, textBuffer.length());
		elementStack.add(qName);
	}

	private long parseLong(String s, long defaultValue) {
		long value;
		try {
			value = (s != null) ? Long.parseLong(s) : defaultValue;
		} catch (NumberFormatException e) {
			value = defaultValue;
		}
		return value;
	}

	/**
	 * Extract a hash value from an element.
	 * 
	 * @param qName      name of element containing hash value
	 * @param attributes element attributes
	 * @return the decoded hash value
	 * @throws SAXException
	 */
	private byte[] extractHash(String qName, Attributes attributes) throws SAXException {
		String encodedHash = getRequiredAttribute(attributes, "value", qName);
		byte[] hash;
		try {
			//System.out.println("Extract hash " + encodedHash);
			hash= ClassHash.stringToHash(encodedHash);
		} catch (IllegalArgumentException e) {
			throw new SAXException("Invalid class hash", e);
		}
		return hash;
	}

	private void setAnnotationRole(Attributes attributes, BugAnnotation bugAnnotation) {
		String role = attributes.getValue("role");
		if (role != null)
			bugAnnotation.setDescription(role);
	}

	private SourceLineAnnotation createSourceLineAnnotation(String qName, Attributes attributes)
			throws SAXException {
		String classname = getRequiredAttribute(attributes, "classname", qName);
		String sourceFile = attributes.getValue("sourcefile");
		if (sourceFile == null)
			sourceFile = SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
		String startLine = getRequiredAttribute(attributes, "start", qName);
		String endLine = getRequiredAttribute(attributes, "end", qName);
		String startBytecode = attributes.getValue("startBytecode");
		String endBytecode = attributes.getValue("endBytecode");

		try {
			int sl = Integer.parseInt(startLine);
			int el = Integer.parseInt(endLine);
			int sb = startBytecode != null ? Integer.parseInt(startBytecode) : -1;
			int eb = endBytecode != null ? Integer.parseInt(endBytecode) : -1;

			SourceLineAnnotation annotation =
				new SourceLineAnnotation(classname, sourceFile, sl, el, sb, eb);
			
			return annotation;
		} catch (NumberFormatException e) {
			throw new SAXException("Bad integer value in SourceLine element", e);
		}
	}


	public void endElement(String uri, String name, String qName) throws SAXException {
		// URI should always be empty.
		// So, qName is the name of the element.

		if (discardedElement(qName)) {
			nestingOfIgnoredElements--;
		} else if (nestingOfIgnoredElements > 0) {
			// ignore it
		} else if (elementStack.size() > 1) {
			String outerElement = elementStack.get(elementStack.size() - 2);

			if (outerElement.equals("BugCollection")) {
				if (qName.equals("BugInstance")) {
					bugCollection.add(bugInstance, false);
                   // TODO: check this
                    if (bugInstance.getLastVersion() == -1)
                    	bugCollection.getProjectStats().addBug(bugInstance);
				}
			} else if (outerElement.equals("Project")) {
				//System.out.println("Adding project element " + qName + ": " + textBuffer.toString());
				if (qName.equals("Jar"))
					project.addFile(textBuffer.toString());
				else if (qName.equals("SrcDir"))
					project.addSourceDir(textBuffer.toString());
				else if (qName.equals("AuxClasspathEntry"))
					project.addAuxClasspathEntry(textBuffer.toString());
			} else if (outerElement.equals("BugInstance")) {
				if (qName.equals("UserAnnotation")) {
					bugInstance.setAnnotationText(textBuffer.toString());
				}
			} else if (outerElement.equals(BugCollection.ERRORS_ELEMENT_NAME)) {
				if (qName.equals(BugCollection.ANALYSIS_ERROR_ELEMENT_NAME)) {
					analysisError.setMessage(textBuffer.toString());
					bugCollection.addError(analysisError);
				} else if (qName.equals(BugCollection.ERROR_ELEMENT_NAME)) {
					if (stackTrace.size() > 0) {
						analysisError.setStackTrace(stackTrace.toArray(new String[stackTrace.size()]));
					}
					bugCollection.addError(analysisError);
				} else if (qName.equals(BugCollection.MISSING_CLASS_ELEMENT_NAME)) {
					bugCollection.addMissingClass(textBuffer.toString());
				}
				
			} else if (outerElement.equals(BugCollection.ERROR_ELEMENT_NAME)) {
				if (qName.equals(BugCollection.ERROR_MESSAGE_ELEMENT_NAME)) {
					analysisError.setMessage(textBuffer.toString());
				} else if (qName.equals(BugCollection.ERROR_EXCEPTION_ELEMENT_NAME)) {
					analysisError.setExceptionMessage(textBuffer.toString());
				} else if (qName.equals(BugCollection.ERROR_STACK_TRACE_ELEMENT_NAME)) {
					stackTrace.add(textBuffer.toString());
				}
			} else if (outerElement.equals("ClassFeatures")) {
				if (qName.equals(ClassFeatureSet.ELEMENT_NAME)) {
					bugCollection.setClassFeatureSet(classFeatureSet);
					classFeatureSet = null;
				}
			}
		}

		elementStack.remove(elementStack.size() - 1);
	}

	public void characters(char[] ch, int start, int length) {
		textBuffer.append(ch, start, length);
	}

	private static String getRequiredAttribute(Attributes attributes, String attrName, String elementName)
		throws SAXException {
		String value = attributes.getValue(attrName);
		if (value == null)
			throw new SAXException(elementName + " element missing " + attrName + " attribute");
		return value;
	}

	// Just a test driver
	public static void main(String[] argv) throws Exception {
		XMLReader xr = XMLReaderFactory.createXMLReader();

		BugCollection bugCollection = new SortedBugCollection();
		Project project = new Project();

		SAXBugCollectionHandler handler = new SAXBugCollectionHandler(bugCollection, project);
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);

		// Parse each file provided on the
		// command line.
		for (String aArgv : argv) {
			FileReader r = new FileReader(aArgv);
			xr.parse(new InputSource(r));
		}
	}
}

// vim:ts=4
