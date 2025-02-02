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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.sonar.plugins.findbugs.resource.SmapParser;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.service.ClassFileLocator;

/**
 * DSL to build BugInstanceMatcher
 */
public class BugInstanceMatcherBuilder {

    private String bugType;
    private String className;
    private String methodName;
    private String fieldName;
    private String variableName;
    private Integer lineNumber;
    private Integer lineNumberApprox;
    private Confidence confidence;
    private String jspFile;
    private Integer jspLine;

    public BugInstanceMatcherBuilder bugType(String bugType) {
        this.bugType = bugType;
        return this;
    }

    public BugInstanceMatcherBuilder inClass(String className) {
        this.className = className;
        return this;
    }

    public BugInstanceMatcherBuilder inMethod(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public BugInstanceMatcherBuilder atField(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public BugInstanceMatcherBuilder atVariable(String variableName) {
        this.variableName = variableName;
        return this;
    }

    public BugInstanceMatcherBuilder atLine(int lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    /**
     * @deprecated Use atJspLine for JSP line mapping
     * @param lineNumberApprox
     *            Line to verify accepting an offset of 1
     * @return this
     */
    @Deprecated
    public BugInstanceMatcherBuilder atLineApprox(int lineNumberApprox) {
        this.lineNumberApprox = lineNumberApprox;
        return this;
    }

    /**
     * Define the priority of the detector
     *
     * @param confidence
     *            The desired confidence
     * @return this
     */
    public BugInstanceMatcherBuilder withConfidence(Confidence confidence) {
        this.confidence = confidence;
        return this;
    }

    public BugInstanceMatcherBuilder inJspFile(String jspFile) {
        this.jspFile = jspFile;
        return this;
    }

    public BugInstanceMatcherBuilder atJspLine(Integer jspLine) {
        this.jspLine = jspLine;
        return this;
    }

    /**
     * @return Hamcrest Matcher
     */
    public BugInstanceMatcher build() {
        //JSP line to Java source conversion
        List<Integer> multipleChoicesLine = null;
        if (jspLine != null) {
            if (jspFile != null) {
                //Map JSP lines to Java base on the smap file if available
                multipleChoicesLine = mapJspToJavaLine(jspFile, jspLine);
            } else {
                throw new RuntimeException("JSP file not set.");
            }
        }

        return new BugInstanceMatcher(bugType, className, methodName, fieldName, variableName, lineNumber,
                lineNumberApprox, confidence, jspFile, multipleChoicesLine);
    }

    private static List<Integer> mapJspToJavaLine(final String jspFile, final Integer jspLine) {
        final ClassFileLocator locator = new ClassFileLocator();
        final File smapFile = new File(locator.getJspFilePath(jspFile) + ".smap");
        if (!smapFile.exists()) {
            throw new RuntimeException("SMAP File are missing. (" + smapFile + ")");
        }
        try {
            //Convert
            final String contents = new String(Files.readAllBytes(smapFile.toPath()), StandardCharsets.UTF_8);
            final SmapParser smapParser = new SmapParser(contents);
            final List<Integer> javaLineNumbers = smapParser.getJavaLineNumbers(jspLine);
            if (javaLineNumbers.isEmpty()) {
                throw new RuntimeException("Unable to find the mapping for the JSP line " + jspLine);
            }

            return javaLineNumbers;
        } catch (IOException e) {
            throw new RuntimeException("Unable to open the smap file.", e);
        }
    }

}
