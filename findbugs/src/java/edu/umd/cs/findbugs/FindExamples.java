package edu.umd.cs.findbugs;


import java.util.*;

public class FindExamples {

	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			System.err.println("Usage: " + FindExamples.class.getName() +
				" [-category <category>]" +
				" <results file>");
			System.exit(1);
		}

		DetectorFactoryCollection.instance(); // load plugins

		int start = 0;
		String category = null;
		if (argv[0].equals("-category")) {
			category = argv[1];
			start = 2;
		}

		for (int i = start; i < argv.length; ++i)
			scan(argv[i], category);
	}

	public static void scan(String filename, String category) throws Exception {

		BugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(filename, new Project());

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String annotation = bugInstance.getAnnotationText();
			if (annotation.equals(""))
				continue;

			if (category != null && !bugInstance.getAbbrev().equals(category))
				continue;

			Set<String> contents = parseAnnotation(annotation);

			if (contents.contains("GOOD_EXAMPLE") || contents.contains("EXCELLENT_EXAMPLE"))
				dumpBug(bugInstance, filename);
		}

	}

	private static void dumpBug(BugInstance bugInstance, String filename) {
		System.out.println("In " + filename);
		System.out.println(bugInstance.getMessage());
		System.out.println("\t" + bugInstance.getAbbrev());
		SourceLineAnnotation srcLine = bugInstance.getPrimarySourceLineAnnotation();
		if (srcLine != null)
			System.out.println("\t" + srcLine.toString());
		System.out.println(bugInstance.getAnnotationText());
	}

	private static Set<String> parseAnnotation(String annotation) {
		HashSet<String> result = new HashSet<String>();
		StringTokenizer tok = new StringTokenizer(annotation, " \t\r\n\f");
		while (tok.hasMoreTokens())
			result.add(tok.nextToken());
		return result;
	}
}

// vim:ts=4
