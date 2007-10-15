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
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;

/**
 * Ant task to create/update a bug history database.
 * 
 * @author David Hovemeyer
 */
public class ComputeBugHistoryTask extends AbstractFindBugsTask {
	
	public static class DataFile {
		String name;

		public DataFile() {
		}

		/**
		 * @param name The name to set.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return Returns the name.
		 */
		public String getName() {
			return name;
		}
	}

	private File outputFile;
	private boolean overrideRevisionNames;
	private boolean noPackageMoves;
	private boolean preciseMatch;
	private boolean precisePriorityMatch;
	private boolean quiet;
	private boolean withMessages;
	private List<DataFile> dataFileList;
	
	public ComputeBugHistoryTask() {
		super("edu.umd.cs.findbugs.workflow.Update");
		dataFileList = new LinkedList<DataFile>();
	}
	
	public void setOutput(File arg) {
		this.outputFile = arg;
	}
	
	public void setOverrideRevisionNames(boolean arg) {
		this.overrideRevisionNames = arg;
	}
	
	public void setNoPackageMoves(boolean arg) {
		this.noPackageMoves = arg;
	}
	
	public void setPreciseMatch(boolean arg) {
		this.preciseMatch = arg;
	}
	
	public void setPrecisePriorityMatch(boolean arg) {
		this.precisePriorityMatch = arg;
	}
	
	public void setQuiet(boolean arg) {
		this.quiet = arg;
	}
	
	public void setWithMessages(boolean arg) {
		this.withMessages = arg;
	}
	
	/**
	 * Called to create DataFile objects in response to nested
	 * &lt;DataFile&gt; elements.
	 * 
	 * @return new DataFile object specifying the location of an input data file
	 */
	public DataFile createDataFile() {
		DataFile dataFile = new DataFile();
		dataFileList.add(dataFile);
		return dataFile;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#checkParameters()
	 */
	@Override
	protected void checkParameters() {
		super.checkParameters();
		
		if (outputFile == null) {
			throw new BuildException("outputFile attribute must be set", getLocation());
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#configureFindbugsEngine()
	 */
	@Override
	protected void configureFindbugsEngine() {
		addArg("-output");
		addArg(outputFile.getPath());
		if (overrideRevisionNames) {
			addArg("-overrideRevisionNames");
		}
		if (noPackageMoves) {
			addArg("-noPackageMoves");
		}
		if (preciseMatch) {
			addArg("-preciseMatch");
		}
		if (precisePriorityMatch) {
			addArg("-precisePriorityMatch");
		}
		if (quiet) {
			addArg("-quiet");
		}
		if (withMessages) {
			addArg("-withMessages");
		}
		
		if (outputFile.exists()) {
			addArg(outputFile.getPath());
		}
		
		for (DataFile dataFile : dataFileList) {
			addArg(dataFile.getName());
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#beforeExecuteJavaProcess()
	 */
	@Override
	protected void beforeExecuteJavaProcess() {
		log("Running computeBugHistory...");
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#afterExecuteJavaProcess(int)
	 */
	@Override
	protected void afterExecuteJavaProcess(int rc) {
		if (rc == 0) {
			log("History database written to " + outputFile.getPath());
		}
	}
}
