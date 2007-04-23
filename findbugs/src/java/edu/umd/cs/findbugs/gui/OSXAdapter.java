package edu.umd.cs.findbugs.gui;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/*
 * Based on sample code from Apple.
 *
 * This is the only class that uses the Apple specific EAWT classes.
 * This class should only ever be referenced via reflection after
 * checking that we are running on Mac OS X.
 */
public class OSXAdapter extends ApplicationAdapter {

	// pseudo-singleton model; no point in making multiple instances
	// of the EAWT application or our adapter
	private static OSXAdapter theAdapter;
	private static com.apple.eawt.Application theApplication;

	// reference to the app where the existing quit, about, prefs code is
	private FindBugsFrame mainApp;

	private OSXAdapter (FindBugsFrame inApp) {
		mainApp = inApp;
	}

	// implemented handler methods. These are basically hooks into
	// existing functionality from the main app, as if it came
	// over from another platform.

	@Override
	public void handleAbout(ApplicationEvent ae) {
		if (mainApp != null) {
			ae.setHandled(true);
						// We need to invoke modal About Dialog asynchronously
						// otherwise the Application queue is locked for the duration
						// of the about Dialog, which results in a deadlock if a URL is
                        // selected, and we get a ReOpenApplication event when user
						// switches back to Findbugs.
						javax.swing.SwingUtilities.invokeLater(new Runnable() {
								public void run() {
                                    mainApp.about();
								}
							});
		} else {
			throw new IllegalStateException("handleAbout: " +
														"MyApp instance detached from listener");
		}
	}

	@Override
	public void handlePreferences(ApplicationEvent ae) {
		if (mainApp != null) {
//			mainApp.preferences();
			ae.setHandled(true);
		} else {
			throw new IllegalStateException("handlePreferences: MyApp instance " +
														"detached from listener");
		}
	}

	@Override
	public void handleQuit(ApplicationEvent ae) {
		if (mainApp != null) {

			/*
						 * You MUST setHandled(false) if you want to
			 * delay or cancel the quit. This is important
			 * for cross-platform development -- have a
			 * universal quit routine that chooses whether
			 * or not to quit, so the functionality is
			 * identical on all platforms.  This example
			 * simply cancels the AppleEvent-based quit and 
			 * defers to that universal method.
			*/

			ae.setHandled(false);
			mainApp.exitFindBugs();
		} else {
			throw new IllegalStateException("handleQuit: MyApp instance detached " +
														"from listener");
		}
	}


	// The main entry-point for this functionality.  This is the only method
	// that needs to be called at runtime, and it can easily be done using
	// reflection (see MyApp.java) 
	public static void registerMacOSXApplication(FindBugsFrame inApp) {
		if (theApplication == null) {
			theApplication = new com.apple.eawt.Application();
		}			

		if (theAdapter == null) {
			theAdapter = new OSXAdapter(inApp);
		}
		theApplication.addApplicationListener(theAdapter);
	}

	// Another static entry point for EAWT functionality.  Enables the 
	// "Preferences..." menu item in the application menu. 
	public static void enablePrefs(boolean enabled) {
		if (theApplication == null) {
			theApplication = new com.apple.eawt.Application();
		}
		theApplication.setEnabledPreferencesMenu(enabled);
	}
}
