/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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

package edu.umd.cs.findbugs.anttask;


import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Ant.Reference;
import org.apache.tools.ant.types.Path;

import edu.umd.cs.findbugs.ExitCodes;

/**
 * FindBugsViewerTask.java -- Ant Task to launch the FindBugsFrame
 * 
 * To use, create a new task that refrences the ant task
 * (such as "findbugs-viewer"). Then call this task while
 * passing in parameters to modify it's behaviour. It
 * supports several options that are the same as the
 * findbugs task:
 *
 * -projectFile
 * -debug
 * -jvmargs
 * -home
 * -classpath
 * -pluginList
 * -timeout
 *
 * It also adds some new options:
 *
 * -look: string name representing look and feel. Can be
 * "native", "plastic" or "gtk"
 * -loadbugs: file name of bug report to load
 *
 * The below is an example of how this could be done in an
 * ant script:
 *
 * <taskdef name="findbugs"
 * classname="edu.umd.cs.findbugs.anttask.FindBugsTask"
 * classpath="C:\dev\cvs.sourceforge.net\findbugs\lib\findbugs-ant.jar" />
 * <taskdef name="findbugs-viewer"
 * classname="edu.umd.cs.findbugs.anttask.FindBugsViewerTask"
 * classpath="C:\dev\cvs.sourceforge.net\findbugs\lib\findbugs-ant.jar" />
 *
 * <property name="findbugs.home"
 * location="C:\dev\cvs.sourceforge.net\findbugs" />
 * <property name="findbugs.bugReport"
 * location="bcel-fb.xml" />
 *
 * <target name="findbugs-viewer" depends="jar">
 * <findbugs-viewer home="${findbugs.home}"
 * look="native" loadbugs="${findbugs.bugReport}"/>
 * </target>
 *
 * Created on March 21, 2006, 12:57 PM
 * @author Mark McKay, mark@kitfox.com
 */
public class FindBugsViewerTask extends Task {

	private static final String FINDBUGSGUI_JAR = "findbugsGUI.jar";
	private static final long DEFAULT_TIMEOUT = -1; // ten minutes

	//location to load bug report from
	private boolean debug = false;
	private File projectFile = null;
	private File loadbugs = null;
	private long timeout = DEFAULT_TIMEOUT;
	private String jvmargs = "";
	private String look = "native";
	private File homeDir = null;
	private Path classpath = null;
	private Path pluginList = null;

	private Java findbugsEngine = null;

	/** Creates a new instance of FindBugsViewerTask */
	public FindBugsViewerTask() {
	}

	/**
	 * Sets the file that contains the XML output of a findbugs report.
	 *
	 * @param bugReport XML output from a findbugs session
	 */
	public void setLoadbugs(File loadbugs) 	{
		this.loadbugs = loadbugs;
	}

	/**
	 * Set the project file
	 */
	public void setProjectFile(File projectFile) {
		this.projectFile = projectFile;
	}

	/**
	 * Set the debug flag
	 */
	public void setDebug(boolean flag) {
		this.debug = flag;
	}

	/**
	 * Set any specific jvm args
	 */
	public void setJvmargs(String args) {
		this.jvmargs = args;
	}

	/**
	 * Set look.  One of "native", "gtk" or "plastic"
	 */
	public void setLook(String look) {
		this.look = look;
	}


	/**
	 * Set the home directory into which findbugs was installed
	 */
	public void setHome(File homeDir) {
		this.homeDir = homeDir;
	}


	/**
	 * Path to use for classpath.
	 */
	public Path createClasspath() {
		if (classpath == null) {
			classpath = new Path(getProject());
		}
		return classpath.createPath();
	}

	/**
	 * Adds a reference to a classpath defined elsewhere.
	 */
	public void setClasspathRef(Reference r) {
		createClasspath().setRefid(r);
	}


	/**
	 * the plugin list to use.
	 */
	public void setPluginList(Path src) {
		if (pluginList == null) {
			pluginList = src;
		}
		else {
			pluginList.append(src);
		}
	}

	/**
	 * Path to use for plugin list.
	 */
	public Path createPluginList() {
		if (pluginList == null) {
			pluginList = new Path(getProject());
		}
		return pluginList.createPath();
	}

	/**
	 * Adds a reference to a plugin list defined elsewhere.
	 */
	public void setPluginListRef(Reference r) 	{
		createPluginList().setRefid(r);
	}

	/**
	 * Set timeout in milliseconds.
	 *
	 * @param timeout the timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Add an argument to the JVM used to execute FindBugs.
	 * @param arg the argument
	 */
	private void addArg(String arg) {
		findbugsEngine.createArg().setValue(arg);
	}

	@Override
	public void execute() throws BuildException {
		findbugsEngine = (Java)getProject().createTask("java");

		findbugsEngine.setTaskName(getTaskName());
		findbugsEngine.setFork(true);

		if (timeout > 0) {
			findbugsEngine.setTimeout(timeout);
		}


		if (debug) {
			jvmargs = jvmargs + " -Dfindbugs.debug=true";
		}
		findbugsEngine.createJvmarg().setLine(jvmargs);

		if (homeDir != null) {
			// Use findbugs.home to locate findbugs.jar and the standard
			// plugins.  This is the usual means of initialization.
			File findbugsLib = new File(homeDir, "lib");
			
			File findbugsLibFindBugs = new File(findbugsLib, "findbugs.jar");
			File findBugsFindBugs =  new File(homeDir, "findbugs.jar");
			//log("executing using home dir [" + homeDir + "]");
			if (findbugsLibFindBugs.exists())
				findbugsEngine.setClasspath(new Path(getProject(), findbugsLibFindBugs.getPath()));
			else if (findBugsFindBugs.exists())
				findbugsEngine.setClasspath(new Path(getProject(), findBugsFindBugs.getPath()));
			else throw new IllegalArgumentException("Can't find findbugs.jar in " + homeDir);

			File findbugsGuiLibFindBugs = new File(findbugsLib, "findbugsGUI.jar");
			File findBugsGuiFindBugs =  new File(homeDir, "findbugsGUI.jar");
			//log("executing using home dir [" + homeDir + "]");
			if (findbugsGuiLibFindBugs.exists())
				findbugsEngine.setClasspath(new Path(getProject(), findbugsGuiLibFindBugs.getPath()));
			else if (findBugsGuiFindBugs.exists())
				findbugsEngine.setClasspath(new Path(getProject(), findBugsGuiFindBugs.getPath()));
			else throw new IllegalArgumentException("Can't find findbugsGUI.jar in " + homeDir);

			findbugsEngine.setClassname("edu.umd.cs.findbugs.LaunchAppropriateUI");
			findbugsEngine.createJvmarg().setValue("-Dfindbugs.home=" + homeDir.getPath());
		}
		else {
			// Use an explicitly specified classpath and list of plugin Jars
			// to initialize.  This is useful for other tools which may have
			// FindBugs installed using a non-standard directory layout.

			findbugsEngine.setClasspath(classpath);
			findbugsEngine.setClassname("edu.umd.cs.findbugs.LaunchAppropriateUI");

			addArg("-pluginList");
			addArg(pluginList.toString());
		}

		if (projectFile != null) {
			addArg("-project");
			addArg(projectFile.getPath());
		}

		if (loadbugs != null) {
			addArg("-loadbugs");
			addArg(loadbugs.getPath());
		}

		if (look != null) {
			addArg("-look:" + look);
			//addArg("-look");
			//addArg(look);
		}


		// findbugsEngine.setClassname("edu.umd.cs.findbugs.gui.FindBugsFrame");

		log("Launching FindBugs Viewer...");

		int rc = findbugsEngine.executeJava();

		if ((rc & ExitCodes.ERROR_FLAG) != 0) {
			throw new BuildException("Execution of findbugs failed.");
		}
		if ((rc & ExitCodes.MISSING_CLASS_FLAG) != 0) 	{
			log("Classes needed for analysis were missing");
		}
	}

}
