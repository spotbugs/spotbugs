/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.umd.cs.findbugs;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Base class for FindBugs command line classes. Handles all shared
 * switches/options.
 *
 * @author David Hovemeyer
 */
public abstract class FindBugsCommandLine extends CommandLine {

    /**
     * Analysis settings to configure the analysis effort.
     */
    protected AnalysisFeatureSetting[] settingList = FindBugs.DEFAULT_EFFORT;

    /**
     * Project to analyze.
     */
    protected Project project;

    /**
     * True if project was initialized by loading a project file.
     */
    protected boolean projectLoadedFromFile;

    /**
     * Constructor. Adds shared options/switches.
     */
    public FindBugsCommandLine() {
        super();
        project = new Project();
        startOptionGroup("General FindBugs options:");
        addOption("-project", "project", "analyze given project");
        addOption("-home", "home directory", "specify FindBugs home directory");
        addOption("-pluginList", "jar1[" + File.pathSeparator + "jar2...]", "specify list of plugin Jar files to load");
        addSwitchWithOptionalExtraPart("-effort", "min|less|default|more|max", "set analysis effort level");
        addSwitch("-adjustExperimental", "lower priority of experimental Bug Patterns");
        addSwitch("-workHard", "ensure analysis effort is at least 'default'");
        addSwitch("-conserveSpace", "same as -effort:min (for backward compatibility)");
    }

    /**
     * Additional constuctor just as hack for decoupling the core package from
     * gui2 package
     *
     * @param modernGui
     *            ignored. In any case, gui2 options are added here.
     */
    public FindBugsCommandLine(boolean modernGui) {
        this();
        addOption("-f", "font size", "set font size");
        addSwitch("-clear", "clear saved GUI settings and exit");
        addOption("-priority", "thread priority", "set analysis thread priority");
        addOption("-loadbugs", "saved analysis results", "load bugs from saved analysis results");
        makeOptionUnlisted("-loadbugs");
        addOption("-loadBugs", "saved analysis results", "load bugs from saved analysis results");

        addSwitch("-d", "disable docking");
        addSwitch("--nodock", "disable docking");
        addSwitchWithOptionalExtraPart("-look", "plastic|gtk|native", "set UI look and feel");
    }

    public AnalysisFeatureSetting[] getSettingList() {
        return settingList;
    }

    public @Nonnull
    Project getProject() {
        return project;
    }

    public boolean isProjectLoadedFromFile() {
        return projectLoadedFromFile;
    }

    @Override
    protected void handleOption(String option, String optionExtraPart) {
        if ("-effort".equals(option)) {
            if ("min".equals(optionExtraPart)) {
                settingList = FindBugs.MIN_EFFORT;
            } else if ("less".equals(optionExtraPart)) {
                settingList = FindBugs.LESS_EFFORT;
            } else if ("default".equals(optionExtraPart)) {
                settingList = FindBugs.DEFAULT_EFFORT;
            } else if ("more".equals(optionExtraPart)) {
                settingList = FindBugs.MORE_EFFORT;
            } else if ("max".equals(optionExtraPart)) {
                settingList = FindBugs.MAX_EFFORT;
            } else {
                throw new IllegalArgumentException("-effort:<value> must be one of min,default,more,max");
            }
        } else if ("-workHard".equals(option)) {
            if (settingList != FindBugs.MAX_EFFORT) {
                settingList = FindBugs.MORE_EFFORT;
            }

        } else if ("-conserveSpace".equals(option)) {
            settingList = FindBugs.MIN_EFFORT;
        } else if ("-adjustExperimental".equals(option)) {
            BugInstance.setAdjustExperimental(true);
        } else {
            throw new IllegalArgumentException("Don't understand option " + option);
        }
    }

    @Override
    protected void handleOptionWithArgument(String option, String argument) throws IOException {
        if ("-home".equals(option)) {
            FindBugs.setHome(argument);
        } else if ("-pluginList".equals(option)) {
            String pluginListStr = argument;
            Map<String, Boolean> customPlugins = getProject().getConfiguration().getCustomPlugins();
            StringTokenizer tok = new StringTokenizer(pluginListStr, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                File file = new File(tok.nextToken());
                Boolean enabled = Boolean.valueOf(file.isFile());
                customPlugins.put(file.getAbsolutePath(), enabled);
                if(enabled.booleanValue()) {
                    try {
                        Plugin.loadCustomPlugin(file, getProject());
                    } catch (PluginException e) {
                        throw new IllegalStateException("Failed to load plugin " +
                                "specified by the '-pluginList', file: " + file, e);
                    }
                }
            }
        } else if ("-project".equals(option)) {
            loadProject(argument);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Load given project file.
     *
     * @param arg
     *            name of project file
     * @throws java.io.IOException
     */
    public void loadProject(String arg) throws IOException {
        Project newProject = Project.readProject(arg);
        newProject.setConfiguration(project.getConfiguration());
        project = newProject;
        projectLoadedFromFile = true;
    }
}
