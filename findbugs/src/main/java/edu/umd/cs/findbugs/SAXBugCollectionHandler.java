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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.umd.cs.findbugs.filter.AndMatcher;
import edu.umd.cs.findbugs.filter.BugMatcher;
import edu.umd.cs.findbugs.filter.ClassMatcher;
import edu.umd.cs.findbugs.filter.CompoundMatcher;
import edu.umd.cs.findbugs.filter.ConfidenceMatcher;
import edu.umd.cs.findbugs.filter.DesignationMatcher;
import edu.umd.cs.findbugs.filter.FieldMatcher;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.FirstVersionMatcher;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import edu.umd.cs.findbugs.filter.LocalMatcher;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.filter.MethodMatcher;
import edu.umd.cs.findbugs.filter.NotMatcher;
import edu.umd.cs.findbugs.filter.OrMatcher;
import edu.umd.cs.findbugs.filter.PriorityMatcher;
import edu.umd.cs.findbugs.filter.RankMatcher;
import edu.umd.cs.findbugs.filter.SourceMatcher;
import edu.umd.cs.findbugs.filter.TypeMatcher;
import edu.umd.cs.findbugs.model.ClassFeatureSet;
import edu.umd.cs.findbugs.util.MapCache;
import edu.umd.cs.findbugs.util.Strings;

/**
 * Build a BugCollection based on SAX events. This is intended to replace the
 * old DOM-based parsing of XML bug result files, which was very slow.
 *
 * @author David Hovemeyer
 */
public class SAXBugCollectionHandler extends DefaultHandler {
    private static final String FIND_BUGS_FILTER = "FindBugsFilter";

    private static final String PROJECT = "Project";

    private static final String BUG_COLLECTION = "BugCollection";

    private static final Logger LOGGER = Logger.getLogger(SAXBugCollectionHandler.class.getName());

    public String getOptionalAttribute(Attributes attributes, String qName) {
        return memoized(attributes.getValue(qName));
    }

    @CheckForNull
    private final BugCollection bugCollection;

    @CheckForNull
    private final Project project;

    private final Stack<CompoundMatcher> matcherStack = new Stack<CompoundMatcher>();

    private Filter filter;

    private final MapCache<String, String> cache = new MapCache<String, String>(2000);

    private final ArrayList<String> elementStack;

    private final StringBuilder textBuffer;

    private BugInstance bugInstance;

    private BugAnnotationWithSourceLines bugAnnotationWithSourceLines;

    private AnalysisError analysisError;

    // private ClassHash classHash;
    private ClassFeatureSet classFeatureSet;

    private final ArrayList<String> stackTrace;

    private int nestingOfIgnoredElements = 0;

    private final @CheckForNull File base;

    private final String topLevelName;

    private String cloudPropertyKey;

    private SAXBugCollectionHandler(String topLevelName, @CheckForNull BugCollection bugCollection,
            @CheckForNull Project project, @CheckForNull File base) {
        this.topLevelName = topLevelName;
        this.bugCollection = bugCollection;
        this.project = project;

        this.elementStack = new ArrayList<String>();
        this.textBuffer = new StringBuilder();
        this.stackTrace = new ArrayList<String>();
        this.base = base;

    }

    public SAXBugCollectionHandler(BugCollection bugCollection, @CheckForNull File base) {
        this(BUG_COLLECTION, bugCollection, bugCollection.getProject(), base);
    }

    public SAXBugCollectionHandler(BugCollection bugCollection) {
        this(BUG_COLLECTION, bugCollection, bugCollection.getProject(), null);
    }

    public SAXBugCollectionHandler(Project project, File base) {
        this(PROJECT, null, project, base);
    }

    public SAXBugCollectionHandler(Filter filter, File base) {
        this(FIND_BUGS_FILTER, null, null, base);
        this.filter = filter;
        pushCompoundMatcher(filter);
    }

    Pattern ignoredElement = Pattern.compile("Message|ShortMessage|LongMessage");

    public boolean discardedElement(String qName) {
        return ignoredElement.matcher(qName).matches();

    }

    public String getTextContents() {
        return memoized(Strings.unescapeXml(textBuffer.toString()));
    }

    private String memoized(String s) {
        if (s == null) {
            return s;
        }
        String result = cache.get(s);
        if (result != null) {
            return result;
        }
        cache.put(s, s);
        return s;
    }

    private static boolean DEBUG = false;

    @Override
    public void startElement(String uri, String name, String qName, Attributes attributes) throws SAXException {
        // URI should always be empty.
        // So, qName is the name of the element.

        if (discardedElement(qName)) {
            nestingOfIgnoredElements++;
        } else if (nestingOfIgnoredElements > 0) {
            // ignore it
        } else {
            // We should be parsing the outer BugCollection element.
            if (elementStack.isEmpty() && !qName.equals(topLevelName)) {
                throw new SAXException("Invalid top-level element (expected " + topLevelName + ", saw " + qName + ")");
            }

            if (BUG_COLLECTION.equals(qName)) {
                BugCollection bugCollection = this.bugCollection;
                assert bugCollection != null;
                // Read and set the sequence number.
                String version = getOptionalAttribute(attributes, "version");
                if (bugCollection instanceof SortedBugCollection) {
                    bugCollection.setAnalysisVersion(version);
                }

                // Read and set the sequence number.
                String sequence = getOptionalAttribute(attributes, "sequence");
                long seqval = parseLong(sequence, 0L);
                bugCollection.setSequenceNumber(seqval);

                // Read and set timestamp.
                String timestamp = getOptionalAttribute(attributes, "timestamp");
                long tsval = parseLong(timestamp, -1L);
                bugCollection.setTimestamp(tsval);
                // Read and set timestamp.
                String analysisTimestamp = getOptionalAttribute(attributes, "analysisTimestamp");
                if (analysisTimestamp != null) {
                    bugCollection.setAnalysisTimestamp(parseLong(analysisTimestamp, -1L));
                }
                String analysisVersion = getOptionalAttribute(attributes, "version");
                if (analysisVersion != null) {
                    bugCollection.setAnalysisVersion(analysisVersion);
                }

                // Set release name, if present.
                String releaseName = getOptionalAttribute(attributes, "release");
                bugCollection.setReleaseName((releaseName != null) ? releaseName : "");
            } else if (isTopLevelFilter(qName)) {
                if (project != null) {
                    filter = new Filter();
                    project.setSuppressionFilter(filter);
                }
                matcherStack.clear();
                pushCompoundMatcher(filter);
            } else if (PROJECT.equals(qName)) {
                Project project = this.project;
                assert project != null;
                // Project element
                String projectName = getOptionalAttribute(attributes, Project.PROJECTNAME_ATTRIBUTE_NAME);
                if (projectName != null) {
                    project.setProjectName(projectName);
                }
            } else {
                String outerElement = elementStack.get(elementStack.size() - 1);
                if (BUG_COLLECTION.equals(outerElement)) {

                    // Parsing a top-level element of the BugCollection
                    if ("BugInstance".equals(qName)) {
                        // BugInstance element - get required type and priority
                        // attributes
                        String type = getRequiredAttribute(attributes, "type", qName);
                        String priority = getRequiredAttribute(attributes, "priority", qName);

                        try {
                            int prio = Integer.parseInt(priority);
                            bugInstance = new BugInstance(type, prio);
                        } catch (NumberFormatException e) {
                            throw new SAXException("BugInstance with invalid priority value \"" + priority + "\"", e);
                        }

                        String firstVersion = getOptionalAttribute(attributes, "first");
                        if (firstVersion != null) {
                            bugInstance.setFirstVersion(Long.parseLong(firstVersion));
                        }
                        String lastVersion = getOptionalAttribute(attributes, "last");
                        if (lastVersion != null) {
                            bugInstance.setLastVersion(Long.parseLong(lastVersion));
                        }

                        if (bugInstance.isDead() && bugInstance.getFirstVersion() > bugInstance.getLastVersion()) {
                            throw new IllegalStateException("huh");
                        }

                        String introducedByChange = getOptionalAttribute(attributes, "introducedByChange");
                        if (introducedByChange != null) {
                            bugInstance.setIntroducedByChangeOfExistingClass(Boolean.parseBoolean(introducedByChange));
                        }
                        String removedByChange = getOptionalAttribute(attributes, "removedByChange");
                        if (removedByChange != null) {
                            bugInstance.setRemovedByChangeOfPersistingClass(Boolean.parseBoolean(removedByChange));
                        }
                        String oldInstanceHash = getOptionalAttribute(attributes, "instanceHash");
                        if (oldInstanceHash == null) {
                            oldInstanceHash = getOptionalAttribute(attributes, "oldInstanceHash");
                        }
                        if (oldInstanceHash != null) {
                            bugInstance.setOldInstanceHash(oldInstanceHash);
                        }

                        String firstSeen = getOptionalAttribute(attributes, "firstSeen");
                        if (firstSeen != null) {
                            try {
                                bugInstance.getXmlProps().setFirstSeen(BugInstance.firstSeenXMLFormat().parse(firstSeen));
                            } catch (ParseException e) {
                                LOGGER.warning("Could not parse first seen entry: " + firstSeen);
                                // ignore
                            }
                        }

                        String isInCloud = getOptionalAttribute(attributes, "isInCloud");
                        if (isInCloud != null) {
                            bugInstance.getXmlProps().setIsInCloud(Boolean.parseBoolean(isInCloud));
                        }

                        String reviewCount = getOptionalAttribute(attributes, "reviews");
                        if (reviewCount != null) {
                            bugInstance.getXmlProps().setReviewCount(Integer.parseInt(reviewCount));
                        }

                        String consensus = getOptionalAttribute(attributes, "consensus");
                        if (consensus != null) {
                            bugInstance.getXmlProps().setConsensus(consensus);
                        }

                    } else if ("FindBugsSummary".equals(qName)) {
                        BugCollection bugCollection = this.bugCollection;
                        assert bugCollection != null;
                        String timestamp = getRequiredAttribute(attributes, "timestamp", qName);
                        String vmVersion = getOptionalAttribute(attributes, "vm_version");
                        String totalClasses = getOptionalAttribute(attributes, "total_classes");
                        if (totalClasses != null && totalClasses.length() > 0) {
                            bugCollection.getProjectStats().setTotalClasses(Integer.parseInt(totalClasses));
                        }

                        String totalSize = getOptionalAttribute(attributes, "total_size");
                        if (totalSize != null && totalSize.length() > 0) {
                            bugCollection.getProjectStats().setTotalSize(Integer.parseInt(totalSize));
                        }

                        String referencedClasses = getOptionalAttribute(attributes, "referenced_classes");
                        if (referencedClasses != null && referencedClasses.length() > 0) {
                            bugCollection.getProjectStats().setReferencedClasses(Integer.parseInt(referencedClasses));
                        }
                        bugCollection.getProjectStats().setVMVersion(vmVersion);
                        try {
                            bugCollection.getProjectStats().setTimestamp(timestamp);
                        } catch (java.text.ParseException e) {
                            throw new SAXException("Unparseable sequence number: '" + timestamp + "'", e);
                        }
                    }
                } else if ("BugInstance".equals(outerElement)) {
                    parseBugInstanceContents(qName, attributes);
                } else if ("Method".equals(outerElement) || "Field".equals(outerElement) || "Class".equals(outerElement)
                        || "Type".equals(outerElement)) {
                    if ("SourceLine".equals(qName)) {
                        // package member elements can contain nested SourceLine
                        // elements.
                        bugAnnotationWithSourceLines.setSourceLines(createSourceLineAnnotation(qName, attributes));
                    }
                } else if (BugCollection.ERRORS_ELEMENT_NAME.equals(outerElement)) {
                    if (BugCollection.ANALYSIS_ERROR_ELEMENT_NAME.equals(qName) || BugCollection.ERROR_ELEMENT_NAME.equals(qName)) {
                        analysisError = new AnalysisError("Unknown error");
                        stackTrace.clear();
                    }
                } else if ("FindBugsSummary".equals(outerElement) && "PackageStats".equals(qName)) {
                    BugCollection bugCollection = this.bugCollection;
                    assert bugCollection != null;
                    String packageName = getRequiredAttribute(attributes, "package", qName);
                    int numClasses = Integer.parseInt(getRequiredAttribute(attributes, "total_types", qName));
                    int size = Integer.parseInt(getRequiredAttribute(attributes, "total_size", qName));
                    bugCollection.getProjectStats().putPackageStats(packageName, numClasses, size);

                } else if ("PackageStats".equals(outerElement)) {
                    BugCollection bugCollection = this.bugCollection;
                    assert bugCollection != null;
                    if ("ClassStats".equals(qName)) {
                        String className = getRequiredAttribute(attributes, "class", qName);
                        Boolean isInterface = Boolean.valueOf(getRequiredAttribute(attributes, "interface", qName));
                        int size = Integer.parseInt(getRequiredAttribute(attributes, "size", qName));
                        String sourceFile = getOptionalAttribute(attributes, "sourceFile");
                        bugCollection.getProjectStats().addClass(className, sourceFile, isInterface, size, false);
                    }

                } else if (isTopLevelFilter(outerElement) || isCompoundElementTag(outerElement)) {
                    parseMatcher(qName, attributes);
                } else if ("ClassFeatures".equals(outerElement)) {
                    if (ClassFeatureSet.ELEMENT_NAME.equals(qName)) {
                        String className = getRequiredAttribute(attributes, "class", qName);
                        classFeatureSet = new ClassFeatureSet();
                        classFeatureSet.setClassName(className);
                    }
                } else if (ClassFeatureSet.ELEMENT_NAME.equals(outerElement)) {
                    if (ClassFeatureSet.FEATURE_ELEMENT_NAME.equals(qName)) {
                        String value = getRequiredAttribute(attributes, "value", qName);
                        classFeatureSet.addFeature(value);
                    }
                } else if (BugCollection.HISTORY_ELEMENT_NAME.equals(outerElement)) {
                    if (AppVersion.ELEMENT_NAME.equals(qName)) {
                        BugCollection bugCollection = this.bugCollection;
                        assert bugCollection != null;

                        try {
                            String sequence = getRequiredAttribute(attributes, "sequence", qName);
                            String timestamp = getOptionalAttribute(attributes, "timestamp");
                            String releaseName = getOptionalAttribute(attributes, "release");
                            String codeSize = getOptionalAttribute(attributes, "codeSize");
                            String numClasses = getOptionalAttribute(attributes, "numClasses");
                            AppVersion appVersion = new AppVersion(Long.parseLong(sequence));
                            if (timestamp != null) {
                                appVersion.setTimestamp(Long.parseLong(timestamp));
                            }
                            if (releaseName != null) {
                                appVersion.setReleaseName(releaseName);
                            }
                            if (codeSize != null) {
                                appVersion.setCodeSize(Integer.parseInt(codeSize));
                            }
                            if (numClasses != null) {
                                appVersion.setNumClasses(Integer.parseInt(numClasses));
                            }

                            bugCollection.addAppVersion(appVersion);
                        } catch (NumberFormatException e) {
                            throw new SAXException("Invalid AppVersion element", e);
                        }
                    }
                } else if (BugCollection.PROJECT_ELEMENT_NAME.equals(outerElement)) {
                    Project project = this.project;
                    assert project != null;
                    if (Project.CLOUD_ELEMENT_NAME.equals(qName)) {
                        String cloudId = getRequiredAttribute(attributes, Project.CLOUD_ID_ATTRIBUTE_NAME, qName);
                        project.setCloudId(cloudId);
                        if(bugCollection != null){
                            Map<String,String> map = new HashMap<String, String>();
                            for (int i = 0; i < attributes.getLength(); i++) {
                                map.put(attributes.getLocalName(i), attributes.getValue(i));
                            }
                            bugCollection.setXmlCloudDetails(Collections.unmodifiableMap(map));
                        }
                    } else if (Project.PLUGIN_ELEMENT_NAME.equals(qName)) {
                        String pluginId = getRequiredAttribute(attributes, Project.PLUGIN_ID_ATTRIBUTE_NAME, qName);
                        Boolean enabled = Boolean.valueOf(getRequiredAttribute(attributes, Project.PLUGIN_STATUS_ELEMENT_NAME, qName));
                        project.setPluginStatusTrinary(pluginId, enabled);
                    }

                } else if (Project.CLOUD_ELEMENT_NAME.equals(outerElement)) {
                    if (Project.CLOUD_PROPERTY_ELEMENT_NAME.equals(qName)) {
                        cloudPropertyKey = getRequiredAttribute(attributes, "key", qName);
                    }

                }
            }
        }

        textBuffer.delete(0, textBuffer.length());
        elementStack.add(qName);
    }

    private boolean isCompoundElementTag(String qName) {
        return outerElementTags .contains(qName);
    }

    private boolean isTopLevelFilter(String qName) {
        return FIND_BUGS_FILTER.equals(qName) || "SuppressionFilter".equals(qName);
    }

    private void addMatcher(Matcher m) {
        if (m == null) {
            throw new IllegalArgumentException("matcher must not be null");
        }

        CompoundMatcher peek = matcherStack.peek();
        if (peek == null) {
            throw new NullPointerException("Top of stack is null");
        }
        peek.addChild(m);
        if (nextMatchedIsDisabled) {
            if (peek instanceof Filter) {
                ((Filter) peek).disable(m);
            } else {
                assert false;
            }
            nextMatchedIsDisabled = false;
        }
    }

    private void pushCompoundMatcherAsChild(CompoundMatcher m) {
        addMatcher(m);
        pushCompoundMatcher(m);
    }

    private void pushCompoundMatcher(CompoundMatcher m) {
        if (m == null) {
            throw new IllegalArgumentException("matcher must not be null");
        }
        matcherStack.push(m);
    }

    boolean nextMatchedIsDisabled;
    private final Set<String> outerElementTags = unmodifiableSet(new HashSet<String>(asList("And", "Match", "Or", "Not")));

    private void parseMatcher(String qName, Attributes attributes) throws SAXException {
        if (DEBUG) {
            System.out.println(elementStack + " " + qName + " " + matcherStack);
        }
        String disabled = getOptionalAttribute(attributes, "disabled");
        nextMatchedIsDisabled = "true".equals(disabled);
        if ("Bug".equals(qName)) {
            addMatcher(new BugMatcher(getOptionalAttribute(attributes, "code"), getOptionalAttribute(attributes, "pattern"),
                    getOptionalAttribute(attributes, "category")));
        } else if ("Class".equals(qName)) {
            String role = getOptionalAttribute(attributes, "role");
            addMatcher(new ClassMatcher(getRequiredAttribute(attributes, "name", qName), role));
        } else if ("Type".equals(qName)) {
            String role = getOptionalAttribute(attributes, "role");
            String typeParameters = getOptionalAttribute(attributes, "typeParameters");
            addMatcher(new TypeMatcher(getRequiredAttribute(attributes, "descriptor", qName), role, typeParameters));
        } else if ("FirstVersion".equals(qName)) {
            addMatcher(new FirstVersionMatcher(getRequiredAttribute(attributes, "value", qName), getRequiredAttribute(attributes,
                    "relOp", qName)));
        } else if ("LastVersion".equals(qName)) {
            addMatcher(new LastVersionMatcher(getRequiredAttribute(attributes, "value", qName), getRequiredAttribute(attributes,
                    "relOp", qName)));
        } else if ("Designation".equals(qName)) {
            addMatcher(new DesignationMatcher(getRequiredAttribute(attributes, "designation", qName)));
        } else if ("BugCode".equals(qName)) {
            addMatcher(new BugMatcher(getRequiredAttribute(attributes, "name", qName), "", ""));
        } else if ("Local".equals(qName)) {
            addMatcher(new LocalMatcher(getRequiredAttribute(attributes, "name", qName)));
        } else if ("BugPattern".equals(qName)) {
            addMatcher(new BugMatcher("", getRequiredAttribute(attributes, "name", qName), ""));
        } else if ("Priority".equals(qName)) {
            addMatcher(new PriorityMatcher(getRequiredAttribute(attributes, "value", qName)));
        } else if ("Confidence".equals(qName)) {
            addMatcher(new ConfidenceMatcher(getRequiredAttribute(attributes, "value", qName)));
        } else if ("Rank".equals(qName)) {
            addMatcher(new RankMatcher(getRequiredAttribute(attributes, "value", qName)));
        } else if ("Package".equals(qName)) {
            String pName = getRequiredAttribute(attributes, "name", qName);
            pName = pName.startsWith("~") ? pName : "~" + pName.replace(".", "\\.");
            addMatcher(new ClassMatcher(pName + "\\.[^.]+"));
        } else if ("Method".equals(qName)) {
            String name = getOptionalAttribute(attributes, "name");
            String params = getOptionalAttribute(attributes, "params");
            String returns = getOptionalAttribute(attributes, "returns");
            String role = getOptionalAttribute(attributes, "role");
            addMatcher(new MethodMatcher(name, params, returns, role));
        } else if ("Field".equals(qName)) {
            String name = getOptionalAttribute(attributes, "name");
            String type = getOptionalAttribute(attributes, "type");
            String role = getOptionalAttribute(attributes, "role");
            addMatcher(new FieldMatcher(name, type, role));
        } else if ("Or".equals(qName)) {
            CompoundMatcher matcher = new OrMatcher();
            pushCompoundMatcherAsChild(matcher);
        } else if ("And".equals(qName) || "Match".equals(qName)) {
            AndMatcher matcher = new AndMatcher();
            pushCompoundMatcherAsChild(matcher);
            if ("Match".equals(qName)) {
                String classregex = getOptionalAttribute(attributes, "classregex");
                String classMatch = getOptionalAttribute(attributes, "class");

                if (classregex != null) {
                    addMatcher(new ClassMatcher("~" + classregex));
                } else if (classMatch != null) {
                    addMatcher(new ClassMatcher(classMatch));
                }
            }
        } else if("Not".equals(qName)) {
            NotMatcher matcher = new NotMatcher();
            pushCompoundMatcherAsChild(matcher);
        } else if ("Source".equals(qName)) {
            addMatcher(new SourceMatcher(getRequiredAttribute(attributes, "name", qName)));
        }
        nextMatchedIsDisabled = false;
    }

    private void parseBugInstanceContents(String qName, Attributes attributes) throws SAXException {
        // Parsing an attribute or property of a BugInstance
        BugAnnotation bugAnnotation = null;
        if ("Class".equals(qName)) {
            String className = getRequiredAttribute(attributes, "classname", qName);
            bugAnnotation = bugAnnotationWithSourceLines = new ClassAnnotation(className);
        } else if ("Type".equals(qName)) {
            String typeDescriptor = getRequiredAttribute(attributes, "descriptor", qName);
            TypeAnnotation typeAnnotation;
            bugAnnotation = bugAnnotationWithSourceLines = typeAnnotation = new TypeAnnotation(typeDescriptor);
            String typeParameters = getOptionalAttribute(attributes, "typeParameters");
            if (typeParameters != null) {
                typeAnnotation.setTypeParameters(Strings.unescapeXml(typeParameters));
            }

        } else if ("Method".equals(qName) || "Field".equals(qName)) {
            String classname = getRequiredAttribute(attributes, "classname", qName);
            String fieldOrMethodName = getRequiredAttribute(attributes, "name", qName);
            String signature = getRequiredAttribute(attributes, "signature", qName);
            if ("Method".equals(qName)) {
                String isStatic = getOptionalAttribute(attributes, "isStatic");
                if (isStatic == null) {
                    isStatic = "false"; // Hack for old data
                }

                bugAnnotation = bugAnnotationWithSourceLines = new MethodAnnotation(classname, fieldOrMethodName, signature,
                        Boolean.valueOf(isStatic));

            } else {
                String isStatic = getRequiredAttribute(attributes, "isStatic", qName);
                String sourceSignature = getOptionalAttribute(attributes, "sourceSignature");
                bugAnnotation = bugAnnotationWithSourceLines = new FieldAnnotation(classname, fieldOrMethodName, signature,
                        sourceSignature, Boolean.valueOf(isStatic));
            }

        } else if ("SourceLine".equals(qName)) {
            SourceLineAnnotation sourceAnnotation = createSourceLineAnnotation(qName, attributes);
            if (!sourceAnnotation.isSynthetic()) {
                bugAnnotation = sourceAnnotation;
            }
        } else if ("Int".equals(qName)) {
            try {
                String value = getRequiredAttribute(attributes, "value", qName);
                bugAnnotation = new IntAnnotation(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new SAXException("Bad integer value in Int");
            }
        } else if ("String".equals(qName)) {
            String value = getRequiredAttribute(attributes, "value", qName);
            bugAnnotation = StringAnnotation.fromXMLEscapedString(value);
        } else if ("LocalVariable".equals(qName)) {
            try {
                String varName = getRequiredAttribute(attributes, "name", qName);
                int register = Integer.parseInt(getRequiredAttribute(attributes, "register", qName));
                int pc = Integer.parseInt(getRequiredAttribute(attributes, "pc", qName));
                bugAnnotation = new LocalVariableAnnotation(varName, register, pc);
            } catch (NumberFormatException e) {
                throw new SAXException("Invalid integer value in attribute of LocalVariable element");
            }
        } else if ("Property".equals(qName)) {
            // A BugProperty.
            String propName = getRequiredAttribute(attributes, "name", qName);
            String propValue = getRequiredAttribute(attributes, "value", qName);
            bugInstance.setProperty(propName, propValue);
        } else if ("UserAnnotation".equals(qName)) {
            // ignore AnnotationText for now; will handle in endElement
            String s = getOptionalAttribute(attributes, "designation"); // optional
            if (s != null) {
                bugInstance.setUserDesignationKey(s, null);
            }
            s = getOptionalAttribute(attributes, "user"); // optional
            if (s != null) {
                bugInstance.setUser(s);
            }
            s = getOptionalAttribute(attributes, "timestamp"); // optional
            if (s != null) {
                try {
                    long timestamp = Long.parseLong(s);
                    bugInstance.setUserAnnotationTimestamp(timestamp);
                } catch (NumberFormatException nfe) {
                    // ok to contine -- just won't set a timestamp for the user
                    // designation.
                    // but is there anyplace to report this?
                }
            }
            s = getOptionalAttribute(attributes, "needsSync"); // optional
            if (s == null || "false".equals(s)) {
                bugInstance.setUserAnnotationDirty(false);
            }

        } else {
            throw new SAXException("Unknown bug annotation named " + qName);
        }

        if (bugAnnotation != null) {
            setAnnotationRole(attributes, bugAnnotation);
            bugInstance.add(bugAnnotation);
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

    /*
     * Extract a hash value from an element.
     *
     * @param qName
     *            name of element containing hash value
     * @param attributes
     *            element attributes
     * @return the decoded hash value
     * @throws SAXException
     *
    private byte[] extractHash(String qName, Attributes attributes) throws SAXException {
        String encodedHash = getRequiredAttribute(attributes, "value", qName);
        byte[] hash;
        try {
            // System.out.println("Extract hash " + encodedHash);
            hash = ClassHash.stringToHash(encodedHash);
        } catch (IllegalArgumentException e) {
            throw new SAXException("Invalid class hash", e);
        }
        return hash;
    }*/

    private void setAnnotationRole(Attributes attributes, BugAnnotation bugAnnotation) {
        String role = getOptionalAttribute(attributes, "role");
        if (role != null) {
            bugAnnotation.setDescription(role);
        }
    }

    private SourceLineAnnotation createSourceLineAnnotation(String qName, Attributes attributes) throws SAXException {
        String classname = getRequiredAttribute(attributes, "classname", qName);
        String sourceFile = getOptionalAttribute(attributes, "sourcefile");
        if (sourceFile == null) {
            sourceFile = SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
        }
        String startLine = getOptionalAttribute(attributes, "start"); // "start"/"end"
        // are now
        // optional
        String endLine = getOptionalAttribute(attributes, "end"); // (were too
        // many "-1"s
        // in the xml)
        String startBytecode = getOptionalAttribute(attributes, "startBytecode");
        String endBytecode = getOptionalAttribute(attributes, "endBytecode");
        String synthetic = getOptionalAttribute(attributes, "synthetic");

        try {
            int sl = startLine != null ? Integer.parseInt(startLine) : -1;
            int el = endLine != null ? Integer.parseInt(endLine) : -1;
            int sb = startBytecode != null ? Integer.parseInt(startBytecode) : -1;
            int eb = endBytecode != null ? Integer.parseInt(endBytecode) : -1;

            SourceLineAnnotation s = new SourceLineAnnotation(classname, sourceFile, sl, el, sb, eb);
            if ("true".equals(synthetic)) {
                s.setSynthetic(true);
            }
            return s;
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
        } else if ("Project".equals(qName)) {
            // noop
        } else if (elementStack.size() > 1) {
            String outerElement = elementStack.get(elementStack.size() - 2);

            if (isTopLevelFilter(qName) || isCompoundElementTag(qName)) {
                if (DEBUG) {
                    System.out.println("  ending " + elementStack + " " + qName + " " + matcherStack);
                }

                matcherStack.pop();
            } else if (BUG_COLLECTION.equals(outerElement)) {
                BugCollection bugCollection = this.bugCollection;
                assert bugCollection != null;
                if ("BugInstance".equals(qName)) {
                    bugCollection.add(bugInstance, false);
                }
            } else if (PROJECT.equals(outerElement)) {
                Project project = this.project;
                assert project != null;
                if ("Jar".equals(qName)) {
                    project.addFile(makeAbsolute(getTextContents()));
                } else if ("SrcDir".equals(qName)) {
                    project.addSourceDir(makeAbsolute(getTextContents()));
                } else if ("AuxClasspathEntry".equals(qName)) {
                    project.addAuxClasspathEntry(makeAbsolute(getTextContents()));
                }



            } else if (Project.CLOUD_ELEMENT_NAME.equals(outerElement) && Project.CLOUD_PROPERTY_ELEMENT_NAME.equals(qName)) {
                Project project = this.project;
                assert project != null;
                assert cloudPropertyKey != null;
                project.getCloudProperties().setProperty(cloudPropertyKey, getTextContents());
                cloudPropertyKey = null;
            } else if ("BugInstance".equals(outerElement)) {
                if ("UserAnnotation".equals(qName)) {
                    bugInstance.setAnnotationText(getTextContents(), null);
                }
            } else if (BugCollection.ERRORS_ELEMENT_NAME.equals(outerElement)) {
                BugCollection bugCollection = this.bugCollection;
                assert bugCollection != null;
                if (BugCollection.ANALYSIS_ERROR_ELEMENT_NAME.equals(qName)) {
                    analysisError.setMessage(getTextContents());
                    bugCollection.addError(analysisError);
                } else if (BugCollection.ERROR_ELEMENT_NAME.equals(qName)) {
                    if (stackTrace.size() > 0) {
                        analysisError.setStackTrace(stackTrace.toArray(new String[stackTrace.size()]));
                    }
                    bugCollection.addError(analysisError);
                } else if (BugCollection.MISSING_CLASS_ELEMENT_NAME.equals(qName)) {
                    bugCollection.addMissingClass(getTextContents());
                }

            } else if (BugCollection.ERROR_ELEMENT_NAME.equals(outerElement)) {
                if (BugCollection.ERROR_MESSAGE_ELEMENT_NAME.equals(qName)) {
                    analysisError.setMessage(getTextContents());
                } else if (BugCollection.ERROR_EXCEPTION_ELEMENT_NAME.equals(qName)) {
                    analysisError.setExceptionMessage(getTextContents());
                } else if (BugCollection.ERROR_STACK_TRACE_ELEMENT_NAME.equals(qName)) {
                    stackTrace.add(getTextContents());
                }
            } else if ("ClassFeatures".equals(outerElement)) {
                if (ClassFeatureSet.ELEMENT_NAME.equals(qName)) {
                    BugCollection bugCollection = this.bugCollection;
                    assert bugCollection != null;

                    bugCollection.setClassFeatureSet(classFeatureSet);
                    classFeatureSet = null;
                }
            }
        }

        elementStack.remove(elementStack.size() - 1);
    }

    private String makeAbsolute(String possiblyRelativePath) {
        if (possiblyRelativePath.contains("://") || possiblyRelativePath.startsWith("http:")
                || possiblyRelativePath.startsWith("https:") || possiblyRelativePath.startsWith("file:")) {
            return possiblyRelativePath;
        }
        if (base == null) {
            return possiblyRelativePath;
        }
        if (new File(possiblyRelativePath).isAbsolute()) {
            return possiblyRelativePath;
        }

        return new File(base.getParentFile(), possiblyRelativePath).getAbsolutePath();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        textBuffer.append(ch, start, length);
    }

    private String getRequiredAttribute(Attributes attributes, String attrName, String elementName) throws SAXException {
        String value = attributes.getValue(attrName);
        if (value == null) {
            throw new SAXException(elementName + " element missing " + attrName + " attribute");
        }
        return memoized(Strings.unescapeXml(value));
    }

}
