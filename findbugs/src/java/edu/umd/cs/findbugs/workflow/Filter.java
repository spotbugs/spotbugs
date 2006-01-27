/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.filter.Matcher;

/**
 * Java main application to filter/transform an XML bug collection
 * or bug history collection.
 *
 * @author William Pugh
 */
public class Filter {
	static SourceFinder sourceFinder = new SourceFinder();
	static class FilterCommandLine extends CommandLine {
		Pattern className,bugPattern;
		public boolean notSpecified = false;
		public boolean not = false;
		long first;
		String firstAsString; 
		long after;
		String afterAsString;  
		long before;
		String beforeAsString; 
		
		long last;
		String lastAsString; 
		long present;
		String presentAsString; 
		long absent;
		String absentAsString; 
		String annotation;
		public boolean activeSpecified = false;
		public boolean active = false;
	
		public boolean withSource = false;
		public boolean withSourceSpecified = false;
		public boolean introducedByChange = false;
		public boolean introducedByChangeSpecified = false;

		public boolean removedByChange = false;
		public boolean removedByChangeSpecified = false;
		
		public boolean newCode = false;
		public boolean newCodeSpecified = false;

		public boolean removedCode = false;
		public boolean removedCodeSpecified = false;

		public boolean classified = false;
		public boolean classifiedSpecified = false;


		public boolean withMessagesSpecified = false;
		public boolean withMessages = false;
		public boolean serious = false;
		public boolean seriousSpecified = false;
		
		private Matcher includeFilter, excludeFilter;
		String category;
		int priority = 3;

		FilterCommandLine() {
			
			addSwitch("-not", "reverse (all) switches for the filter");
			addSwitchWithOptionalExtraPart("-withSource", "truth", "only warnings for switch source is available");
			addOption("-exclude", "filter file", "exclude bugs matching given filter");
			addOption("-include", "filter file", "include only bugs matching given filter");
			
			addOption("-annotation", "text", "allow only warnings containing this text in an annotation");
			addSwitchWithOptionalExtraPart("-classified", "truth", "allow only classified warnings");
			addSwitchWithOptionalExtraPart("-withMessages", "truth", "generated XML should contain textual messages");
			
			addSwitchWithOptionalExtraPart("-serious", "truth", "allow only warnings classified as serious");
			
			addOption("-after", "when", "allow only warnings that first occurred after this version");
			addOption("-before", "when", "allow only warnings that first occurred before this version");
			addOption("-first", "when", "allow only warnings that first occurred in this version");
			addOption("-last", "when", "allow only warnings that last occurred in this version");
			addOption("-present", "when", "allow only warnings present in this version");
			addOption("-absent", "when", "allow only warnings absent in this version");
			addSwitchWithOptionalExtraPart("-active", "truth", "allow only warnings alive in the last sequence number");
			
			addSwitchWithOptionalExtraPart("-introducedByChange", "truth",
					"allow only warnings introduced by a change of an existing class");
			addSwitchWithOptionalExtraPart("-removedByChange", "truth",
					"allow only warnings removed by a change of a persisting class");
			addSwitchWithOptionalExtraPart("-newCode", "truth",
			"allow only warnings introduced by the addition of a new class");
			addSwitchWithOptionalExtraPart("-removedCode", "truth",
			"allow only warnings removed by removal of a class");
			addOption("-priority", "level", "allow only warnings with this priority or higher");
			addOption("-class", "pattern", "allow only bugs whose primary class name matches this pattern");
			addOption("-bugPattern", "pattern", "allow only bugs whose type matches this pattern");
			addOption("-category", "category", "allow only warnings with a category that starts with this string");
			
			
		}

		static long getVersionNum(Map<String, AppVersion> versions, 
				SortedMap<Long, AppVersion> timeStamps , String val, boolean roundToLaterVersion) {
			if (val == null) return -1;
				AppVersion v = versions.get(val);
				if (v != null) return v.getSequenceNumber();
				try {
				long time = Date.parse(val);
				return getAppropriateSeq(timeStamps, time, roundToLaterVersion);
			} catch (IllegalArgumentException e) {
				try {
					return Long.parseLong(val);
				}
				catch (NumberFormatException e1) {
					throw new IllegalArgumentException("Could not interpret version specification of '" + val + "'");
				}
			}
		}
		
		// timeStamps contains 0 10 20 30
		// if roundToLater == true, ..0 = 0, 1..10 = 1, 11..20 = 2, 21..30 = 3, 31.. = Long.MAX
		// if roundToLater == false, ..-1 = Long.MIN, 0..9 = 0, 10..19 = 1, 20..29 = 2, 30..39 = 3, 40 .. = 4
		static private long getAppropriateSeq(SortedMap<Long, AppVersion> timeStamps, long when, boolean roundToLaterVersion) {
			if (roundToLaterVersion) {
				SortedMap<Long, AppVersion> geq = timeStamps.tailMap(when);
				if (geq.isEmpty()) return Long.MAX_VALUE;
				return geq.get(geq.firstKey()).getSequenceNumber();
			} else {
				SortedMap<Long, AppVersion> leq = timeStamps.headMap(when);
				if (leq.isEmpty()) return Long.MIN_VALUE;
				return leq.get(leq.lastKey()).getSequenceNumber();
			}
		}

		void adjustFilter(BugCollection collection) {
			Map<String, AppVersion> versions = new HashMap<String, AppVersion>();
			SortedMap<Long, AppVersion> timeStamps = new TreeMap<Long, AppVersion>();

			for(Iterator<AppVersion> i = collection.appVersionIterator(); i.hasNext(); ) {
				AppVersion v = i.next();
				versions.put(v.getReleaseName(), v);
				timeStamps.put(v.getTimestamp(), v);
			}
			first = getVersionNum(versions, timeStamps, firstAsString, true);
			last = getVersionNum(versions, timeStamps, lastAsString, true);
			before = getVersionNum(versions, timeStamps, beforeAsString, true);
			after = getVersionNum(versions, timeStamps, afterAsString, false);
			present = getVersionNum(versions, timeStamps, presentAsString, true);
			absent = getVersionNum(versions, timeStamps, absentAsString, true);
			
		}
		boolean accept(BugInstance bug) {
			boolean result = evaluate(bug);
			if (not) return !result;
			return result;
		}
		HashSet<String> sourceFound = new HashSet<String>();
		HashSet<String> sourceNotFound = new HashSet<String>();
		
		boolean findSource(SourceLineAnnotation srcLine) {
			if (srcLine == null) return false;
			String sourceFile = srcLine.getSourceFile();
			if (sourceFile != null && !sourceFile.equals("<Unknown>")) {
				
				String cName = srcLine.getClassName();
				if (sourceFound.contains(cName)) return true;
				if (sourceNotFound.contains(cName)) return false;
				try {
					InputStream in = sourceFinder.openSource(srcLine.getPackageName(), sourceFile);
					in.close();
					sourceFound.add(cName);
					return true;
				} catch (IOException e) {
					assert true; // ignore it -- couldn't find source file
					sourceNotFound.add(cName);
				}
			}
			return false;
		}
		boolean evaluate(BugInstance bug) {

			if (includeFilter != null && !includeFilter.match(bug)) return false;
			if (excludeFilter != null && excludeFilter.match(bug)) return false;
			if (annotation != null && bug.getAnnotationText().indexOf(annotation) == -1)
				return false;
			if (bug.getPriority() > priority)
				return false;
			if (firstAsString != null && bug.getFirstVersion() != first)
				return false;
			if (afterAsString != null && bug.getFirstVersion() <= after)
				return false;
			if (beforeAsString != null && bug.getFirstVersion() >= before)
				return false;
			if (lastAsString != null && bug.getLastVersion() != last)
				return false;
			if (presentAsString != null && !bugLiveAt(bug, present))
				return false;
			if (absentAsString != null && bugLiveAt(bug, absent))
				return false;
			
			if (activeSpecified && active != (bug.getLastVersion() == -1))
				return false;
			if (removedByChangeSpecified
					&& bug.isRemovedByChangeOfPersistingClass() != removedByChange)
				return false;
			if (introducedByChangeSpecified
					&& bug.isIntroducedByChangeOfExistingClass() != introducedByChange)
				return false;
			if (newCodeSpecified && newCode != (!bug.isIntroducedByChangeOfExistingClass() && bug.getFirstVersion() != 0))
				return false;
			if (removedCodeSpecified && removedCode != (!bug.isRemovedByChangeOfPersistingClass() && bug.getLastVersion() != -1))
				return false;

			if (bugPattern != null && !bugPattern.matcher(bug.getType()).find())
					return false;
			if (className != null && !className.matcher(bug.getPrimaryClass().getClassName()).find())
					return false;

			if (category != null && !bug.getBugPattern().getCategory().startsWith(category))
				return false;
			
			if (withSourceSpecified) {
				if (findSource(bug.getPrimarySourceLineAnnotation()) != withSource) 
					return false;
			}

			if (classifiedSpecified && classified != isClassified(bug)) {
				return false;
			}

			if (seriousSpecified) {
				Set<String> words = bug.getTextAnnotationWords();
				boolean thisOneIsSerious = words.contains("BUG")
				&& !(words.contains("NOT_BUG") || words.contains("HARMLESS"));
				if (serious != thisOneIsSerious) return false;
			}

			return true;
		}

		private boolean isClassified(BugInstance bug) {
			Set<String> words = bug.getTextAnnotationWords();
			return words.contains("BUG") || words.contains("NOT_BUG");
		}

		private boolean bugLiveAt(BugInstance bug, long now) {
			if (now < bug.getFirstVersion())
				return false;
			if (bug.getLastVersion() != -1 && bug.getLastVersion() < now)
				return false;
			return true;
		}

		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			option = option.substring(1);
			if (optionExtraPart.length() == 0)
				setField(option, true);
			else
				setField(option, TigerSubstitutes.parseBoolean(optionExtraPart));
			setField(option+"Specified", true);
		}

		private void setField(String fieldName, boolean value) {
			try {
			Field f = FilterCommandLine.class.getField(fieldName);
			f.setBoolean(this, value);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		}
		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {

			if (option.equals("-priority")) {
				priority = parsePriority(argument);
			}

			
			else if (option.equals("-first")) 
				firstAsString = argument;
			else if (option.equals("-last")) 
				lastAsString = argument;
			else if (option.equals("-after")) 
				afterAsString = argument;
			else if (option.equals("-before")) 
				beforeAsString = argument;
			else if (option.equals("-present")) 
				presentAsString = argument;
			else if (option.equals("-absent")) 
				absentAsString = argument;
			
			else if (option.equals("-category"))
				category = argument;
			else if (option.equals("-class"))
					className = Pattern.compile(argument);
			else if (option.equals("-bugPattern"))
					bugPattern = Pattern.compile(argument);
			else if (option.equals("-annotation"))
				annotation = argument;
			else if (option.equals("-include")) {
				try {
					includeFilter = new edu.umd.cs.findbugs.filter.Filter(argument);
				} catch (FilterException e) {
					throw new IllegalArgumentException("Error processing include file: " + argument, e);
				}
			} else if (option.equals("-exclude")) {
				try {
					excludeFilter = new edu.umd.cs.findbugs.filter.Filter(argument);
				} catch (FilterException e) {
					throw new IllegalArgumentException("Error processing include file: " + argument, e);
				}
			} else throw new IllegalArgumentException("can't handle command line argument of " + option);
		}



	}
	public static int parsePriority(String argument) {
		int i = " HMLE".indexOf(argument);
		if (i == -1)
			i = " 1234".indexOf(argument);
		if (i == -1)
			throw new IllegalArgumentException("Bad priority: " + argument);
		return i;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DetectorFactoryCollection.instance();
		final FilterCommandLine commandLine = new FilterCommandLine();

		int argCount = commandLine.parse(args, 0, 2, "Usage: " + Filter.class.getName()
				+ " [options] [<orig results> [<new results]] ");
		Project project = new Project();
		SortedBugCollection origCollection = new SortedBugCollection();

		if (argCount == args.length)
			origCollection.readXML(System.in, project);
		else
			origCollection.readXML(args[argCount++], project);
		boolean verbose = argCount < args.length;
		SortedBugCollection resultCollection = origCollection.createEmptyCollectionWithMetadata();
		int passed = 0;
		int dropped = 0;
		resultCollection.setWithMessages(commandLine.withMessages);
		sourceFinder.setSourceBaseList(project.getSourceDirList());
		commandLine.adjustFilter(resultCollection);
		resultCollection.getProjectStats().clearBugCounts();

		for (BugInstance bug : origCollection.getCollection())
			if (commandLine.accept(bug)) {
				resultCollection.add(bug, false);
				if (bug.getLastVersion() == -1 )
					resultCollection.getProjectStats().addBug(bug);
				passed++;
			} else
				dropped++;

		if (verbose)
			System.out.println(passed + " warnings passed through, " + dropped
				+ " warnings dropped");
		if (argCount == args.length) {
			assert !verbose;
			resultCollection.writeXML(System.out, project);
		}
		else {
			resultCollection.writeXML(args[argCount++], project);

		}

	}

}

// vim:ts=4
