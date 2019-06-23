/**
 * Find Security Bugs
 * Copyright (c) Philippe Arteau, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package edu.umd.cs.findbugs.test.matcher;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.Confidence;

public class BugInstanceMatcher extends BaseMatcher<BugInstance> {

    private static final Pattern ANON_FUNCTION_SCALA_PATTERN = Pattern.compile("\\$\\$anonfun\\$([^\\$]+)\\$");

    private final String bugType;
    private final String className;
    private final String methodName;
    private final String fieldName;
    private final String variableName;
    private final Integer lineNumber;
    private final Integer lineNumberApprox;
    private final Confidence confidence;
    private final String jspFile;
    private final List<Integer> multipleChoicesLine;

    /**
     * All the parameters are optional. Only the non-null parameters are used.
     *
     * @param bugType
     *            Expected bug type
     * @param className
     *            Class name
     * @param methodName
     *            Method name
     * @param fieldName
     *            Field name
     * @param variableName
     *            Variable name
     * @param lineNumber
     *            Line number
     * @param lineNumberApprox
     *            Approximate line for test samples that are unstable (Historically the JSP samples)
     * @param confidence
     *            Confidence
     * @param jspFile
     *            JSP file name
     * @param multipleChoicesLine
     *            At least of the line (JSP samples specific)
     */
    public BugInstanceMatcher(String bugType, String className, String methodName, String fieldName,
            String variableName, Integer lineNumber, Integer lineNumberApprox, Confidence confidence, String jspFile,
            List<Integer> multipleChoicesLine) {
        this.bugType = bugType;
        this.className = className;
        this.methodName = methodName;
        this.fieldName = fieldName;
        this.variableName = variableName;
        this.lineNumber = lineNumber;
        this.lineNumberApprox = lineNumberApprox;
        this.confidence = confidence;
        this.jspFile = jspFile;
        this.multipleChoicesLine = multipleChoicesLine;
    }

    @SuppressWarnings("boxing")
    @Override
    public boolean matches(Object obj) {
        if (obj instanceof BugInstance) {
            BugInstance bugInstance = (BugInstance) obj;

            boolean criteriaMatches = true;
            if (bugType != null) {
                criteriaMatches &= bugInstance.getType().equals(bugType);
            }
            if (confidence != null) {
                criteriaMatches &= bugInstance.getPriority() == confidence.getConfidenceValue();
            }
            if (className != null) {
                ClassAnnotation classAnn = extractBugAnnotation(bugInstance, ClassAnnotation.class);
                if (classAnn == null) {
                    return false;
                }

                String fullName = classAnn.getClassName();
                int startDot = fullName.lastIndexOf(".") + 1;
                int endDollar = fullName.indexOf('$');
                String simpleName = fullName.substring(startDot != -1 ? startDot : 0, endDollar != -1 ? endDollar : fullName.length());
                String simpleNameInner = fullName.substring(startDot != -1 ? startDot : 0, fullName.length());
                criteriaMatches &= fullName.equals(className) || simpleName.equals(className) || simpleNameInner.equals(className);
            }
            if (methodName != null) {
                MethodAnnotation methodAnn = extractBugAnnotation(bugInstance, MethodAnnotation.class);
                ClassAnnotation classAnn = extractBugAnnotation(bugInstance, ClassAnnotation.class);
                String fullClassName = classAnn.getClassName();
                if (methodAnn == null) {
                    return false;
                }

                if (methodAnn.getMethodName().startsWith("apply") && fullClassName != null) {
                    Matcher m = ANON_FUNCTION_SCALA_PATTERN.matcher(fullClassName);
                    if (m.find()) { //Scala function enclose in
                        criteriaMatches &= methodAnn.getMethodName().equals(methodName) || methodName.equals(m.group(1));
                    }
                } else { //
                    criteriaMatches &= methodAnn.getMethodName().equals(methodName);
                }
            }
            if (fieldName != null) {
                FieldAnnotation fieldAnn = extractBugAnnotation(bugInstance, FieldAnnotation.class);
                if (fieldAnn == null) {
                    return false;
                }
                criteriaMatches &= fieldAnn.getFieldName().equals(fieldName);
            }
            if (variableName != null) {
                LocalVariableAnnotation localVarAnn = extractBugAnnotation(bugInstance, LocalVariableAnnotation.class);
                if (localVarAnn == null) {
                    return false;
                }
                criteriaMatches &= localVarAnn.getName().equals(variableName);
            }
            if (lineNumber != null) {
                SourceLineAnnotation srcAnn = extractBugAnnotation(bugInstance, SourceLineAnnotation.class);
                if (srcAnn == null) {
                    return false;
                }
                criteriaMatches &= srcAnn.getStartLine() <= lineNumber && lineNumber <= srcAnn.getEndLine();
            }
            if (lineNumberApprox != null) {
                SourceLineAnnotation srcAnn = extractBugAnnotation(bugInstance, SourceLineAnnotation.class);
                if (srcAnn == null) {
                    return false;
                }
                criteriaMatches &= srcAnn.getStartLine() - 1 <= lineNumberApprox && lineNumberApprox <= srcAnn.getEndLine() + 1;
            }
            if (jspFile != null) {
                ClassAnnotation classAnn = extractBugAnnotation(bugInstance, ClassAnnotation.class);
                String fullName = classAnn.getClassName().replaceAll("\\.", "/").replaceAll("_005f", "_").replaceAll("_jsp", ".jsp");
                //String simpleName = fullName.substring(fullName.lastIndexOf("/") + 1);
                criteriaMatches &= fullName.endsWith(jspFile);
            }
            if (multipleChoicesLine != null) {
                SourceLineAnnotation srcAnn = extractBugAnnotation(bugInstance, SourceLineAnnotation.class);
                if (srcAnn == null) {
                    return false;
                }
                boolean found = false;
                for (Integer potentialMatch : multipleChoicesLine) {
                    if (srcAnn.getStartLine() - 1 <= potentialMatch && potentialMatch <= srcAnn.getEndLine() + 1) {
                        found = true;
                    }
                }
                //if(!found) {
                //log.info("The bug was between lines "+srcAnn.getStartLine()+" and "+srcAnn.getEndLine());
                //}
                criteriaMatches &= found;
            }
            return criteriaMatches;
        }
        return false;
    }

    private static <T> T extractBugAnnotation(BugInstance bugInstance, Class<T> annotationType) {
        for (BugAnnotation annotation : bugInstance.getAnnotations()) {
            if (annotation.getClass().equals(annotationType)) {
                return annotationType.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("BugInstance with:\n");
        if (bugType != null) {
            description.appendText("bugType=").appendValue(bugType).appendText(",");
        }
        if (className != null) {
            description.appendText("className=").appendValue(className).appendText(",");
        }
        if (methodName != null) {
            description.appendText("methodName=").appendValue(methodName).appendText(",");
        }
        if (fieldName != null) {
            description.appendText("fieldName=").appendValue(fieldName).appendText(",");
        }
        if (variableName != null) {
            description.appendText("variableName=").appendValue(variableName).appendText(",");
        }
        if (lineNumber != null) {
            description.appendText("lineNumber=").appendValue(lineNumber);
        }
    }
}
