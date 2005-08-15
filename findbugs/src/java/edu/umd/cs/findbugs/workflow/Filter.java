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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.config.CommandLine;
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
		Pattern className,bugType;
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
		long alive;
		String aliveAsString; 
		long dead;
		String deadAsString; 
		String annotation;
		String revisionName = null;
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

		public boolean unclassified = false;
		public boolean unclassifiedSpecified = false;

		public boolean serious = false;
		public boolean seriousSpecified = false;
		
		public  List<String> sourcePaths = new LinkedList<String>();

		private Matcher includeFilter, excludeFilter;
		String category;
		int priority = 3;

		FilterCommandLine() {
			addSwitch("-not", "reverse (all) switches for the filter");
			addSwitch("-withSource", "only warnings for switch source is available");
			addOption("-exclude", "filter file", "exclude bugs matching given filter");
			addOption("-include", "filter file", "include only bugs matching given filter");
			addOption("-after", "when", "allow only warnings that first occurred after this version");
			addOption("-before", "when", "allow only warnings that first occurred before this version");
			addOption("-first", "when", "allow only warnings that first occurred in this sequence number");
			
			addOption("-first", "when", "allow only warnings that first occurred in this sequence number");
			addOption("-setRevisionName", "name", "set the name of the last revision in this database");
			addOption("-annotation", "text", "allow only warnings containing this text in an annotation");
			addSwitch("-classified", "allow only classified warnings");
			addSwitch("-serious", "allow only warnings classified as serious");
			addSwitch("-unclassified", "allow only unclassified warnings");
			addOption("-last", "when", "allow only warnings that last occurred in this sequence number");
			addOption("-alive", "when", "allow only warnings alive in this sequence number");
			addOption("-dead", "when", "allow only warnings dead in this sequence number");
			addOption("-source", "directory", "Add this directory to the source search path");
			addOption("-priority", "level", "allow only warnings with this priority or higher");
			addSwitchWithOptionalExtraPart("-active", "truth", "allow only warnings alive in the last sequence number");
			addSwitchWithOptionalExtraPart("-introducedByChange", "truth",
					"allow only warnings introduced by a change of an existing class");
			addSwitchWithOptionalExtraPart("-removedByChange", "truth",
					"allow only warnings removed by a change of a persisting class");
			addSwitchWithOptionalExtraPart("-newCode", "truth",
			"allow only warnings introduced by the addition of a new class");
			addSwitchWithOptionalExtraPart("-removedCode", "truth",
			"allow only warnings removed by removal of a class");
			addOption("-class", "pattern", "allow only bugs whose primary class name matches this pattern");
			addOption("-type", "pattern", "allow only bugs whose type matches this pattern");
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
					throw new IllegalArgumentException("Could not interprete version specification of '" + val + "'");
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
			alive = getVersionNum(versions, timeStamps, aliveAsString, true);
			dead = getVersionNum(versions, timeStamps, deadAsString, true);
			
		}
		boolean accept(BugInstance bug) {
			boolean result = evaluate(bug);
			if (not) return !result;
			return result;
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
			if (aliveAsString != null && !bugLiveAt(bug, alive))
				return false;
			if (deadAsString != null && bugLiveAt(bug, dead))
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

			if (bugType != null && !bugType.matcher(bug.getType()).find())
					return false;
			if (className != null && !className.matcher(bug.getPrimaryClass().getClassName()).find())
					return false;

			if (category != null && !bug.getBugPattern().getCategory().startsWith(category))
				return false;
			
			if (withSourceSpecified) {
				boolean hasSource = false;
				SourceLineAnnotation srcLine = bug.getPrimarySourceLineAnnotation();
				if (srcLine != null) {
					String sourceFile = srcLine.getSourceFile();
					if (sourceFile != null && !sourceFile.equals("<Unknown>")) {
						try {
						  InputStream in = sourceFinder.openSource(srcLine.getPackageName(), sourceFile);
						  in.close();
						  hasSource = true;
						} catch (IOException e) {
							assert true; // ignore it -- couldn't find source file
						}
						
					}
					
				}
				if (hasSource != withSource) return false;
			}

			if (classifiedSpecified && !isClassified(bug)) {
				return false;
			}

			if (unclassifiedSpecified && isClassified(bug)) {
				return false;
			}

			if (seriousSpecified) {
				Set<String> words = bug.getTextAnnotationWords();
				if (   !words.contains("BUG")
					|| (words.contains("NOT_BUG") || words.contains("HARMLESS"))) {
					return false;
				}
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
				setField(option, Boolean.parseBoolean(optionExtraPart));
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
				int i = " HMLE".indexOf(argument);
				if (i == -1)
					i = " 1234".indexOf(argument);
				if (i == -1)
					throw new IllegalArgumentException("Bad priority: " + argument);
				priority = i;
			}

			if (option.equals("-source"))
				sourcePaths.add(argument);
			
			if (option.equals("-first")) 
				firstAsString = argument;
			if (option.equals("-last")) 
				lastAsString = argument;
			if (option.equals("-after")) 
				afterAsString = argument;
			if (option.equals("-before")) 
				beforeAsString = argument;
			if (option.equals("-alive")) 
				aliveAsString = argument;
			if (option.equals("-dead")) 
				deadAsString = argument;
			
			if (option.equals("-setRevisionName"))
				revisionName = argument;
			if (option.equals("-category"))
				category = argument;
			if (option.equals("-class"))
					className = Pattern.compile(argument);
			if (option.equals("-type"))
					bugType = Pattern.compile(argument);
			if (option.equals("-annotation"))
				annotation = argument;
		}

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
		BugCollection origCollection = new SortedBugCollection();

		if (argCount == args.length)
			origCollection.readXML(System.in, project);
		else
			origCollection.readXML(args[argCount++], project);
		boolean verbose = argCount < args.length;
		BugCollection resultCollection = origCollection.createEmptyCollectionWithMetadata();
		int passed = 0;
		int dropped = 0;
		for(String source : commandLine.sourcePaths)
			project.addSourceDir(source);
		sourceFinder.setSourceBaseList(project.getSourceDirList());
		if (commandLine.revisionName != null)
			resultCollection.setReleaseName(commandLine.revisionName);
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
