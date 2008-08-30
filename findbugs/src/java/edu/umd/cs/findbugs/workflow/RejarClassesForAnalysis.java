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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import edu.umd.cs.findbugs.config.CommandLine;

/**
 * @author William Pugh
 */
public class RejarClassesForAnalysis {
	static class RejarClassesForAnalysisCommandLine extends CommandLine {
		public String prefix = "";

		int maxClasses = 29999;

		long maxAge = Long.MIN_VALUE;

		public String inputFileList;

		public String auxFileList;

		RejarClassesForAnalysisCommandLine() {
			addOption("-maxAge", "days", "maximum age in days (ignore jar files older than this)");
			addOption("-inputFileList", "filename", "text file containing names of jar files");
			addOption("-auxFileList", "filename", "text file containing names of jar files for aux class path");

			addOption("-maxClasses", "num", "maximum number of classes per analysis*.jar file");
			addOption("-prefix", "class name prefix", "prefix of class names that should be analyzed (e.g., edu.umd.cs.)");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String,
		 * java.lang.String)
		 */
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			throw new IllegalArgumentException("Unknown option : " + option);
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
			if (option.equals("-prefix"))
				prefix = argument;
			else if (option.equals("-inputFileList"))
				inputFileList = argument;
			else if (option.equals("-auxFileList"))
				auxFileList = argument;
			else if (option.equals("-maxClasses"))
				maxClasses = Integer.parseInt(argument);
			else if (option.equals("-maxAge"))
				maxAge = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) * Integer.parseInt(argument);
			else
				throw new IllegalArgumentException("Unknown option : " + option);
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

	public static List<String> readFromStandardInput() throws IOException {
		return readFrom(new InputStreamReader(System.in));
	}

	public static List<String> readFrom(Reader r) throws IOException {
		BufferedReader in = new BufferedReader(r);
		List<String> lst = new LinkedList<String>();
		while (true) {
			String s = in.readLine();
			if (s == null) {
				in.close();
				return lst;
			}
			lst.add(s);
		}

	}

	int analysisCount = 1;

	int auxilaryCount = 1;

	String getNextAuxilaryFileOutput() {
		String result;
		if (auxilaryCount == 1)
			result = "auxilary.jar";
		else
			result = "auxilary" + (auxilaryCount) + ".jar";
		auxilaryCount++;
		System.out.println("Starting " + result);
		return result;
	}

	String getNextAnalyzeFileOutput() {
		String result;
		if (analysisCount == 1)
			result = "analyze.jar";
		else
			result = "analyze" + (analysisCount) + ".jar";
		analysisCount++;
		System.out.println("Starting " + result);
		return result;
	}

	Set<String> copied = new HashSet<String>();

	int filesToAnalyze = 0;

	public static void main(String args[]) throws Exception {
		RejarClassesForAnalysisCommandLine commandLine = new RejarClassesForAnalysisCommandLine();
		int argCount = commandLine.parse(args, 0, Integer.MAX_VALUE, "Usage: " + RejarClassesForAnalysis.class.getName()
		        + " [options] [<jarFile>+] ");
		RejarClassesForAnalysis doit = new RejarClassesForAnalysis(commandLine, argCount, args);
		doit.execute();
	}

	int analysisClassCount = 0;

	int auxilaryClassCount = 0;

	ZipOutputStream analyzeOut;

	ZipOutputStream auxilaryOut;

	final byte buffer[] = new byte[8192];

	public void execute() throws IOException {

		analyzeOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(getNextAnalyzeFileOutput())));

		List<String> fileList;

		if (commandLine.inputFileList != null)
			fileList = readFrom(new FileReader(commandLine.inputFileList));
		else if (argCount == args.length)
			fileList = readFromStandardInput();
		else
			fileList = Arrays.asList(args).subList(argCount, args.length);
		List<String> auxFileList = Collections.emptyList();
		if (commandLine.auxFileList != null)
			auxFileList = readFrom(new FileReader(commandLine.auxFileList));

		List<File> inputZipFiles = new ArrayList<File>(fileList.size());
		List<File> auxZipFiles = new ArrayList<File>(auxFileList.size());
		for (String fInName : fileList) {
			File f = new File(fInName);
			if (f.lastModified() < commandLine.maxAge) {
				System.err.println("Skipping " + fInName + ", too old (" + new Date(f.lastModified()) + ")");
				continue;
			}

			int oldSize = copied.size();
			if (processZipEntries(f, new ZipElementHandler() {
				public void handle(ZipFile file, ZipEntry ze) {
					if (copied.add(ze.getName()) && ze.getName().replace('/', '.').startsWith(commandLine.prefix))
						filesToAnalyze++;
				}
			}) && oldSize < copied.size())
				inputZipFiles.add(f);
		}
		for (String fInName : auxFileList) {
			File f = new File(fInName);
			if (f.lastModified() < commandLine.maxAge) {
				System.err.println("Skipping " + fInName + ", too old (" + new Date(f.lastModified()) + ")");
				continue;
			}
			int oldSize = copied.size();

			if (processZipEntries(f, new ZipElementHandler() {
				public void handle(ZipFile file, ZipEntry ze) {
					copied.add(ze.getName());
				}
			}) && oldSize < copied.size())
				auxZipFiles.add(f);
		}

		System.out.println("# Zip/jar files: " + inputZipFiles.size());
		System.out.println("# aux Zip/jar files: " + auxZipFiles.size());
		if (filesToAnalyze == copied.size())

			System.out.println("Unique class files: " + filesToAnalyze);
		else {
			System.out.println("Unique class files: " + copied.size());
			System.out.println("  files to analyze: " + filesToAnalyze);
		}

		if (commandLine.prefix.length() > 0 || filesToAnalyze > commandLine.maxClasses)
			auxilaryOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
			        getNextAuxilaryFileOutput())));

		copied.clear();

		for (File f : inputZipFiles) {
			System.err.println("Opening " + f);
			processZipEntries(f, new ZipElementHandler() {

				public void handle(ZipFile zipInputFile, ZipEntry ze) throws IOException {
					String name = ze.getName();
					if (!copied.add(name)) {
						System.err.println("Skipping duplicate of " + name);
						return;

					}
					boolean writeToAnalyzeOut = false;
					boolean writeToAuxilaryOut = false;
					if (name.replace('/', '.').startsWith(commandLine.prefix)) {
						writeToAnalyzeOut = true;
						if (filesToAnalyze > commandLine.maxClasses)
							writeToAuxilaryOut = true;
						analysisClassCount++;
						if (analysisClassCount > commandLine.maxClasses) {
							advanceAnalyzeOut();
						}

					} else
						writeToAuxilaryOut = auxilaryOut != null;

					if (writeToAnalyzeOut)
						analyzeOut.putNextEntry(new ZipEntry(name));
					if (writeToAuxilaryOut) {
						auxilaryClassCount++;
						if (auxilaryClassCount > commandLine.maxClasses) {
							auxilaryClassCount = 0;
							advanceAuxilaryOut();
						}
						auxilaryOut.putNextEntry(new ZipEntry(name));
					}

					copyEntry(zipInputFile, ze, writeToAnalyzeOut, writeToAuxilaryOut);
				}

			});
		}

		for (File f : auxZipFiles) {
			System.err.println("Opening aux file " + f);
			processZipEntries(f, new ZipElementHandler() {

				public void handle(ZipFile zipInputFile, ZipEntry ze) throws IOException {
					String name = ze.getName();
					if (!copied.add(name)) {
						System.err.println("Skipping duplicate of " + name);
						return;
					}
					auxilaryClassCount++;
					if (auxilaryClassCount > commandLine.maxClasses) {
						auxilaryClassCount = 0;
						advanceAuxilaryOut();
					}
					auxilaryOut.putNextEntry(new ZipEntry(name));

					copyEntry(zipInputFile, ze, false, true);
				}

			});
		}

		analyzeOut.close();
		if (auxilaryOut != null)
			auxilaryOut.close();

	}

	private void copyEntry(ZipFile zipInputFile, ZipEntry ze, boolean writeToAnalyzeOut, boolean writeToAuxilaryOut)
	        throws IOException {
		InputStream zipIn = zipInputFile.getInputStream(ze);

		while (true) {
			int bytesRead = zipIn.read(buffer);
			if (bytesRead < 0)
				break;
			if (writeToAnalyzeOut)
				analyzeOut.write(buffer, 0, bytesRead);
			if (writeToAuxilaryOut)
				auxilaryOut.write(buffer, 0, bytesRead);
		}
		if (writeToAnalyzeOut)
			analyzeOut.closeEntry();
		if (writeToAuxilaryOut)
			auxilaryOut.closeEntry();
		zipIn.close();
	}

	private void advanceAuxilaryOut() throws IOException, FileNotFoundException {
		auxilaryOut.close();
		auxilaryOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(getNextAuxilaryFileOutput())));
	}

	private void advanceAnalyzeOut() throws IOException, FileNotFoundException {
		analysisClassCount = 0;
		analyzeOut.close();
		analyzeOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(getNextAnalyzeFileOutput())));
	}

	boolean processZipEntries(File f, ZipElementHandler handler) {
		ZipFile zipInputFile;
		try {
			zipInputFile = new ZipFile(f);
			for (Enumeration<? extends ZipEntry> e = zipInputFile.entries(); e.hasMoreElements();) {
				ZipEntry ze = e.nextElement();
				if (!ze.isDirectory() && ze.getName().endsWith(".class"))
					handler.handle(zipInputFile, ze);

			}
			zipInputFile.close();
		} catch (IOException e) {
			e.printStackTrace(System.out);
			return false;
		}
		return true;

	}

	interface ZipElementHandler {
		void handle(ZipFile file, ZipEntry ze) throws IOException;
	}
}
