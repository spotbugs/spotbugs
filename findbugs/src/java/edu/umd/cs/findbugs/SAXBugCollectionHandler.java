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

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.filter.AndMatcher;
import edu.umd.cs.findbugs.filter.BugMatcher;
import edu.umd.cs.findbugs.filter.ClassMatcher;
import edu.umd.cs.findbugs.filter.CompoundMatcher;
import edu.umd.cs.findbugs.filter.FieldMatcher;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LocalMatcher;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.filter.MethodMatcher;
import edu.umd.cs.findbugs.filter.OrMatcher;
import edu.umd.cs.findbugs.filter.PriorityMatcher;
import edu.umd.cs.findbugs.model.ClassFeatureSet;
import edu.umd.cs.findbugs.util.Strings;

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
	private Stack<CompoundMatcher> matcherStack = new Stack<CompoundMatcher>();
	private Filter filter;

	private ArrayList<String> elementStack;
	private StringBuffer textBuffer;
	private BugInstance bugInstance;
	private PackageMemberAnnotation packageMemberAnnotation;
	private AnalysisError analysisError;
//	private ClassHash classHash;
	private ClassFeatureSet classFeatureSet;
	private ArrayList<String> stackTrace;
	private int nestingOfIgnoredElements = 0;
	private final File base;

	public SAXBugCollectionHandler(BugCollection bugCollection, Project project, File base) {
		this.bugCollection = bugCollection;
		this.project = project;

		this.elementStack = new ArrayList<String>();
		this.textBuffer = new StringBuffer();
		this.stackTrace = new ArrayList<String>();
		this.base = base;
	}

	Pattern ignoredElement = Pattern.compile("Message|ShortMessage|LongMessage|BugCategory|BugPattern|BugCode");

	public boolean discardedElement(String qName) {
		return ignoredElement.matcher(qName).matches();

	}

	@Override
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
			String version = attributes.getValue("version");
			if (bugCollection instanceof SortedBugCollection)
			  ((SortedBugCollection)bugCollection).setAnalysisVersion(version);

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
				parseBugCollectionContents(qName, attributes);
			} else if (outerElement.equals("BugInstance")) {
				parseBugInstanceContents(qName, attributes);
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
					String sourceFile =  attributes.getValue("sourceFile");
					bugCollection.getProjectStats().addClass(className, sourceFile, isInterface, size);
				}
			} else if (outerElement.equals("Project")) {
				if (isTopLevelFilter(qName))  {
					filter = new Filter();
					matcherStack.clear();
					pushCompoundMatcher(filter);
					project.setSuppressionFilter(filter);
				}
			} else if (isTopLevelFilter(outerElement) ) {
				 if (qName.equals("Match")) {
					 pushCompoundMatcher(new AndMatcher());
				 }
			} else if (outerElement.equals("Match") || outerElement.equals("And") || outerElement.equals("Or")) {
				parseMatcher(qName, attributes);
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

	private boolean isTopLevelFilter(String qName) {
	    return qName.equals("FindBugsFilter") || qName.equals("SuppressionFilter");
    }

	private void addMatcher(Matcher m) {
		if (m == null) throw new IllegalArgumentException("matcher must not be null");
		CompoundMatcher peek = matcherStack.peek();
		if (peek == null)
			throw new NullPointerException("Top of stack is null");
		peek.addChild(m);
	}
	private void pushCompoundMatcherAsChild(CompoundMatcher m) {
		addMatcher(m);
		pushCompoundMatcher(m);
	}
	private void pushCompoundMatcher(CompoundMatcher m) {
		if (m == null) 
			throw new IllegalArgumentException("matcher must not be null");
		matcherStack.push(m);
	}
	private void parseMatcher(String qName, Attributes attributes) {
	    if (qName.equals("Bug")) {
	    	addMatcher(new BugMatcher(attributes.getValue("code"),
	    			attributes.getValue("pattern"),
	    			attributes.getValue("category")));
	    } else if (qName.equals("Class")) {
	    	addMatcher(new ClassMatcher(attributes.getValue("name")));
	    } else if (qName.equals("BugCode")) {
	    	addMatcher(new BugMatcher(attributes.getValue("name"),"",""));
	    } else if (qName.equals("Local")) {
	    	addMatcher(new LocalMatcher(attributes.getValue("name")));
	    } else if (qName.equals("BugPattern")) {
	    	addMatcher(new BugMatcher("",attributes.getValue("name"),""));
	    } else if (qName.equals("Priority")) {
	    	addMatcher(new PriorityMatcher(attributes.getValue("value")));
	    } else if (qName.equals("Package")) {
	    	String pName = attributes.getValue("name");
	    	pName = pName.startsWith("~") ? pName : "~" + Strings.replace(pName, ".", "\\.");			
			addMatcher( new ClassMatcher(pName + "\\.[^.]+"));
	    } else if (qName.equals("Method")) {
	    	String name = attributes.getValue("name");
	    	String params = attributes.getValue("params");
	    	String returns = attributes.getValue("returns");
	    	addMatcher(new MethodMatcher(name, params, returns));

	    } else if (qName.equals("Field")) {
	    	String name = attributes.getValue("name");
			String type = attributes.getValue("type");
			addMatcher(new FieldMatcher(name, type));
	    } else if (qName.equals("Or")) {
	    	CompoundMatcher matcher = new OrMatcher();
	    	pushCompoundMatcherAsChild(matcher);
	    	
	    } else if (qName.equals("And")) {
	    	AndMatcher matcher = new AndMatcher();
	    	pushCompoundMatcherAsChild(matcher);
	    }
    }


	private void parseBugInstanceContents(String qName, Attributes attributes) throws SAXException {
	    // Parsing an attribute or property of a BugInstance
	    BugAnnotation bugAnnotation = null;
	    if (qName.equals("Class")) {
	    	String className = getRequiredAttribute(attributes, "classname", qName);
	    	bugAnnotation = packageMemberAnnotation = new ClassAnnotation(className);
	    } else if (qName.equals("Type")) {
	    	String typeDescriptor = getRequiredAttribute(attributes, "descriptor", qName);
	    	bugAnnotation = new TypeAnnotation(typeDescriptor);
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
	    } else if (qName.equals("UserAnnotation")) {
	    	// ignore AnnotationText for now; will handle in endElement
	    	String s = attributes.getValue("designation"); // optional
	    	BugDesignation userDesignation = bugInstance.getNonnullUserDesignation();
	    	if (s != null) userDesignation.setDesignationKey(s);
	    	s = attributes.getValue("user"); // optional
	    	if (s != null) userDesignation.setUser(s);
	    	s = attributes.getValue("timestamp"); // optional
	    	if (s != null) try {
	    		long timestamp = Long.valueOf(s);
	    		userDesignation.setTimestamp(timestamp);
	    	}
	    	catch (NumberFormatException nfe) {
	    		// ok to contine -- just won't set a timestamp for the user designation.
	    		// but is there anyplace to report this?
	    	}
	    } else throw new SAXException("Unknown bug annotation named " + qName);

	    if (bugAnnotation != null) {
	    	String role = attributes.getValue("role");
	    	if (role != null)
	    		bugAnnotation.setDescription(role);
	    	setAnnotationRole(attributes, bugAnnotation);
	    	bugInstance.add(bugAnnotation);
	    }
    }

	private void parseBugCollectionContents(String qName, Attributes attributes) throws SAXException {
	    // Parsing a top-level element of the BugCollection
	    if (qName.equals("Project")) {
	    	// Project element
	    	String filename = attributes.getValue(Project.FILENAME_ATTRIBUTE_NAME);
	    	if (filename != null)
	    		project.setProjectFileName(filename);
	    	String projectName = attributes.getValue(Project.PROJECTNAME_ATTRIBUTE_NAME);
	    	if (projectName != null)
	    		project.setProjectName(projectName);
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
	    	String oldInstanceHash = attributes.getValue("instanceHash");
	    	if (oldInstanceHash != null) {
	    		bugInstance.setOldInstanceHash(oldInstanceHash);
	    		}


	    } else if (qName.equals("FindBugsSummary")) {
	    	String timestamp = getRequiredAttribute(attributes, "timestamp", qName);
	    	try {
	    		bugCollection.getProjectStats().setTimestamp(timestamp);
	    	} catch (java.text.ParseException e) {
	    		throw new SAXException("Unparseable sequence number: '" + timestamp + "'", e);
	    	}
	    }
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
		String startLine = attributes.getValue("start"); // "start"/"end" are now optional
		String endLine = attributes.getValue("end");     // (were too many "-1"s in the xml)
		String startBytecode = attributes.getValue("startBytecode");
		String endBytecode = attributes.getValue("endBytecode");

		try {
			int sl = startLine != null ? Integer.parseInt(startLine) : -1;
			int el = endLine != null ? Integer.parseInt(endLine) : -1;
			int sb = startBytecode != null ? Integer.parseInt(startBytecode) : -1;
			int eb = endBytecode != null ? Integer.parseInt(endBytecode) : -1;

			SourceLineAnnotation annotation =
				new SourceLineAnnotation(classname, sourceFile, sl, el, sb, eb);

			return annotation;
		} catch (NumberFormatException e) {
			throw new SAXException("Bad integer value in SourceLine element", e);
		}
	}


	@Override
	public void endElement(String uri, String name, String qName) throws SAXException {
		// URI should always be empty.
		// So, qName is the name of the element.

		if (discardedElement(qName)) {
			nestingOfIgnoredElements--;
		} else if (nestingOfIgnoredElements > 0) {
			// ignore it
		} else if (elementStack.size() > 1) {
			String outerElement = elementStack.get(elementStack.size() - 2);

			if (qName.equals("Or") || qName.equals("And") || qName.equals("Match") || qName.equals("FindBugsFilter"))
				matcherStack.pop();
			else if (outerElement.equals("BugCollection")) {
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

	@Override
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


}

// vim:ts=4
