package edu.umd.cs.findbugs;

import java.util.*;

public class CategorizeBugs {

	private static final int ALL = 0;
	private static final int BENIGN = 1;
	private static final int DUBIOUS = 2;
	private static final int SERIOUS = 3;

	private static class Stats {
		public int total = 0;
		public int[] bug = new int[4];
		public int[] notBug = new int[4];

		public void addTo(boolean isBug, int severity) {
			int[] arr = isBug ? bug : notBug;

			arr[ALL]++;
			if (severity >= 0)
				arr[severity]++;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();

			for (int j = 0; j < 4; ++j)
				buf.append(bug[j] + "\t");
			for (int j = 0; j < 4; ++j)
				buf.append(notBug[j] + "\t");

			int total = bug[ALL] + notBug[ALL];
			if (total > 0) {
				int serious = bug[ALL] - bug[BENIGN];
				double accuracy = ((double) serious / (double) total) * 100.0;
				buf.append(String.valueOf(accuracy));
			}

			return buf.toString();
		}
	}

	private static TreeMap<String, Stats> statsByType = new TreeMap<String, Stats>();
	private static TreeMap<String, Stats> statsByCode = new TreeMap<String, Stats>();
	private static Stats aggregateStats = new Stats();

	private static final boolean BY_CODE_ONLY = Boolean.getBoolean("findbugs.categorize.byCodeOnly");

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + CategorizeBugs.class.getName() + " <results file>");
			System.exit(1);
		}

		//if (BY_CODE_ONLY) System.out.println("Only dump bug codes");

		DetectorFactoryCollection.instance(); // load plugins

		BugCollection bugCollection = new SortedBugCollection();

		bugCollection.readXML(argv[0], new Project());

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String annotation = bugInstance.getAnnotationText();
			if (annotation.equals(""))
				continue;

			Set<String> contents = parseAnnotation(annotation);

			boolean isBug = contents.contains("BUG");
			boolean isNotBug = contents.contains("NOT_BUG");
			if (!isBug && !isNotBug) {
				System.out.println("Unknown status for bug:");
				dumpBug(bugInstance);
				continue;
			}

			int severity = -1;
			if (contents.contains("BENIGN") || contents.contains("HARMLESS"))
				severity = BENIGN;
			else if (contents.contains("DUBIOUS"))
				severity = DUBIOUS;
			else if (contents.contains("SERIOUS"))
				severity = SERIOUS;

			updateStats(bugInstance.getType(), statsByType, isBug, severity);
			updateStats(bugInstance.getAbbrev(), statsByCode, isBug, severity);
			aggregateStats.addTo(isBug, severity);
		}

		if (!BY_CODE_ONLY)
			dumpStats("Statistics by bug pattern", statsByType);
		dumpStats("Statistics by bug code", statsByCode);
		System.out.println("\nAggregate statistics\n");
		System.out.println(aggregateStats.toString());
	}

	private static void dumpBug(BugInstance bugInstance) {
		System.out.println(bugInstance.getMessage());
		SourceLineAnnotation srcLine = bugInstance.getPrimarySourceLineAnnotation();
		if (srcLine != null)
			System.out.println("\t" + srcLine.toString());
		System.out.println(bugInstance.getAnnotationText());
	}

	private static void updateStats(String key, Map<String, Stats> map, boolean isBug, int severity) {
		Stats stats = map.get(key);
		if (stats == null) {
			stats = new Stats();
			map.put(key, stats);
		}

		stats.addTo(isBug, severity);
	}

	private static void dumpStats(String banner, Map<String, Stats> map) {
		System.out.println("\n" + banner + ":");
		for (Map.Entry<String, Stats> entry : map.entrySet()) {
			String key = entry.getKey();
			Stats stats = entry.getValue();

			System.out.print(key + ":\t");
			System.out.print(stats.toString());

			System.out.println();
		}
	}

	private static Set<String> parseAnnotation(String annotation) {
		Set<String> result = new HashSet<String>();
		StringTokenizer tok = new StringTokenizer(annotation, " \t\r\n\f");
		while (tok.hasMoreTokens())
			result.add(tok.nextToken());
		return result;
	}
}

// vim:ts=4
