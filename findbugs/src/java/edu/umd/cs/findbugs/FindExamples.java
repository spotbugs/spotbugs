package edu.umd.cs.findbugs;


import java.util.*;

public class FindExamples {

	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			System.err.println("Usage: " + FindExamples.class.getName() + " <results file>");
			System.exit(1);
		}

		DetectorFactoryCollection.instance(); // load plugins

		for (int i = 0; i < argv.length; ++i)
			scan(argv[i]);
	}

	public static void scan(String filename) throws Exception {

		BugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(filename, new Project());

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String annotation = bugInstance.getAnnotationText();
			if (annotation.equals(""))
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
