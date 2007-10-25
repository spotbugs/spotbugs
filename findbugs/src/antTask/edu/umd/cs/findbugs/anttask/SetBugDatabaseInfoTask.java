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

import org.apache.tools.ant.BuildException;

/**
 * Ant task to invoke the SetBugDatabaseInfo program in the
 * workflow package
 * 
 * @author David Hovemeyer
 * @author Ben Langmead
 */
public class SetBugDatabaseInfoTask extends AbstractFindBugsTask {
	
	private File outputFile;
    private String name;
	private String timestamp;
	private String source;
    private String findSource;
    private String suppress;
    private String withMessages;
    private String resetSource;
	private DataFile inputFile;

	public SetBugDatabaseInfoTask() {
    	super("edu.umd.cs.findbugs.workflow.SetBugDatabaseInfo");
    	setFailOnError(true);
    }
	
	public DataFile createDataFile() {
		if (inputFile != null) {
			throw new BuildException("only one dataFile element is allowed", getLocation());
		}
		inputFile = new DataFile();
		return inputFile;
	}
	
    public void setName(String arg) {
        this.name = arg;
    }

    public void setTimestamp(String arg) {
        this.timestamp = arg;
    }

    public void setOutput(File output) {
		this.outputFile = output;
	}
	
	public void setInput(String input) {
		this.inputFile = new DataFile();
		this.inputFile.name = input;
	}
            
    public void setSuppress(String arg) {
    	this.suppress = arg;
    }
    
    public void setSource(String arg) {
    	this.source = arg;
    }

    public void setFindSource(String arg) {
        this.findSource = arg;
    }

    public void setWithMessages(String arg) {
        this.withMessages = arg;
    }

    public void setResetSource(String arg) {
        this.resetSource = arg;
    }

    private void checkBoolean(String attrVal, String attrName) {
    	if (attrVal == null) {
    		return;
    	}
    	attrVal = attrVal.toLowerCase();
    	if (!attrVal.equals("true") && !attrVal.equals("false")) {
    		throw new BuildException("attribute " + attrName + " requires boolean value", getLocation());
    	}
    }
    
    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#checkParameters()
     */
    @Override
    protected void checkParameters() {
    	super.checkParameters();
    	
    	if (outputFile == null) {
    		throw new BuildException("output attribute is required", getLocation());
    	}
    	
    	if (inputFile == null) {
    		throw new BuildException("inputFile element is required");
    	}
    	
    	checkBoolean(withMessages, "withMessages");
    	checkBoolean(resetSource, "resetSource");
    }
    
    private void addOption(String name, String value) {
    	if (value != null) {
    		addArg(name);
    		addArg(value);
    	}
    }
    
    public void addBoolOption(String option, String value) {
    	if (value != null) {
    		addArg(option + ":" + value);
    	}
    }

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#configureFindbugsEngine()
	 */
	@Override
	protected void configureFindbugsEngine() {
        addOption("-name", name);
        addOption("-timestamp", timestamp);
		addOption("-source", source);
		addOption("-findSource", findSource);
		addOption("-suppress", suppress);
		addBoolOption("-withMessages", withMessages);
		if(resetSource != null && resetSource.equals("true")) {
            addArg("-resetSource");
		}
		addArg(inputFile.getName());
		if(outputFile != null) {
	        addArg(outputFile.getName());
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#beforeExecuteJavaProcess()
	 */
	@Override
	protected void beforeExecuteJavaProcess() {
		log("running setBugDatabaseInfo...");
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#afterExecuteJavaProcess(int)
	 */
	@Override
	protected void afterExecuteJavaProcess(int rc) {
		if (rc != 0) {
			throw new BuildException("execution of " + getTaskName() + " failed");
		}
	}

}
