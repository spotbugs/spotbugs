package org.sonar.plugins.findbugs.resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class is a highly modified version of Michael Schierl's "SmapParser.java".
 * It was test with Jetty and WebLogic SMAP.
 *
 * =======
 *
 * SmapParser.java - Parse source debug extensions and
 * enhance stack traces.
 *
 * Copyright (c) 2012 Michael Schierl
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * - Neither name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND THE CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR THE CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Utility class to parse Source Debug Extensions and enhance stack traces.
 *
 * Note that only the first stratum is parsed and used.
 *
 * @author Michael Schierl
 */
public class SmapParser {

    /** The java filename. */
    private final String javaFilename;

    /** The fileinfo. */
    private final Map<Integer, FileInfo> fileinfo = new HashMap<>();

    /** The java 2 jsp. */
    private final Map<Integer, int[]> java2jsp = new HashMap<>();

    /** The Constant LINE_INFO_PATTERN. */
    private static final Pattern LINE_INFO_PATTERN = Pattern.compile("(\\d+)(?:#(\\d+))?(?:,(\\d+))?:(\\d+)(?:,(\\d+))?");

    /**
     * Gets the line.
     *
     * @param reader the reader
     * @return the line
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static String getLine(BufferedReader reader) throws IOException {
        String s = reader.readLine();
        if (s == null) {
            throw new IOException("EOF parsing SMAP");
        }
        return s;
    }

    /**
     * Instantiates a new smap parser.
     *
     * @param smap the smap
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "To keep backward compatibility")
    public SmapParser(String smap) throws IOException {
        //BufferedReader is use to support multiple types of line return
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(smap.getBytes())));

        String header = getLine(reader); //SMAP
        javaFilename = getLine(reader); //*****.java
        String jsp = getLine(reader); //JSP or alternative script
        String stratum = getLine(reader); //*S JSP
        String f = getLine(reader); //*F

        if (!header.equals("SMAP") || !stratum.startsWith("*S ") || !f.equals("*F")) {
            throw new IllegalArgumentException("Unexpected SMAP file format");
        }

        //Parse the file info section (*F)
        String line;
        while ((line = getLine(reader)) != null && !line.equals("*L")) {
            String path = null;
            if (line.startsWith("+ ")) {
                path = getLine(reader);
                line = line.substring(2);
            }

            int pos = line.indexOf(" ");
            int fileNum = Integer.parseInt(line.substring(0, pos));
            String name = line.substring(pos + 1);
            fileinfo.put(fileNum, new FileInfo(name, path == null ? name : path));
        }

        //Parse the line number mapping section (*L)
        int lastLFI = 0;
        while ((line = getLine(reader)) != null && !line.equals("*E")) {

            if (!line.startsWith("*")) {

                Matcher m = LINE_INFO_PATTERN.matcher(line);
                if (!m.matches()) {
                    throw new IllegalArgumentException(line);
                }

                int inputStartLine = Integer.parseInt(m.group(1));
                int lineFileID = m.group(2) == null ? lastLFI : Integer.parseInt(m.group(2));
                int repeatCount = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
                int outputStartLine = Integer.parseInt(m.group(4));
                int outputLineIncrement = m.group(5) == null ? 1 : Integer.parseInt(m.group(5));

                for (int i = 0; i < repeatCount; i++) {
                    int[] inputMapping = new int[] { lineFileID, inputStartLine + i };
                    int baseOL = outputStartLine + i * outputLineIncrement;
                    for (int ol = baseOL; ol < baseOL + outputLineIncrement; ol++) {
                        if (!java2jsp.containsKey(ol)) {
                            java2jsp.put(ol, inputMapping);
                        }
                    }
                }
                lastLFI = lineFileID;
            }
        }
    }

    /**
     * Gets the java filename.
     *
     * @return the java filename
     */
    public String getJavaFilename() {
        return javaFilename;
    }

    /**
     * Gets the script filename.
     *
     * @param fileIndex the file index
     * @return the script filename
     */
    public String getScriptFilename(int fileIndex) {
        FileInfo f = fileinfo.get(fileIndex);
        return f.name;
    }

    /**
     * Gets the script line number.
     *
     * @param lineNo the line no
     * @return the script line number
     */
    public int[] getScriptLineNumber(Integer lineNo) {
        return java2jsp.get(lineNo);
    }

    /**
     * Gets the java line numbers.
     *
     * @param jspLineNo the jsp line no
     * @return the java line numbers
     */
    public List<Integer> getJavaLineNumbers(Integer jspLineNo) {
        final List<Integer> javaLines = new ArrayList<>();
        for (final Map.Entry<Integer, int[]> lineMap : java2jsp.entrySet()) {
            if (lineMap.getValue()[1] == jspLineNo) {
                javaLines.add(lineMap.getKey());
            }
        }
        return javaLines;
    }

    /**
     * Gets the smap location.
     *
     * @param lineNo the line no
     * @return the smap location
     */
    public SmapLocation getSmapLocation(Integer lineNo) {
        int[] origSource = java2jsp.get(lineNo);
        FileInfo info = fileinfo.get(origSource[0]);
        return new SmapLocation(info, origSource[1], origSource[0] == 0);
    }

    /**
     * The Class FileInfo.
     */
    public static class FileInfo {

        /** The name. */
        public final String name;

        /** The path. */
        public final String path;

        /**
         * Instantiates a new file info.
         *
         * @param name the name
         * @param path the path
         */
        public FileInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    /**
     * The Class SmapLocation.
     */
    public static class SmapLocation {

        /** The file info. */
        public final FileInfo fileInfo;

        /** The line. */
        public final int line;

        /** The is primary file. */
        public final boolean isPrimaryFile;

        /**
         * Instantiates a new smap location.
         *
         * @param fileInfo the file info
         * @param line the line
         * @param isPrimaryFile the is primary file
         */
        public SmapLocation(FileInfo fileInfo, int line, boolean isPrimaryFile) {
            this.fileInfo = fileInfo;
            this.line = line;
            this.isPrimaryFile = isPrimaryFile;
        }

    }
}
