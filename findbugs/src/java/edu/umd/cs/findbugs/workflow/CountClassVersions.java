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

import java.security.MessageDigest;
import java.util.zip.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;

import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.util.DualKeyHashMap;
import edu.umd.cs.findbugs.workflow.RejarClassesForAnalysis.RejarClassesForAnalysisCommandLine;

/**
 * @author William Pugh
 */
public class CountClassVersions {

	public static List<String> readFromStandardInput() throws IOException {
		return readFrom(new InputStreamReader(System.in));
	}

	public static List<String> readFrom(Reader r) throws IOException {
		BufferedReader in = new BufferedReader(r);
		List<String> lst = new LinkedList<String>();
		while (true) {
			String s = in.readLine();
			if (s == null)
				return lst;
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String,
		 *      java.lang.String)
		 */
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			throw new IllegalArgumentException("Unknown option : " + option);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String,
		 *      java.lang.String)
		 */
		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			if (option.equals("-prefix"))
				prefix = argument;
			else if (option.equals("-inputFileList"))
				inputFileList = argument;
			else if (option.equals("-maxAge"))
				maxAge = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) * Integer.parseInt(argument);
			else
				throw new IllegalArgumentException("Unknown option : " + option);
		}

	}

	public static void main(String args[]) throws Exception {
		CountClassVersionsCommandLine commandLine = new CountClassVersionsCommandLine();
		int argCount = commandLine.parse(args, 0, Integer.MAX_VALUE, "Usage: " + CountClassVersions.class.getName()
		        + " [options] [<jarFile>+] ");

		int analysisClassCount = 0;
		Set<String> copied = new HashSet<String>();
		List<String> fileList;

		if (commandLine.inputFileList != null)
			fileList = readFrom(new FileReader(commandLine.inputFileList));
		else if (argCount == args.length)
			fileList = readFromStandardInput();
		else
			fileList = Arrays.asList(args).subList(argCount, args.length - 1);
		byte buffer[] = new byte[8192];
		MessageDigest digest = MessageDigest.getInstance("MD5");
		DualKeyHashMap<String, String, String> map = new DualKeyHashMap<String, String, String>();

		for (String fInName : fileList) {
			File f = new File(fInName);
			if (f.lastModified() < commandLine.maxAge) {
				System.err.println("Skipping " + fInName + ", too old (" + new Date(f.lastModified()) + ")");
				continue;
			}
			System.err.println("Opening " + f);
			ZipFile zipInputFile;
			try {
				zipInputFile = new ZipFile(f);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			for (Enumeration<? extends ZipEntry> e = zipInputFile.entries(); e.hasMoreElements();) {
				ZipEntry ze = e.nextElement();

				if (ze == null)
					break;
				if (ze.isDirectory())
					continue;

				String name = ze.getName();
				if (!name.endsWith(".class"))
					continue;
				if (!name.replace('/', '.').startsWith(commandLine.prefix))
					continue;

				InputStream zipIn = zipInputFile.getInputStream(ze);

				while (true) {
					int bytesRead = zipIn.read(buffer);
					if (bytesRead < 0)
						break;
					digest.update(buffer, 0, bytesRead);

				}
				String hash = new BigInteger(1, digest.digest()).toString(16);
				map.put(name, hash, fInName);
			}
			zipInputFile.close();
		}
		for (String s : map.keySet()) {
			Map<String, String> values = map.get(s);
			if (values.size() > 1) {
				System.out.println(values.size() + "\t" + s + "\t" + values.values());
			}

		}
	}
}
