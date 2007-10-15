/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.anttask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Abstract base class for Ant tasks that run programs
 * (main() methods) in findbugs.jar or findbugsGUI.jar.
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractFindBugsTask extends Task {
	public static final String FINDBUGS_JAR = "findbugs.jar";
	public static final long DEFAULT_TIMEOUT = 600000; // ten minutes

	// A System property to set when FindBugs is run
	public static class SystemProperty {
		private String name;
		private String value;

		public SystemProperty() {
		}

		public void setName(String name) { this.name = name; }
		public void setValue(String value) { this.value = value; }

		public String getName() { return name; }
		public String getValue() { return value; }
	}

	private String mainClass;
	private boolean debug = false;
	private File homeDir = null;
	private String jvm = "";
	private String jvmargs = "";
	private long timeout = DEFAULT_TIMEOUT;
	private boolean failOnError = false;
	private String errorProperty = null;
	private List<SystemProperty> systemPropertyList = new ArrayList<SystemProperty>();
	private Path classpath = null;
	private Path pluginList = null;

	private Java findbugsEngine = null;

	/**
	 * Constructor.
	 */
	protected AbstractFindBugsTask(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Set the home directory into which findbugs was installed
	 */
	public void setHome(File homeDir) {
		this.homeDir = homeDir;
	}

	/**
	 * Set the debug flag
	 */
	public void setDebug(boolean flag) {
		this.debug = flag;
	}
	
	/**
	 * Get the debug flag.
	 */
	protected boolean getDebug() {
		return debug;
	}

	/**
	 * Set any specific jvm args
	 */
	public void setJvmargs(String args) {
		this.jvmargs = args;
	}

	/**
	 *  Set the command used to start the VM 
	 */
	public void setJvm(String jvm) {
		this.jvm = jvm;
	}

	/**
	 * Set timeout in milliseconds.
	 * @param timeout the timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Set the failOnError flag
	 */
	public void setFailOnError(boolean flag) {
		this.failOnError = flag;
	}

	/**
	 * Tells this task to set the property with the
	 * given name to "true" when there were errors.
	 */
	public void setErrorProperty(String name) {
		this.errorProperty = name;
	}

	/**
	 * Create a SystemProperty (to handle &lt;systemProperty&gt; elements).
	 */
	public SystemProperty createSystemProperty() {
		SystemProperty systemProperty = new SystemProperty();
		systemPropertyList.add(systemProperty);
		return systemProperty;
	}

	/**
	 * Set the classpath to use.
	 */
	public void setClasspath(Path src) {
		if (classpath == null) {
			classpath = src;
		} else {
			classpath.append(src);
		}
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
		Path path = createClasspath();
		path.setRefid(r);
        path.toString(); // Evaluated for its side-effects (throwing a BuildException)
	}

	/**
	 * the plugin list to use.
	 */
	public void setPluginList(Path src) {
		if (pluginList == null) {
			pluginList = src;
		} else {
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
	public void setPluginListRef(Reference r) {
		createPluginList().setRefid(r);
	}

	@Override
	public void execute() throws BuildException {
		checkParameters();
		try {
			execFindbugs();
		} catch (BuildException e) {
			log("Oops: " + e.getMessage());
			if (errorProperty != null) {
				getProject().setProperty(errorProperty, "true");
			}
			if (failOnError) {
				throw e;
			}
		}
	}

	/**
	 * Check that all required attributes have been set.
	 */
	protected void checkParameters() {
		if ( homeDir == null && (classpath == null || pluginList == null) ) {
			throw new BuildException( "either home attribute or " +
									  "classpath and pluginList attributes " +
									  " must be defined for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}

		if (pluginList != null) {
			// Make sure that all plugins are actually Jar files.
			String[] pluginFileList = pluginList.list();
			for (String pluginFile : pluginFileList) {
				if (!pluginFile.endsWith(".jar")) {
					throw new BuildException("plugin file " + pluginFile + " is not a Jar file " +
							"in task <" + getTaskName() + "/>",
							getLocation());
				}
			}
		}

		for (SystemProperty aSystemPropertyList : systemPropertyList) {
			SystemProperty systemProperty = (SystemProperty) aSystemPropertyList;
			if (systemProperty.getName() == null || systemProperty.getValue() == null)
				throw new BuildException("systemProperty elements must have name and value attributes");
		}
	}
	
	/**
	 * Create the FindBugs engine (the Java process that will run
	 * whatever FindBugs-related program this task is
	 * going to execute).
	 */
	protected void createFindbugsEngine() {
		findbugsEngine = (Java) getProject().createTask("java");

		findbugsEngine.setTaskName( getTaskName() );
		findbugsEngine.setFork( true );
		if (jvm.length()>0)
			findbugsEngine.setJvm( jvm );
		findbugsEngine.setTimeout( timeout  );

		if ( debug ) {
			jvmargs = jvmargs + " -Dfindbugs.debug=true";
		}
		findbugsEngine.createJvmarg().setLine( jvmargs ); 

		// Add JVM arguments for system properties
		for (SystemProperty aSystemPropertyList : systemPropertyList) {
			SystemProperty systemProperty = (SystemProperty) aSystemPropertyList;
			String jvmArg = "-D" + systemProperty.getName() + "=" + systemProperty.getValue();
			findbugsEngine.createJvmarg().setValue(jvmArg);
		}

		if (homeDir != null) {
			// Use findbugs.home to locate findbugs.jar and the standard
			// plugins.  This is the usual means of initialization.
			
			log("executing using home dir [" + homeDir + "]");
			
			findbugsEngine.setClasspath(new Path(getProject(), homeDir + File.separator + "lib" + 
										 File.separator + FINDBUGS_JAR));

			//addArg("-home");
			//addArg(homeDir.getPath());
			findbugsEngine.createJvmarg().setValue("-Dfindbugs.home=" + homeDir.getPath());
		} else {
			// Use an explicitly specified classpath and list of plugin Jars
			// to initialize.  This is useful for other tools which may have
			// FindBugs installed using a non-standard directory layout.

			findbugsEngine.setClasspath(classpath);

			// Set the main class to be whatever the subclass's constructor
			// specified.
			findbugsEngine.setClassname(mainClass);

			addArg("-pluginList");
			addArg(pluginList.toString());
		}

		findbugsEngine.setClassname(mainClass);
	}

	/**
	 * Get the Findbugs engine.
	 */
	protected Java getFindbugsEngine() {
		return findbugsEngine;
	}

	/**
	 * Add an argument to the JVM used to execute FindBugs.
	 * @param arg the argument
	 */
	protected void addArg(String arg) {
		findbugsEngine.createArg().setValue(arg);
	}

	/**
	 * Create a new JVM to do the work.
	 *
	 * @since Ant 1.5
	 */
	private void execFindbugs() throws BuildException {
		
		createFindbugsEngine();
		configureFindbugsEngine();

		beforeExecuteJavaProcess();

		if (getDebug()) {
			log(getFindbugsEngine().getCommandLine().describeCommand());    
		}

		int rc = getFindbugsEngine().executeJava();

		afterExecuteJavaProcess(rc);
	}
	
	protected abstract void configureFindbugsEngine();
	
	protected abstract void beforeExecuteJavaProcess();
	
	protected abstract void afterExecuteJavaProcess(int rc);
}
