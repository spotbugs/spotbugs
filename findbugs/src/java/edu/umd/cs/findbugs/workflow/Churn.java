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

package edu.umd.cs.findbugs.workflow;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Mine historical information from a BugCollection. The BugCollection should be
 * built using UpdateBugCollection to record the history of analyzing all
 * versions over time.
 * 
 * @author David Hovemeyer
 * @author William Pugh
 */
public class Churn {
	BugCollection bugCollection;

	public Churn() {
	}

	public Churn(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}

	public void setBugCollection(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}

	String getKey(BugInstance b) {
		return b.getPriorityAbbreviation() + "-" + b.getType();
	}

	static class Data {
		int persist, fixed;
	}

	Map<String, Data> data = new HashMap<String, Data>();

	public Churn execute() {

		for (Iterator<BugInstance> j = bugCollection.iterator(); j.hasNext();) {
			BugInstance bugInstance = j.next();

			String key = getKey(bugInstance);
			Data d = data.get(key);
			if (d == null)
				data.put(key, d = new Data());
			if (bugInstance.isDead())
				d.fixed++;
			else
				d.persist++;
		}
		return this;
	}

	public void dump(PrintStream out) {
		System.out.printf("%3s %5s %5s  %s\n", "%", "const", "fix", "new");
		for (Map.Entry<String, Data> e : data.entrySet()) {
			Data d = e.getValue();
			int total = d.persist + d.fixed;
			if (total < 2)
				continue;
			System.out.printf("%3d %5d %5d  %s\n", d.fixed * 100 / total, d.persist, d.fixed, e.getKey());
		}

	}

	class ChurnCommandLine extends CommandLine {

		@Override
		public void handleOption(String option, String optionalExtraPart) {
			throw new IllegalArgumentException("unknown option: " + option);
		}

		@Override
		public void handleOptionWithArgument(String option, String argument) {
			throw new IllegalArgumentException("unknown option: " + option);
		}
	}

	public static void main(String[] args) throws Exception {
		DetectorFactoryCollection.instance(); // load plugins

		Churn churn = new Churn();
		ChurnCommandLine commandLine = churn.new ChurnCommandLine();
		int argCount = commandLine
		        .parse(args, 0, 2, "Usage: " + Churn.class.getName() + " [options] [<xml results> [<history]] ");

		SortedBugCollection bugCollection = new SortedBugCollection();
		if (argCount < args.length)
			bugCollection.readXML(args[argCount++], new Project());
		else
			bugCollection.readXML(System.in, new Project());
		churn.setBugCollection(bugCollection);
		churn.execute();
		PrintStream out = System.out;
		try {
			if (argCount < args.length) {
				out = new PrintStream(new FileOutputStream(args[argCount++]), true);
			}
			churn.dump(out);
		} finally {
			out.close();
		}

	}
}
