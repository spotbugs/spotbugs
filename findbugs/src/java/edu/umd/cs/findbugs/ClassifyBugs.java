/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.dom4j.DocumentException;

/**
 * @author David Hovemeyer
 */
public class ClassifyBugs {
	static class Count {
		int value;
	}
	
	static final int SERIOUS = 0;
	static final int HARMLESS = 1;
	
	SortedBugCollection bugCollection;
	Project project;
	Set<String> patternSet;
	ArrayList<Map<String, Count>> maps;
	
	public ClassifyBugs() {
		patternSet = new HashSet<String>();
		maps = new ArrayList<Map<String,Count>>();
		for (int i = 0; i < 2; ++i) {
			maps.add(new TreeMap<String,Count>());
		}
	}
	
	public void setBugCollection(SortedBugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}
	
	public void setProject(Project project) {
		this.project = project;
	}
	
	public void load(String fileName) throws IOException, DocumentException {
		bugCollection.readXML(fileName, project);
	}
	
	public void execute() {
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
			BugInstance warning = i.next();
			if (!isClassified(warning))
				continue;
			patternSet.add(warning.getType());
			addToCount(warning.getType(), maps.get(index(warning)));
		}
		
		for (String pattern : patternSet) {
			int numSerious = getCount(pattern, maps.get(SERIOUS)).value;
			int numHarmless = getCount(pattern, maps.get(HARMLESS)).value;
			
			emitClassificationForBugPattern(pattern, numSerious, numHarmless);
		}
	}

	protected void emitClassificationForBugPattern(String pattern, int numSerious, int numHarmless) {
		System.out.println(pattern + " " +numSerious + " " + numHarmless + " " +
				((numSerious * 100) / (numSerious + numHarmless)));
	}
	
	private static int index(BugInstance warning) {
		return isSerious(warning) ? SERIOUS  : HARMLESS;
	}
	
	private static Count getCount(String type, Map<String,Count> map) {
		Count count = map.get(type);
		if (count == null) {
			count = new Count();
			map.put(type, count);
		}
		return count;
	}
	
	private static void addToCount(String type, Map<String,Count> map) {
		getCount(type, map).value++;
	}
	
	private boolean isClassified(BugInstance bugInstance) {
		return !bugInstance.getAnnotationText().equals("");
	}

	
	private static boolean isSerious(BugInstance bugInstance) {
		if (bugInstance.getAnnotationText().equals(""))
			return false;
		Set<String> annotationWords =bugInstance.getTextAnnotationWords();
		return annotationWords.contains("BUG")
			&& !annotationWords.contains("HARMLESS");
	}

	/**
	 * @param args
	 * @throws DocumentException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, DocumentException {
		
		ClassifyBugs cb = new ClassifyBugs();
		
		int argCount = 0;
//		while (argCount < args.length) {
//			String arg = args[argCount++];
//			if (!arg.startsWith("-")) {
//				break;
//			}
//			
//			if (arg.equals("-patterns")) {
//				if (argCount >= args.length) {
//					throw new IllegalArgumentException("-patterns option requires argument");
//				}
//				String patterns = args[argCount++];
//			}
//		}
		
		if (argCount != args.length - 1) {
			System.err.println("Usage: " + ClassifyBugs.class.getName() + " [options] <results file>");
			System.exit(1);
		}

		String fileName = args[argCount];

		SortedBugCollection bugCollection = new SortedBugCollection();
		Project project = new Project();
		
		cb.setBugCollection(bugCollection);
		cb.setProject(project);
		cb.load(fileName);
		
		cb.execute();
	}

}
