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
	 * Report that FindBugs is going to start examining an archive.
	 * @param the name of the archive
	 * @param numClasses the number of classes in the archive
	 */
	public void startArchive(String archiveName, int numClasses);
	/**
	 * Report that FindBugs has finished analyzing one of the classes in
	 * the archive.
	 */
	public void finishClass();
	/**
	 * Report that FindBugs has finished analyzing the current archive.
	 */
	public void finishArchive();
}

// vim:ts=4
