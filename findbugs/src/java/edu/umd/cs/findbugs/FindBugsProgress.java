package edu.umd.cs.findbugs;

/**
 * A callback that may be installed in a FindBugs instance
 * to asynchronously keep track of its progress.
 *
 * @see FindBugs
 * @author David Hovemeyer
 */
public interface FindBugsProgress {
	/**
	 * Report the total number of archives (Jar or zip files) that will be analyzed.
	 * @param numArchives the number of archives
	 */
	public void reportNumberOfArchives(int numArchives);

	/**
	 * Report that FindBugs has finished scanning an archive in order
	 * to add its classes to the repository.
	 */
	public void finishArchive();

	/**
	 * Report that FindBugs has finished scanning the archives and will
	 * start analysing the classes contained therein.
	 * @param numClasses number of classes found in all of the archives
	 */
	public void startAnalysis(int numClasses);

	/**
	 * Report that FindBugs has finished analyzing a class.
	 */
	public void finishClass();

	/**
	 * Called to indicate that the per-class analysis is finished, and
	 * that the whole program analysis is taking place.
	 */
	public void finishPerClassAnalysis();
}

// vim:ts=4
