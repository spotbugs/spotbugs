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
	}

	private static TreeMap<String, Stats> statsByType = new TreeMap<String, Stats>();
	private static TreeMap<String, Stats> statsByCode = new TreeMap<String, Stats>();

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + CategorizeBugs.class.getName() + " <results file>");
			System.exit(1);
		}

		BugCollection bugCollection = new SortedBugCollection();

		bugCollection.readXML(argv[0], new Project(), new HashMap<String, String>());

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String annotation = bugInstance.getAnnotationText();
			if (annotation.equals(""))
				continue;

			boolean isBug = annotation.indexOf("BUG") >= 0 && annotation.indexOf("NOT_BUG") < 0;
			boolean isNotBug = annotation.indexOf("NOT_BUG") >= 0;
			if (!isBug && !isNotBug) {
				System.out.println("Unknown status for bug:");
				dumpBug(bugInstance);
				continue;
			}

			int severity = -1;
			if (annotation.indexOf("BENIGN") >= 0)
				severity = BENIGN;
			else if (annotation.indexOf("DUBIOUS") >= 0)
				severity = DUBIOUS;
			else if (annotation.indexOf("SERIOUS") >= 0)
				severity = SERIOUS;
			else if (isBug) {
				System.out.println("Unknown severity for bug:");
				dumpBug(bugInstance);
				continue;
			}

			updateStats(bugInstance.getType(), statsByType, isBug, severity);
			updateStats(bugInstance.getAbbrev(), statsByCode, isBug, severity);
		}

		dumpStats("Statistics by bug pattern", statsByType);
		dumpStats("Statistics by bug code", statsByCode);
	}

	private static void dumpBug(BugInstance bugInstance) {
		System.out.println(bugInstance.getMessage());
		SourceLineAnnotation srcLine = bugInstance.getPrimarySourceLineAnnotation();
		if (srcLine != null)
			System.out.println("\t" + srcLine.toString());
		System.out.println(bugInstance.getAnnotationText());
	}

	private static void updateStats(String key, Map<String,Stats> map, boolean isBug, int severity) {
		Stats stats = map.get(key);
		if (stats == null) {
			stats = new Stats();
			map.put(key, stats);
		}
		int[] arr = isBug ? stats.bug : stats.notBug;

		arr[ALL]++;
		if (severity >= 0)
			arr[severity]++;
	}

	private static void dumpStats(String banner, Map<String, Stats> map) {
		System.out.println("\n" + banner + ":");
		Iterator<Map.Entry<String, Stats>> i = map.entrySet().iterator();
		while(i.hasNext()) {
			Map.Entry<String, Stats> entry = i.next();
			String key = entry.getKey();
			Stats stats = entry.getValue();
			System.out.print(key + ":\t");
			for (int j = 0; j < 4; ++j)
				System.out.print(stats.bug[j] + "\t");
			for (int j = 0; j < 4; ++j)
				System.out.print(stats.notBug[j] + "\t");

			int total = stats.bug[ALL] + stats.notBug[ALL];
			if (total > 0){
				double accuracy = ((double) stats.bug[ALL] / (double) total) * 100.0;
				System.out.print(accuracy);
			}

			System.out.println();
		}
	}
}

// vim:ts=4
