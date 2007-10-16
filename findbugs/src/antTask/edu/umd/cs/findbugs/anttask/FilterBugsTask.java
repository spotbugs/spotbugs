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
 * Ant task to invoke the FilterBugs program in the
 * workflow package (a.k.a. the filterBugs script.) 
 * 
 * @author David Hovemeyer
 */
public class FilterBugsTask extends AbstractFindBugsTask {
	
	private File outputFile;
    private String not;
	private String withSource;
	private String exclude;
	private String include;
	private String annotation;
	private String classified;
	private String serious;
	private String after;
	private String before;
	private String first;
	private String last;
	private String fixed;
	private String present;
	private String absent;
	private String active;
	private String introducedByChange;
	private String removedByChange;
	private String newCode;
	private String removedCode;
	private String priority;
	private String clazz;
	private String bugPattern;
	private String category;
	private String designation;
	private String withMessages;
	private String excludeBugs;
	private DataFile inputFile;

	public FilterBugsTask() {
    	super("edu.umd.cs.findbugs.workflow.Filter");
    	
    	setFailOnError(true);
    }
	
	public DataFile createDataFile() {
		if (inputFile != null) {
			throw new BuildException("only one dataFile element is allowed", getLocation());
		}
		inputFile = new DataFile();
		return inputFile;
	}
	
	public void setOutput(File output) {
		this.outputFile = output;
	}
    
    public void setNot(String arg) {
    	this.not = arg;
    }
    
    public void withSource(String arg) {
    	this.withSource = arg;
    }
    
    public void exclude(String arg) {
    	this.exclude = arg;
    }
    
    public void include(String arg) {
    	this.include = arg;
    }
    
    public void setAnnotation(String arg) {
    	this.annotation = arg;
    }
    
    public void setClassified(String arg) {
    	this.classified = arg;
    }
    
    public void setSerious(String arg) {
    	this.serious = arg;
    }
    
    public void setAfter(String arg) {
    	this.after = arg;
    }
    
    public void setBefore(String arg) {
    	this.before = arg;
    }
    
    public void setFirst(String arg) {
    	this.first = arg;
    }
    
    public void setLast(String arg) {
    	this.last = arg;
    }
    
    public void setFixed(String arg) {
    	this.fixed = arg;
    }
    
    public void setPresent(String arg) {
    	this.present = arg;
    }
    
    public void setAbsent(String arg) {
    	this.absent = arg;
    }
    
    public void setActive(String arg) {
    	this.active = arg;
    }
    
    public void setIntroducedByChange(String arg) {
    	this.introducedByChange = arg;
    }
    
    public void setRemovedByChange(String arg) {
    	this.removedByChange = arg;
    }
    
    public void setNewCode(String arg) {
    	this.newCode = arg;
    }
    
    public void setRemovedCode(String arg) {
    	this.removedCode = arg;
    }
    
    public void setPriority(String arg) {
    	this.priority = arg;
    }
    
    public void setClass(String arg) {
    	this.clazz = arg;
    }
    
    public void setBugPattern(String arg) {
    	this.bugPattern = arg;
    }
    
    public void setCategory(String arg) {
    	this.category = arg;
    }
    
    public void setDesignation(String arg) {
    	this.designation = arg;
    }
    
    public void setWithMessages(String arg) {
    	this.withMessages = arg;
    }
    
    public void setExcludeBugs(String arg) {
    	this.excludeBugs = arg;
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
    	
    	checkBoolean(withSource, "withSource");
    	checkBoolean(classified, "classified");
    	checkBoolean(serious, "serious");
    	checkBoolean(active, "active");
    	checkBoolean(introducedByChange, "introducedByChange");
    	checkBoolean(removedByChange, "removedByChange");
    	checkBoolean(newCode, "newCode");
    	checkBoolean(removedCode, "removedCode");
    	checkBoolean(withMessages, "withMessages");
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
		if (not != null) {
			addArg("-not");
		}
		addBoolOption("-withSource", withSource);
		addOption("-exclude", exclude);
		addOption("-include", include);
		addOption("-annotation", annotation);
		addBoolOption("-classified", classified);
		addBoolOption("-serious", serious);
		addOption("-after", after);
		addOption("-before", before);
		addOption("-first", first);
		addOption("-last", last);
		addOption("-fixed", fixed);
		addOption("-present", present);
		addOption("-absent", absent);
		addBoolOption("-active", active);
		addBoolOption("-introducedByChange", introducedByChange);
		addBoolOption("-removedByChange", removedByChange);
		addBoolOption("-newCode", newCode);
		addBoolOption("-removedCode", removedCode);
		addOption("-priority", priority);
		addOption("-class", clazz);
		addOption("-bugPattern", bugPattern);
		addOption("-category", category);
		addOption("-designation", designation);
		addBoolOption("-withMessages", withMessages);
		if (excludeBugs != null) {
			addArg("-excludeBugs");
			addArg(excludeBugs);
		}
		
		addArg(inputFile.getName());
		
		getFindbugsEngine().setOutput(outputFile);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#beforeExecuteJavaProcess()
	 */
	@Override
	protected void beforeExecuteJavaProcess() {
		log("running filterBugs...");
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
