/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import edu.umd.cs.findbugs.charsets.SourceCharset;

/**
 * @author pugh
 */
public class FixIndentation {
    static final String SPACES = "                                                                                                                                            ";

    public static void main(String args[]) throws Exception {
        recursiveFix(new File(args[0]), true);
    }

    static void recursiveFix(File root, boolean partial) throws IOException {
        String rootPath = root.getAbsolutePath();
        Set<File> seen = new HashSet<File>();
        Queue<File> todo = new LinkedList<File>();
        seen.add(root);
        todo.add(root);
        while (!todo.isEmpty()) {
            File next = todo.remove().getAbsoluteFile();
            String nextPath = next.getAbsolutePath();
            if (!nextPath.startsWith(rootPath))
                continue;

            if (next.isDirectory()) {
                File[] contents = next.listFiles();
                if (contents != null)
                    for (File c : contents)
                        if (seen.add(c))
                            todo.add(c);
            } else if (nextPath.endsWith(".java") || nextPath.endsWith(".xml"))
                fix(next, partial);
        }
    }

    static String fix(String s) {
        if (s.length() == 0)
            return s;
        if (s.trim().length() == 0)
            return "";
        int spaces = 0;
        int pos = 0;
        int indentation = 0;
        for (; pos < s.length(); pos++) {
            char c = s.charAt(pos);
            if (c == ' ') {
                indentation++;
                spaces++;
            } else if (c == '\t') {
                indentation += 4;
            } else
                break;
        }

        if (pos >= s.length())
            return "";
        return SPACES.substring(0, indentation) + s.substring(pos).trim();

    }

    static void fix(File fileToUpdate, boolean partial) throws IOException {
        boolean anyChanges = false;
        BufferedReader in = new BufferedReader(new FileReader(fileToUpdate));
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        int consecutiveFixes = 0;
        try {
            while (true) {
                String s = in.readLine();
                if (s == null)
                    break;
                String s2 = fix(s);
                if (!s2.equals(s)) {
                    consecutiveFixes++;
                    if (consecutiveFixes > 3 && partial) {
                        s2 = s;
                        consecutiveFixes = 0;
                    } else
                        anyChanges = true;
                } else
                    consecutiveFixes = 0;
                out.println(s2);
            }
        } finally {
            in.close();
        }
        if (!anyChanges)
            return;
        StringReader stringReader = new StringReader(stringWriter.toString());
        Writer outFile = SourceCharset.fileWriter(fileToUpdate);
        char[] buffer = new char[1000];
        try {
            while (true) {
                int sz = stringReader.read(buffer);
                if (sz < 0)
                    break;
                outFile.write(buffer, 0, sz);
            }
        } finally {
            outFile.close();
        }
        System.out.println("Updated " + fileToUpdate);
    }

}
