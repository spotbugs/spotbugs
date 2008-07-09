/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;

/**
 * This is where it all begins
 * run with -f int to set font size
 * run with -clear to clear recent projects menu, or any other issues with program not starting properly due to 
 * something being corrupted (or just faulty) in backend store for GUISaveState.
 * 
 */
public class Driver {
	
	private static final String USAGE = Driver.class.getName() + " [options] [project or analysis results file]";

	private static GUI2CommandLine commandLine;
	private static SplashFrame splash;

	public static void main(String[] args) throws Exception {
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
		{
			System.setProperty("apple.laf.useScreenMenuBar","true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "FindBugs");	
			Debug.println("Mac OS detected");
		}

		splash = new SplashFrame();
		splash.setVisible(true);
		
		commandLine = new GUI2CommandLine();
		int remainingArgs = commandLine.parse(args, 0, 1, USAGE);

		if (commandLine.getDocking()) {
			// make sure docking runtime support is available
			try {
				Class.forName("net.infonode.docking.DockingWindow");
				Class.forName("edu.umd.cs.findbugs.gui2.DockLayout");
			} catch (Exception e) {
				commandLine.setDocking(false);
			}
		}

		try {
			GUISaveState.loadInstance();
		} catch (RuntimeException e) {
			GUISaveState.clear();
			e.printStackTrace();	
		}

		float fontSize = commandLine.getFontSize();
		if(fontSize == 12 && GUISaveState.getInstance().getFontSize() != 12)
			fontSize = GUISaveState.getInstance().getFontSize();
		else
			GUISaveState.getInstance().setFontSize(fontSize);

		// System.setProperty("findbugs.home",".."+File.separator+"findbugs");
		DetectorFactoryCollection.instance();

//		The bug with serializable idiom detection has been fixed on the findbugs end.
//		DetectorFactory serializableIdiomDetector=DetectorFactoryCollection.instance().getFactory("SerializableIdiom");
//		System.out.println(serializableIdiomDetector.getFullName());
//		UserPreferences.getUserPreferences().enableDetector(serializableIdiomDetector,false);

		FindBugsLayoutManagerFactory factory;

		if (isDocking())
			factory = new FindBugsLayoutManagerFactory("edu.umd.cs.findbugs.gui2.DockLayout");
		else
			factory = new FindBugsLayoutManagerFactory(SplitLayout.class.getName());
		MainFrame.makeInstance(factory);

		splash.setVisible(false);
		splash.dispose();
	}

	public static void removeSplashScreen() {
		splash.setVisible(false);
		splash.dispose();

		if(commandLine.getSaveFile() != null) {
			MainFrame.getInstance().openAnalysis(commandLine.getSaveFile(), SaveType.XML_ANALYSIS);
		}
		else if(commandLine.getProject() != null) {
			MainFrame.getInstance().setProject(commandLine.getProject());
			MainFrame.getInstance().newProject();
			
			MainFrame.getInstance().redoAnalysis();
		}
	}
	
	public static boolean isDocking()
	{
		return commandLine.getDocking();
	}

	public static float getFontSize(){
		return commandLine.getFontSize();
	}
	
	public static int getPriority() {
		return commandLine.getPriority();
	}
	
	public static AnalysisFeatureSetting[] getAnalysisSettingList() {
		return commandLine.getSettingList();
	}
}
