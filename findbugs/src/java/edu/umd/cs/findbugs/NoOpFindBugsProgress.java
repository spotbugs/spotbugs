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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.FindBugsProgress#predictPassCount(int[])
	 */
	public void predictPassCount(int[] classesPerPass) {
		// TODO Auto-generated method stub
		
	}
}