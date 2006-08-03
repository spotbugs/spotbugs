package edu.umd.cs.findbugs;

/**
 * @author David Hovemeyer
 */
public class NoOpFindBugsProgress implements FindBugsProgress {
	public void reportNumberOfArchives(int numArchives) {
	}

	public void finishArchive() {
	}

	public void startAnalysis(int numClasses) {
	}

	public void finishClass() {
	}

	public void finishPerClassAnalysis() {
	}
}