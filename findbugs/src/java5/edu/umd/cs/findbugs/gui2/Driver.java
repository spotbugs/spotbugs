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

import java.io.FileNotFoundException;

import javax.swing.UIManager;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * This is where it all begins
 * run with -f int to set font size
 * run with -clear to clear recent projects menu, or any other issues with program not starting properly due to 
 * something being corrupted (or just faulty) in backend store for GUISaveState.
 * 
 */
public class Driver {

	private static float fontSize = 12;
	private static boolean docking = true;
	private static SplashFrame splash;
	private static int priority = Thread.NORM_PRIORITY-1;
	private static Project project = null;

	public static void main(String[] args) throws Exception {
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
		{
			System.setProperty("apple.laf.useScreenMenuBar","true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "FindBugs");	
			Debug.println("Mac OS detected");
		}

		splash = new SplashFrame();
		splash.setVisible(true);


		try {
			Class.forName("net.infonode.docking.DockingWindow");
			Class.forName("edu.umd.cs.findbugs.gui2.DockLayout");
		} catch (Exception e) {
			docking = false;
		}

		for(int i = 0; i < args.length; i++){
			if((args[i].equals("-f")) && (i+1 < args.length)){
				float num = 0;
				try{
					i++;
					num = Integer.valueOf(args[i]);
				}
				catch(NumberFormatException exc){
					num = fontSize;
				}
				fontSize = num;
			}

			else if(args[i].startsWith("--font=")){
				float num = 0;
				try{
					num = Integer.valueOf(args[i].substring("--font=".length()));
				}
				catch(NumberFormatException exc){
					num = fontSize;
				}
				fontSize = num;
			}

			else if(args[i].equals("-clear")){
				GUISaveState.clear();
				System.exit(0);
			}

			else if(args[i].equals("-priority")){
				int num = 0;
				try{
					i++;
					num = Integer.valueOf(args[i]);
				}
				catch(NumberFormatException exc){
					num = Thread.NORM_PRIORITY-1;
				}
				priority = num;
			}

			else if(args[i].equals("-project")){
				try {
					i++;
					project = Project.readProject(args[i]);
				} catch(FileNotFoundException e) {
					System.err.println("Project file \"" + args[i] + "\" could not be found");
					System.exit(1);
				} catch(IndexOutOfBoundsException e) {
					System.err.println("Option \"-project\" must be followed by a filename");
					System.exit(1);
				}
			}
			
			else if (args[i].equals("-d") || args[i].equals("--nodock")) {
				docking = false;
			}
			
			else if (args[i].startsWith("-look:")) {
				String arg = args[i].substring("-look:".length());

				String theme = null;
				if (arg.equals("plastic")) {
					// You can get the Plastic look and feel from jgoodies.com:
					//	http://www.jgoodies.com/downloads/libraries.html
					// Just put "plastic.jar" in the lib directory, right next
					// to the other jar files.
					theme = "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel";
				} else if (arg.equals("gtk")) {
					theme = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
				} else if (arg.equals("native")) {
					theme = UIManager.getSystemLookAndFeelClassName();
				} else {
					System.err.println("Style '" + arg + "' not supported");
				}

				if (theme != null) {
					try {
						UIManager.setLookAndFeel(theme);
					} catch (Exception e) {
						System.err.println("Couldn't load " + arg +
								" look and feel: " + e.toString());
					}
				}
			}
			
			else {
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
		}

		try {
			GUISaveState.loadInstance();
		} catch (RuntimeException e) {
			GUISaveState.clear();
			e.printStackTrace();	
		}

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

		if(project != null) {
			MainFrame.getInstance().setProject(project);
			MainFrame.getInstance().newProject();
		}
	}
	public static boolean isDocking()
	{
		return docking;
	}

	public static float getFontSize(){
		return fontSize;
	}
	
	public static int getPriority() {
		return priority;
	}
}
