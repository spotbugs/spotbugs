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
import java.util.Locale;

import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.util.JavaWebStart;

/**
 * This is where it all begins run with -f int to set font size run with -clear
 * to clear recent projects menu, or any other issues with program not starting
 * properly due to something being corrupted (or just faulty) in backend store
 * for GUISaveState.
 *
 */
public class Driver {
    private static final String USAGE = Driver.class.getName() + " [options] [project or analysis results file]";

    private static GUI2CommandLine commandLine = new GUI2CommandLine();

    private static SplashFrame splash;

    public static void main(String[] args) throws Exception {
        try {

            String name = "SpotBugs GUI";
            if (JavaWebStart.isRunningViaJavaWebstart()) {
                name = "SpotBugs webstart GUI";
            }
            Version.registerApplication(name, Version.VERSION_STRING);

            if (SystemProperties.getProperty("os.name").startsWith("Mac")) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SpotBugs");
                Debug.println("Mac OS detected");
            }
            splash = new SplashFrame();
            splash.setVisible(true);

            int numParsed = commandLine.parse(args, 0, 1, USAGE);

            //
            // See if an argument filename was specified after the parsed
            // options/switches.
            //
            if (numParsed < args.length) {
                String arg = args[numParsed];
                String argLowerCase = arg.toLowerCase(Locale.ENGLISH);
                if (argLowerCase.endsWith(".fbp") || argLowerCase.endsWith(".fb")) {
                    // Project file specified
                    commandLine.loadProject(arg);
                } else if (argLowerCase.endsWith(".xml") || argLowerCase.endsWith(".xml.gz") || argLowerCase.endsWith(".fba")) {
                    // Saved analysis results specified
                    commandLine.setSaveFile(new File(arg));
                } else {
                    System.out.println("Unknown argument: " + arg);
                    commandLine.printUsage(System.out);
                    System.exit(1);
                }
            }

            try {
                GUISaveState.loadInstance();
            } catch (RuntimeException e) {
                GUISaveState.clear();
                e.printStackTrace();
            }

            GUISaveState guiSavedPreferences = GUISaveState.getInstance();
            if (commandLine.isFontSizeSpecified()) {
                guiSavedPreferences.setFontSize(commandLine.getFontSize());
            }

            // System.setProperty("findbugs.home",".."+File.separator+"findbugs");

            enablePlugins(guiSavedPreferences.getEnabledPlugins(), true);
            enablePlugins(guiSavedPreferences.getDisabledPlugins(), false);

            // The bug with serializable idiom detection has been fixed on the
            // findbugs end.
            // DetectorFactory
            // serializableIdiomDetector=DetectorFactoryCollection.instance().getFactory("SerializableIdiom");
            // System.out.println(serializableIdiomDetector.getFullName());
            // UserPreferences.getUserPreferences().enableDetector(serializableIdiomDetector,false);

            FindBugsLayoutManagerFactory factory = new FindBugsLayoutManagerFactory(SplitLayout.class.getName());
            MainFrame.makeInstance(factory);


            splash.setVisible(false);
            splash.dispose();
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(null, t.toString(), "Fatal Error during SpotBugs startup", JOptionPane.ERROR_MESSAGE);
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void enablePlugins(Iterable<String> plugins, boolean enabled) {
        for (String pid : plugins) {
            Plugin plugin = Plugin.getByPluginId(pid);
            if (plugin != null) {
                if (!enabled && plugin.cannotDisable()) {
                    JOptionPane.showMessageDialog(null,
                            "Cannot disable plugin: " + plugin.getPluginId() + "\n" + plugin.getShortDescription(),
                            "Cannot disable plugin", JOptionPane.ERROR_MESSAGE);
                } else {
                    plugin.setGloballyEnabled(enabled);
                }
            }
        }
    }

    public static void removeSplashScreen() {
        if (splash == null) {
            return;
        }
        splash.setVisible(false);
        splash.dispose();

        if (commandLine.getSaveFile() != null) {
            MainFrame.getInstance().openAnalysis(commandLine.getSaveFile(), SaveType.XML_ANALYSIS);
        } else if (commandLine.isProjectLoadedFromFile()) {
            MainFrame.getInstance().setProject(commandLine.getProject());
            MainFrame.getInstance().newProject();

            MainFrame.getInstance().redoAnalysis();
        }
    }

    public static float getFontSize() {
        return commandLine.getFontSize();
    }

    public static int getPriority() {
        return commandLine.getPriority();
    }

    public static AnalysisFeatureSetting[] getAnalysisSettingList() {
        return commandLine.getSettingList();
    }
}
