/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.charsets.UserTextFile;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.util.DualKeyHashMap;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author William Pugh
 */
public class CountClassVersions {

    public static List<String> readFromStandardInput() throws IOException {
        return readFrom(UserTextFile.reader(System.in));
    }

    public static List<String> readFrom(Reader r) throws IOException {
        BufferedReader in = new BufferedReader(r);
        List<String> lst = new LinkedList<String>();
        while (true) {
            String s = in.readLine();
            if (s == null) {
                return lst;
            }
            lst.add(s);
        }
    }

    static class CountClassVersionsCommandLine extends CommandLine {
        public String prefix = "";

        public String inputFileList;

        long maxAge = Long.MIN_VALUE;

        CountClassVersionsCommandLine() {
            addOption("-maxAge", "days", "maximum age in days (ignore jar files older than this");
            addOption("-inputFileList", "filename", "text file containing names of jar files");

            addOption("-prefix", "class name prefix", "prefix of class names that should be analyzed e.g., edu.umd.cs.)");
        }

        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            throw new IllegalArgumentException("Unknown option : " + option);
        }

        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
            if ("-prefix".equals(option)) {
                prefix = argument;
            } else if ("-inputFileList".equals(option)) {
                inputFileList = argument;
            } else if ("-maxAge".equals(option)) {
                maxAge = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) * Integer.parseInt(argument);
            } else {
                throw new IllegalArgumentException("Unknown option : " + option);
            }
        }

    }

    public static void main(String args[]) throws Exception {
        FindBugs.setNoAnalysis();
        CountClassVersionsCommandLine commandLine = new CountClassVersionsCommandLine();
        int argCount = commandLine.parse(args, 0, Integer.MAX_VALUE, "Usage: " + CountClassVersions.class.getName()
                + " [options] [<jarFile>+] ");

        List<String> fileList;

        if (commandLine.inputFileList != null) {
            fileList = readFrom(UTF8.fileReader(commandLine.inputFileList));
        } else if (argCount == args.length) {
            fileList = readFromStandardInput();
        } else {
            fileList = Arrays.asList(args).subList(argCount, args.length - 1);
        }
        byte buffer[] = new byte[8192];
        MessageDigest digest = Util.getMD5Digest();
        DualKeyHashMap<String, String, String> map = new DualKeyHashMap<String, String, String>();

        for (String fInName : fileList) {
            File f = new File(fInName);
            if (f.lastModified() < commandLine.maxAge) {
                System.err.println("Skipping " + fInName + ", too old (" + new Date(f.lastModified()) + ")");
                continue;
            }
            System.err.println("Opening " + f);

            try (ZipFile zipInputFile = new ZipFile(f)){
                for (Enumeration<? extends ZipEntry> e = zipInputFile.entries(); e.hasMoreElements();) {
                    ZipEntry ze = e.nextElement();

                    if (ze == null) {
                        break;
                    }
                    if (ze.isDirectory()) {
                        continue;
                    }

                    String name = ze.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    if (!name.replace('/', '.').startsWith(commandLine.prefix)) {
                        continue;
                    }

                    InputStream zipIn = zipInputFile.getInputStream(ze);

                    while (true) {
                        int bytesRead = zipIn.read(buffer);
                        if (bytesRead < 0) {
                            break;
                        }
                        digest.update(buffer, 0, bytesRead);

                    }
                    String hash = new BigInteger(1, digest.digest()).toString(16);
                    map.put(name, hash, fInName);
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
        for (String s : map.keySet()) {
            Map<String, String> values = map.get(s);
            if (values.size() > 1) {
                System.out.println(values.size() + "\t" + s + "\t" + values.values());
            }

        }
    }
}
