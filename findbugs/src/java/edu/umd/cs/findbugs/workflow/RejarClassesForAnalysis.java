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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.charsets.UserTextFile;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * @author William Pugh
 */
public class RejarClassesForAnalysis {
    static class RejarClassesForAnalysisCommandLine extends CommandLine {

        static class PatternMatcher {
            final Pattern[] pattern;

            PatternMatcher(String arg) {
                String[] p = arg.split(",");
                this.pattern = new Pattern[p.length];
                for (int i = 0; i < p.length; i++) {
                    pattern[i] = Pattern.compile(p[i]);
                }
            }

            public boolean matches(String arg) {
                for (Pattern p : pattern) {
                    if (p.matcher(arg).find()) {
                        return true;
                    }
                }

                return false;
            }
        }

        static class PrefixMatcher {
            final String[] prefixes;

            PrefixMatcher(String arg) {
                this.prefixes = arg.split(",");
            }

            PrefixMatcher() {
                this.prefixes = new String[0];
            }

            public boolean matches(String arg) {
                for (String p : prefixes) {
                    if (arg.startsWith(p)) {
                        return true;
                    }
                }

                return false;
            }

            public boolean matchesEverything() {
                for (String p : prefixes) {
                    if (p.length() == 0) {
                        return true;
                    }
                }
                return false;
            }
        }

        PrefixMatcher prefix = new PrefixMatcher("");

        PrefixMatcher exclude = new PrefixMatcher();

        PatternMatcher excludePatterns = null;

        int maxClasses = 29999;

        long maxAge = Long.MIN_VALUE;

        public String inputFileList;

        public String auxFileList;

        boolean onlyAnalyze = false;
        boolean ignoreTimestamps = false;

        RejarClassesForAnalysisCommandLine() {
            addSwitch("-analyzeOnly",  "only read the jars files and analyze them; don't produce new jar files");

            addOption("-maxAge", "days", "maximum age in days (ignore jar files older than this)");
            addOption("-inputFileList", "filename", "text file containing names of jar files");
            addOption("-auxFileList", "filename", "text file containing names of jar files for aux class path");

            addOption("-maxClasses", "num", "maximum number of classes per analysis*.jar file");
            addOption("-outputDir", "dir", "directory for the generated jar files");
            addSwitch("-ignoreTimestamps", "ignore timestamps on zip entries; use first version found");

            addOption("-prefix", "class name prefix",
                    "comma separated list of class name prefixes that should be analyzed (e.g., edu.umd.cs.)");
            addOption("-exclude", "class name prefix",
                    "comma separated list of class name prefixes that should be  excluded from both analyze and auxilary jar files (e.g., java.)");
            addOption("-excludePattern", "class name pattern(s)",
                    "comma separated list of regular expressions; all classes matching them are excluded");

        }

        File outputDir = new File(".");

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String,
         * java.lang.String)
         */
        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            if ("-analyzeOnly".equals(option)) {
                onlyAnalyze = true;
            } else  if ("-ignoreTimestamps".equals(option)) {
                ignoreTimestamps = true;
            } else {
                throw new IllegalArgumentException("Unknown option : " + option);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java
         * .lang.String, java.lang.String)
         */
        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
            if ("-prefix".equals(option)) {
                prefix = new PrefixMatcher(argument);
            } else if ("-exclude".equals(option)) {
                exclude = new PrefixMatcher(argument);
            } else if ("-inputFileList".equals(option)) {
                inputFileList = argument;
            } else if ("-auxFileList".equals(option)) {
                auxFileList = argument;
            } else if ("-maxClasses".equals(option)) {
                maxClasses = Integer.parseInt(argument);
            } else if ("-maxAge".equals(option)) {
                maxAge = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) * Integer.parseInt(argument);
            } else if ("-outputDir".equals(option)) {
                outputDir = new File(argument);
            } else if ("-excludePattern".equals(option)) {
                excludePatterns = new PatternMatcher(argument);
            } else {
                throw new IllegalArgumentException("Unknown option : " + option);
            }
        }

        boolean skip(ZipEntry ze) {
            return ze.getSize() > 1000000;
        }
    }

    final RejarClassesForAnalysisCommandLine commandLine;

    final int argCount;

    final String[] args;

    public RejarClassesForAnalysis(RejarClassesForAnalysisCommandLine commandLine, int argCount, String[] args) {
        this.commandLine = commandLine;
        this.argCount = argCount;
        this.args = args;
    }

    public static void readFromStandardInput(Collection<String> result) throws IOException {
        readFrom(result, UserTextFile.bufferedReader(System.in));
    }

    SortedMap<String, ZipOutputStream> analysisOutputFiles = new TreeMap<String, ZipOutputStream>();

    public @Nonnull
    ZipOutputStream getZipOutputFile(String path) {
        ZipOutputStream result = analysisOutputFiles.get(path);
        if (result != null) {
            return result;
        }
        SortedMap<String, ZipOutputStream> head = analysisOutputFiles.headMap(path);
        String matchingPath = head.lastKey();
        result = analysisOutputFiles.get(matchingPath);
        if (result == null) {
            throw new IllegalArgumentException("No zip output file for " + path);
        }
        return result;
    }

    public static void readFrom(Collection<String> result, @WillClose Reader r) throws IOException {
        BufferedReader in = new BufferedReader(r);
        while (true) {
            String s = in.readLine();
            if (s == null) {
                in.close();
                return;
            }
            result.add(s);
        }

    }

    int analysisCount = 1;

    int auxilaryCount = 1;

    String getNextAuxilaryFileOutput() {
        String result;
        if (auxilaryCount == 1) {
            result = "auxilary.jar";
        } else {
            result = "auxilary" + (auxilaryCount) + ".jar";
        }
        auxilaryCount++;
        System.out.println("Starting " + result);
        return result;
    }

    String getNextAnalyzeFileOutput() {
        String result;
        if (analysisCount == 1) {
            result = "analyze.jar";
        } else {
            result = "analyze" + (analysisCount) + ".jar";
        }
        analysisCount++;
        System.out.println("Starting " + result);
        return result;
    }

    /** For each file, give the latest timestamp */
    Map<String, Long> copied = new HashMap<String, Long>();
    /** While file should we copy it from */
    Map<String, File> copyFrom = new HashMap<String, File>();

    Set<String> excluded = new HashSet<String>();

    TreeSet<String> filesToAnalyze = new TreeSet<String>();

    int numFilesToAnalyze = 0;

    public static void main(String args[]) throws Exception {
        FindBugs.setNoAnalysis();
        RejarClassesForAnalysisCommandLine commandLine = new RejarClassesForAnalysisCommandLine();
        int argCount = commandLine.parse(args, 0, Integer.MAX_VALUE, "Usage: " + RejarClassesForAnalysis.class.getName()
                + " [options] [<jarFile>+] ");
        RejarClassesForAnalysis doit = new RejarClassesForAnalysis(commandLine, argCount, args);
        doit.execute();
    }

    int auxilaryClassCount = 0;

    ZipOutputStream auxilaryOut;

    final byte buffer[] = new byte[8192];

    private boolean exclude(String dottedName) {
        if (!Character.isJavaIdentifierStart(dottedName.charAt(0))) {
            return true;
        }
        if (commandLine.excludePatterns != null && commandLine.excludePatterns.matches(dottedName)
                || commandLine.exclude.matches(dottedName)) {
            excluded.add(dottedName);
            return true;
        }
        return false;
    }

    boolean classFileFound;
    public void execute() throws IOException {

        ArrayList<String> fileList = new ArrayList<String>();

        if (commandLine.inputFileList != null) {
            readFrom(fileList, UTF8.fileReader(commandLine.inputFileList));
        } else if (argCount == args.length) {
            readFromStandardInput(fileList);
        } else {
            fileList.addAll(Arrays.asList(args).subList(argCount, args.length));
        }
        ArrayList<String> auxFileList = new ArrayList<String>();
        if (commandLine.auxFileList != null) {
            readFrom(auxFileList, UTF8.fileReader(commandLine.auxFileList));
            auxFileList.removeAll(fileList);
        }

        List<File> inputZipFiles = new ArrayList<File>(fileList.size());
        List<File> auxZipFiles = new ArrayList<File>(auxFileList.size());
        for (String fInName : fileList) {
            final File f = new File(fInName);
            if (f.lastModified() < commandLine.maxAge) {
                System.err.println("Skipping " + fInName + ", too old (" + new Date(f.lastModified()) + ")");
                continue;
            }

            int oldSize = copied.size();
            classFileFound = false;
            if (processZipEntries(f, new ZipElementHandler() {
                boolean checked = false;

                @Override
                public void handle(ZipFile file, ZipEntry ze) throws IOException {
                    if (commandLine.skip(ze)) {
                        return;
                    }
                    String name = ze.getName();

                    String dottedName = name.replace('/', '.');
                    if (exclude(dottedName)) {
                        return;
                    }

                    if (!checked) {
                        checked = true;
                        if (embeddedNameMismatch(file, ze)) {
                            System.out.println("Class name mismatch for " + name + " in " + file.getName());
                            throw new ClassFileNameMismatch();
                        }
                    }
                    if (!commandLine.prefix.matches(dottedName)) {
                        return;
                    }
                    classFileFound = true;
                    long timestamp = ze.getTime();
                    Long oldTimestamp = copied.get(name);
                    if (oldTimestamp == null) {
                        copied.put(name, timestamp);
                        copyFrom.put(name, f);
                        filesToAnalyze.add(name);
                        numFilesToAnalyze++;
                    } else if (!commandLine.ignoreTimestamps && oldTimestamp < timestamp) {
                        System.out.printf("Found later version of %s; switching from %s to %s%n", name, copyFrom.get(name), f);
                        copied.put(name, timestamp);
                        copyFrom.put(name, f);
                    }
                }

                /**
                 * @param dottedName
                 * @return
                 */

            }) && oldSize < copied.size()) {
                inputZipFiles.add(f);
            } else if (classFileFound) {
                System.err.println("Skipping " + fInName  + ", no new classes found");
            } else {
                System.err.println("Skipping " + fInName  + ", no classes found");
            }
        }
        for (String fInName : auxFileList) {
            final File f = new File(fInName);
            if (f.lastModified() < commandLine.maxAge) {
                System.err.println("Skipping " + fInName + ", too old (" + new Date(f.lastModified()) + ")");
                continue;
            }
            int oldSize = copied.size();
            classFileFound = false;
            if (processZipEntries(f, new ZipElementHandler() {
                @Override
                public void handle(ZipFile file, ZipEntry ze) {
                    if (commandLine.skip(ze)) {
                        return;
                    }

                    String name = ze.getName();
                    String dottedName = name.replace('/', '.');
                    if (!exclude(dottedName)) {
                        classFileFound = true;
                        long timestamp = ze.getTime();
                        Long oldTimestamp = copied.get(name);
                        if (oldTimestamp == null) {
                            copied.put(name, timestamp);
                            copyFrom.put(name, f);
                        } else if (!commandLine.ignoreTimestamps && oldTimestamp < timestamp) {
                            copied.put(name, timestamp);
                            copyFrom.put(name, f);
                        }
                    }
                }
            }) && oldSize < copied.size()) {
                auxZipFiles.add(f);
            } else if (classFileFound) {
                System.err.println("Skipping aux file " + fInName  + ", no new classes found");
            } else {
                System.err.println("Skipping aux file" + fInName  + ", no classes found");
            }
        }

        System.out.printf("    # Zip/jar files: %2d%n", inputZipFiles.size());
        System.out.printf("# aux Zip/jar files: %2d%n", auxZipFiles.size());
        System.out.printf("Unique class files: %6d%n", copied.size());
        if (numFilesToAnalyze != copied.size()) {
            System.out.printf("  files to analyze: %6d%n", numFilesToAnalyze);
        }

        if (!excluded.isEmpty()) {
            System.out.printf("   excluded  files: %6d%n", excluded.size());
        }

        if (commandLine.onlyAnalyze) {
            return;
        }

        if (numFilesToAnalyze < copied.size() || numFilesToAnalyze > commandLine.maxClasses) {
            auxilaryOut = createZipFile(getNextAuxilaryFileOutput());
        }

        int count = Integer.MAX_VALUE;
        String oldBaseClass = "x x";
        String oldPackage = "x x";
        for (String path : filesToAnalyze) {
            int lastSlash = path.lastIndexOf('/');
            String packageName = lastSlash <= 0 ? "" : path.substring(0, lastSlash - 1);
            int firstDollar = path.indexOf('$', lastSlash);
            String baseClass = firstDollar < 0 ? path : path.substring(0, firstDollar - 1);
            boolean switchOutput;
            if (count > commandLine.maxClasses) {
                switchOutput = true;
            } else if (count + 50 > commandLine.maxClasses && !baseClass.equals(oldBaseClass)) {
                switchOutput = true;
            } else if (count + 250 > commandLine.maxClasses && !packageName.equals(oldPackage)) {
                switchOutput = true;
            } else {
                switchOutput = false;
            }

            if (switchOutput) {
                // advance
                String zipFileName = getNextAnalyzeFileOutput();
                analysisOutputFiles.put(path, createZipFile(zipFileName));
                System.out.printf("%s%n -> %s%n", path, zipFileName);
                count = 0;
            }
            count++;
            oldPackage = packageName;
            oldBaseClass = baseClass;
        }

        for (File f : inputZipFiles) {
            System.err.println("Reading " + f);
            final File ff = f;
            processZipEntries(f, new ZipElementHandler() {

                @Override
                public void handle(ZipFile zipInputFile, ZipEntry ze) throws IOException {
                    if (commandLine.skip(ze)) {
                        return;
                    }


                    String name = ze.getName();
                    String dottedName = name.replace('/', '.');
                    if (exclude(dottedName)) {
                        return;
                    }
                    if (!ff.equals(copyFrom.get(name))) {
                        return;
                    }
                    if (name.contains("DefaultProblem.class")) {
                        System.out.printf("%40s %40s%n", name, ff);
                    }

                    boolean writeToAnalyzeOut = false;
                    boolean writeToAuxilaryOut = false;
                    if (commandLine.prefix.matches(dottedName)) {
                        writeToAnalyzeOut = true;
                        if (numFilesToAnalyze > commandLine.maxClasses) {
                            writeToAuxilaryOut = true;
                        }
                    } else {
                        writeToAuxilaryOut = auxilaryOut != null;
                    }
                    ZipOutputStream out = null;
                    if (writeToAnalyzeOut) {
                        out = getZipOutputFile(name);
                        out.putNextEntry(newZipEntry(ze));
                    }

                    if (writeToAuxilaryOut) {
                        auxilaryClassCount++;
                        if (auxilaryClassCount > 29999) {
                            auxilaryClassCount = 0;
                            advanceAuxilaryOut();
                        }
                        auxilaryOut.putNextEntry(newZipEntry(ze));
                    }

                    copyEntry(zipInputFile, ze, writeToAnalyzeOut, out, writeToAuxilaryOut, auxilaryOut);
                }

            });
        }

        for (File f : auxZipFiles) {
            final File ff = f;
            System.err.println("Opening aux file " + f);
            processZipEntries(f, new ZipElementHandler() {

                @Override
                public void handle(ZipFile zipInputFile, ZipEntry ze) throws IOException {
                    if (commandLine.skip(ze)) {
                        return;
                    }

                    String name = ze.getName();
                    String dottedName = name.replace('/', '.');

                    if (exclude(dottedName)) {
                        return;
                    }
                    if (!ff.equals(copyFrom.get(name))) {
                        return;
                    }

                    auxilaryClassCount++;
                    if (auxilaryClassCount > 29999) {
                        auxilaryClassCount = 0;
                        advanceAuxilaryOut();
                    }
                    auxilaryOut.putNextEntry(newZipEntry(ze));

                    copyEntry(zipInputFile, ze, false, null, true, auxilaryOut);
                }

            });
        }

        if (auxilaryOut != null) {
            auxilaryOut.close();
        }
        for (ZipOutputStream out : analysisOutputFiles.values()) {
            out.close();
        }

        System.out.println("All done");
    }

    private ZipOutputStream createZipFile(String fileName) throws FileNotFoundException {
        File newFile = new File(commandLine.outputDir, fileName);
        return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(newFile)));
    }


    private boolean embeddedNameMismatch(ZipFile zipInputFile, ZipEntry ze) throws IOException {
        InputStream zipIn = zipInputFile.getInputStream(ze);
        String name = ze.getName();
        JavaClass j = new ClassParser(zipIn, name).parse();
        zipIn.close();
        String className = j.getClassName();
        String computedFileName = ClassName.toSlashedClassName(className)+".class";
        if (name.charAt(0) == '1') {
            System.out.println(name);
            System.out.println("  " + className);
        }
        if (computedFileName.equals(name)) {
            return false;
        }
        System.out.println("In " + name + " found " + className);
        return true;
    }

    private void copyEntry(ZipFile zipInputFile, ZipEntry ze, boolean writeToAnalyzeOut, ZipOutputStream analyzeOut1,
            boolean writeToAuxilaryOut, ZipOutputStream auxilaryOut1) throws IOException {
        InputStream zipIn = zipInputFile.getInputStream(ze);

        while (true) {
            int bytesRead = zipIn.read(buffer);
            if (bytesRead < 0) {
                break;
            }
            if (writeToAnalyzeOut) {
                analyzeOut1.write(buffer, 0, bytesRead);
            }
            if (writeToAuxilaryOut) {
                auxilaryOut1.write(buffer, 0, bytesRead);
            }
        }
        if (writeToAnalyzeOut) {
            analyzeOut1.closeEntry();
        }
        if (writeToAuxilaryOut) {
            auxilaryOut1.closeEntry();
        }
        zipIn.close();
    }

    private void advanceAuxilaryOut() throws IOException, FileNotFoundException {
        auxilaryOut.close();
        auxilaryOut = createZipFile(getNextAuxilaryFileOutput());
    }

    boolean processZipEntries(File f, ZipElementHandler handler) {
        if (!f.exists()) {
            System.out.println("file not found: '" + f + "'");
            return false;
        }
        if (!f.canRead() || f.isDirectory()) {
            System.out.println("not readable: '" + f + "'");
            return false;
        }
        if (!f.canRead() || f.isDirectory()) {
            System.out.println("not readable: '" + f + "'");
            return false;
        }
        if (f.length() == 0) {
            System.out.println("empty zip file: '" + f + "'");
            return false;
        }
        ZipFile zipInputFile;
        try {
            zipInputFile = new ZipFile(f);
            for (Enumeration<? extends ZipEntry> e = zipInputFile.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                if (!ze.isDirectory() && ze.getName().endsWith(".class") && ze.getSize() != 0) {
                    handler.handle(zipInputFile, ze);
                }

            }
            zipInputFile.close();
        } catch (ClassFileNameMismatch e) {
            return false;
        } catch (IOException e) {
            System.out.println("Error processing '" + f + "'");
            return false;
        }
        return true;

    }


    public ZipEntry newZipEntry(ZipEntry ze) {
        ZipEntry ze2 = new ZipEntry(ze.getName());
        ze2.setComment(ze.getComment());
        ze2.setTime(ze.getTime());
        ze2.setExtra(ze.getExtra());
        return ze2;
    }


    static class ClassFileNameMismatch extends IOException {

    }
    interface ZipElementHandler {
        void handle(ZipFile file, ZipEntry ze) throws IOException;
    }
}
