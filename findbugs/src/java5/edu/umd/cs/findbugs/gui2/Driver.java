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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.dom4j.DocumentException;

import com.apple.eawt.Application;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.gui.FindBugsFrame;

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
			
			if(args[i].startsWith("--font=")){
				float num = 0;
				try{
					num = Integer.valueOf(args[i].substring("--font=".length()));
				}
				catch(NumberFormatException exc){
					num = fontSize;
				}
				fontSize = num;
			}
			
			if(args[i].equals("-clear")){
				GUISaveState.clear();
				System.exit(0);
			}
			
			if (args[i].equals("-d") || args[i].equals("--nodock"))
				docking = false;
		}

		try {
		GUISaveState.loadInstance();
		} catch (RuntimeException e) {
			GUISaveState.clear();
			e.printStackTrace();	
		}

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
	}
	public static boolean isDocking()
	{
		return docking;
	}
	
	public static float getFontSize(){
		return fontSize;
	}
}