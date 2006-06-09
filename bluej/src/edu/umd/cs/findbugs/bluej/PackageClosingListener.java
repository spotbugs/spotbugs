package edu.umd.cs.findbugs.bluej;

import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.event.PackageListener;

/**
 * Listens for when a package opens or closes. When closes also closes the
 * ResultsFrame of findbugs if one is open.
 * @author Kristin Stephens
 *
 */
public class PackageClosingListener implements PackageListener {

	/**
	 * Does not do anything when package is opened.
	 */
	public void packageOpened(PackageEvent vt) {
	}

	/**
	 * When package is closed will also close the FindBugs results frame
	 * if it is open.
	 */
	public void packageClosing(PackageEvent evt) {
		try {
			ResultsFrame frame = ResultsFrame.getInstance(evt.getPackage().getProject(), false);
			if(frame != null)
				frame.setVisible(false);
		} catch (ProjectNotOpenException e) {
			Log.recordBug(e);
		}

	}

}
