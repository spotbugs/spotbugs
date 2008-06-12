/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;


/**
 * Helper class for parsing command line arguments.
 */
public abstract class CommandLine {
	private List<String> optionList;
	private Set<String> unlistedOptions = new HashSet<String>();
	
	private Set<String> requiresArgumentSet;
	private Map<String, String> optionDescriptionMap;
	private Map<String, String> optionExtraPartSynopsisMap;
	private Map<String, String> argumentDescriptionMap;
	int maxWidth;

	public CommandLine() {
		this.optionList = new LinkedList<String>();
		this.requiresArgumentSet = new HashSet<String>();
		this.optionDescriptionMap = new HashMap<String, String>();
		this.optionExtraPartSynopsisMap = new HashMap<String, String>();
		this.argumentDescriptionMap = new HashMap<String, String>();
		this.maxWidth = 0;
	}

	/**
	 * Add a command line switch.
	 * This method is for adding options that do not require
	 * an argument.
	 *
	 * @param option      the option, must start with "-"
	 * @param description single line description of the option
	 */
	public void addSwitch(String option, String description) {
		optionList.add(option);
		optionDescriptionMap.put(option, description);

		if (option.length() > maxWidth)
			maxWidth = option.length();
	}

	/**
	 * Add a command line switch that allows optional extra
	 * information to be specified as part of it.
	 *
	 * @param option                  the option, must start with "-"
	 * @param optionExtraPartSynopsis synopsis of the optional extra information
	 * @param description             single-line description of the option
	 */
	public void addSwitchWithOptionalExtraPart(String option, String optionExtraPartSynopsis,
			String description) {
		optionList.add(option);
		optionExtraPartSynopsisMap.put(option, optionExtraPartSynopsis);
		optionDescriptionMap.put(option, description);

		// Option will display as -foo[:extraPartSynopsis]
		int length = option.length() + optionExtraPartSynopsis.length() + 3;
		if (length > maxWidth)
			maxWidth = length;
	}

	/**
	 * Add an option requiring an argument.
	 *
	 * @param option       the option, must start with "-"
	 * @param argumentDesc brief (one or two word) description of the argument
	 * @param description  single line description of the option
	 */
	public void addOption(String option, String argumentDesc, String description) {
		optionList.add(option);
		optionDescriptionMap.put(option, description);
		requiresArgumentSet.add(option);
		argumentDescriptionMap.put(option, argumentDesc);

		int width = option.length() + 3 + argumentDesc.length();
		if (width > maxWidth)
			maxWidth = width;
	}

	/**
	 * Don't list this option when printing Usage information
	 * @param option
	 */
	public void makeOptionUnlisted(String option) {
		unlistedOptions.add(option);
	}
	/**
	 * Expand option files in given command line.
	 * Any token beginning with "@" is assumed to be an option file.
	 * Option files contain one command line option per line.
	 *
	 * @param argv             the original command line
	 * @param ignoreComments   ignore comments (lines starting with "#")
	 * @param ignoreBlankLines ignore blank lines
	 * @return the expanded command line
	 */
	public static String[] expandOptionFiles(
				String[] argv, boolean ignoreComments, boolean ignoreBlankLines)
				throws IOException {
		ArrayList<String> resultList = new ArrayList<String>();

		for (String arg : argv) {
			if (!arg.startsWith("@")) {
				resultList.add(arg);
				continue;
			}

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(
						new FileInputStream(arg.substring(1)), Charset.forName("UTF-8")));
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();

					if (ignoreComments && line.startsWith("#"))
						continue;

					if (ignoreBlankLines && line.equals(""))
						continue;

					resultList.add(line);
				}
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ignore) {
						// Ignore
					}
				}
			}
		}

		return resultList.toArray(new String[resultList.size()]);
	}

	public static class HelpRequestedException extends Exception {

	}
	@SuppressWarnings("DM_EXIT")
	public int parse(String argv[], int minArgs, int maxArgs, String usage) {
		try {
		int count = parse(argv);
		int remaining = argv.length - count;
		if (remaining < minArgs || remaining > maxArgs) {
			System.out.println(usage);
			System.out.println("Expected " +minArgs + "..." + maxArgs 
					+ " file arguments, found " + remaining);
			System.out.println("Options:");
			printUsage(System.out);
			System.exit(1);
		}
		return count;
		} catch (HelpRequestedException e) {
			// fall through

		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(usage);
		System.out.println("Options:");
		printUsage(System.out);
		System.exit(1);
		return -1;
	}
	/**
	 * Parse a command line.
	 * Calls down to handleOption() and handleOptionWithArgument() methods.
	 * Stops parsing when it reaches the end of the command line,
	 * or when a command line argument not starting with "-" is seen.
	 *
	 * @param argv the arguments
	 * @return the number of arguments parsed; if equal to
	 *         argv.length, then the entire command line was parsed
	 * @throws HelpRequestedException 
	 */
	public int parse(String argv[]) throws IOException, HelpRequestedException {
		int arg = 0;

		while (arg < argv.length) {
			String option = argv[arg];
			if (option.equals("-help"))
				throw new HelpRequestedException();
			if (!option.startsWith("-"))
				break;

			String optionExtraPart = "";
			int colon = option.indexOf(':');
			if (colon >= 0) {
				optionExtraPart = option.substring(colon + 1);
				option = option.substring(0, colon);
			}

			if (optionDescriptionMap.get(option) == null)
				throw new IllegalArgumentException("Unknown option: " + option);

			if (requiresArgumentSet.contains(option)) {
				++arg;
				if (arg >= argv.length)
					throw new IllegalArgumentException("Option " + option + " requires an argument");
				String argument = argv[arg];
				handleOptionWithArgument(option, argument);
				++arg;
			} else {
				handleOption(option, optionExtraPart);
				++arg;
			}
		}

		return arg;
	}

	/**
	 * Callback method for handling an option.
	 *
	 * @param option          the option
	 * @param optionExtraPart the "extra" part of the option (everything after the
	 *                        colon: e.g., "withMessages" in "-xml:withMessages");
	 *                        the empty string if there was no extra part
	 */
	protected abstract void handleOption(String option, String optionExtraPart)
		throws IOException;

	/**
	 * Callback method for handling an option with an argument.
	 *
	 * @param option   the option
	 * @param argument the argument
	 */
	protected abstract void handleOptionWithArgument(String option, String argument) throws IOException;

	/**
	 * Print command line usage information to given stream.
	 *
	 * @param os the output stream
	 */
	public void printUsage(OutputStream os) {
		PrintStream out = new PrintStream(os);
		for (String option : optionList) {
			if (unlistedOptions.contains(option)) continue;
			out.print("  ");

			StringBuilder buf = new StringBuilder();
			buf.append(option);
			if (optionExtraPartSynopsisMap.get(option) != null) {
				String optionExtraPartSynopsis = optionExtraPartSynopsisMap.get(option);
				buf.append("[:");
				buf.append(optionExtraPartSynopsis);
				buf.append("]");
			}
			if (requiresArgumentSet.contains(option)) {
				buf.append(" <");
				buf.append(argumentDescriptionMap.get(option));
				buf.append(">");
			}
			printField(out, buf.toString(), maxWidth + 1);

			out.println(optionDescriptionMap.get(option));
		}
		out.flush();
	}

	private static final String SPACES = "                    ";

	private static void printField(PrintStream out, String s, int width) {
		if (s.length() > width) throw new IllegalArgumentException();
		int nSpaces = width - s.length();
		out.print(s);
		while (nSpaces > 0) {
			int n = Math.min(SPACES.length(), nSpaces);
			out.print(SPACES.substring(0, n));
			nSpaces -= n;
		}
	}
}

// vim:ts=3
