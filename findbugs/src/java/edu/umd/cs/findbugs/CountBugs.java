package edu.umd.cs.findbugs;


import java.util.*;

public class CountBugs {

	private static HashSet<String> categorySet = new HashSet<String>();
	private static TreeMap<String, Integer> countMap = new TreeMap<String, Integer>();

	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			System.err.println("Usage: " + CountBugs.class.getName() + " <results file>");
			System.exit(1);
		}

		DetectorFactoryCollection.instance(); // load plugins

		int arg = 0;
		if (argv[0].equals("-categories")) {
			StringTokenizer tok = new StringTokenizer(argv[1], ",");
			while (tok.hasMoreTokens()) {
				String category = tok.nextToken();
				categorySet.add(category);
				countMap.put(category, new Integer(0));
			}
			arg = 2;
		}

		String filename = argv[arg];

		BugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(filename, new Project());

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String category = bugInstance.getAbbrev();

			if (categorySet.size() > 0 && !categorySet.contains(category))
				continue;

			Integer count = countMap.get(category);
			if (count == null)
				count = new Integer(0);
			countMap.put(category, new Integer(count.intValue() + 1));
		}

		Iterator<Map.Entry<String, Integer>> j = countMap.entrySet().iterator();
		while(j.hasNext()) {
			Map.Entry<String,Integer> entry = j.next();
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}

	}
}
