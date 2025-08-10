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
 * Builder for creating a BugInstanceMatcher.
 * <p>
 * This class provides a fluent API to set various properties of the bug instance
 * such as bug type, class name, method name, field name, variable name, line number,
 * confidence level, and JSP file information.
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

    /**
     * Sets the bug type for the bug instance.
     *
     * @param bugType
     *            the type of the bug
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder bugType(String bugType) {
        this.bugType = bugType;
        return this;
    }

    /**
     * Sets the class name for the bug instance.
     *
     * @param className
     *            the class name
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder inClass(String className) {
        this.className = className;
        return this;
    }

    /**
     * Sets the method name for the bug instance.
     *
     * @param methodName
     *            the method name
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder inMethod(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * Sets the field name for the bug instance.
     *
     * @param fieldName
     *            the field name
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder atField(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    /**
     * Sets the variable name for the bug instance.
     *
     * @param variableName
     *            the variable name
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder atVariable(String variableName) {
        this.variableName = variableName;
        return this;
    }

    /**
     * Sets the line number for the bug instance.
     *
     * @param lineNumber
     *            the line number
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder atLine(int lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    /**
     * Sets the approximate line number for the bug instance.
     *
     * @param lineNumberApprox
     *            Line to verify accepting an offset of 1
     * @return this builder instance
     * @deprecated Use atJspLine for JSP line mapping
     */
    @Deprecated
    public BugInstanceMatcherBuilder atLineApprox(int lineNumberApprox) {
        this.lineNumberApprox = lineNumberApprox;
        return this;
    }

    /**
     * Sets the confidence level for the bug instance.
     *
     * @param confidence
     *            the confidence level
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder withConfidence(Confidence confidence) {
        this.confidence = confidence;
        return this;
    }

    /**
     * Sets the JSP file for the bug instance.
     *
     * @param jspFile
     *            the name of the JSP file
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder inJspFile(String jspFile) {
        this.jspFile = jspFile;
        return this;
    }

    /**
     * Sets the JSP line number for the bug instance.
     *
     * @param jspLine
     *            the line number in the JSP file
     *
     * @return this builder instance
     */
    public BugInstanceMatcherBuilder atJspLine(Integer jspLine) {
        this.jspLine = jspLine;
        return this;
    }

    /**
     * Builds a BugInstanceMatcher with the provided parameters.
     *
     * @return a new BugInstanceMatcher instance
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
