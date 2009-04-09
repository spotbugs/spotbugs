/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.cloud;

import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * @author pwilliam
 */
public  class DBCloud extends AbstractCloud {

	
	Mode mode;
	
	public Mode getMode() {
		return mode;
	}
	public void setMode(Mode mode) {
		this.mode = mode;
	}
	class BugData {
		final String instanceHash;

		public BugData(String instanceHash) {
			this.instanceHash = instanceHash;
		}
		int id;
		long firstSeen;
		SortedSet<BugDesignation> designations = new TreeSet<BugDesignation>();
		Collection<BugInstance> bugs = new LinkedHashSet<BugInstance>();
		
		@CheckForNull BugDesignation getPrimaryDesignation() {
			return mode.getPrimaryDesignation(findbugsUser, designations);
		}

		@CheckForNull BugDesignation getUserDesignation() {
			for(BugDesignation d : designations) 
				if (d.getUser().equals(findbugsUser))
					return d;
			return null;
		}
		@CheckForNull BugDesignation getNonnullUserDesignation() {
			BugDesignation d = getUserDesignation();
			if (d != null) 
				return d;
			d = new BugDesignation(UserDesignation.UNCLASSIFIED.name(), System.currentTimeMillis(), "", findbugsUser);
			designations.add(d);
			return d;
		}
		/**
         * @return
         */
        public boolean canSeeCommentsByOthers() {
	       switch(mode) {
	       case SECRET: return false;
	       case COMMUNAL : return true;
	       case VOTING : return hasVoted();
	       }
	       throw new IllegalStateException();
        }
        
        public boolean hasVoted() {
        	for(BugDesignation bd : designations)
        		if (bd.getUser().equals(findbugsUser)) 
        			return true;
        	return false;
        }
	}

	Map<String, BugData> instanceMap = new HashMap<String, BugData>();

	Map<Integer, BugData> idMap = new HashMap<Integer, BugData>();

	BugData getBugData(String instanceHash) {
		BugData bd = instanceMap.get(instanceHash);
		if (bd == null) {
			bd = new BugData(instanceHash);
			instanceMap.put(instanceHash, bd);
		}
		return bd;

	}
	BugData getBugData(BugInstance bug) {
		BugData bugData = getBugData(bug.getInstanceHash());
		bugData.bugs.add(bug);
		return bugData;

	}

	void loadDatabaseInfo(String hash, int id, long firstSeen) {
		BugData bd = instanceMap.get(hash);
		if (bd == null)
			return;
		if (idMap.containsKey(id)) {
			assert bd == idMap.get(id);
			assert bd.id == id;
			assert bd.firstSeen == firstSeen;
		} else {
			bd.id = id;
			bd.firstSeen = firstSeen;
			idMap.put(id, bd);
		}
	}

	final BugCollection bugs;

	DBCloud(BugCollection bugs) {
		this.bugs = bugs;
	}

	public boolean establishConnection() {

		for (BugInstance b : bugs.getCollection())
			if (!skipBug(b))
				getBugData(b.getInstanceHash()).bugs.add(b);

		int count = 0;
		int updated = 0;
		try {
			Connection c = getConnection();
			PreparedStatement ps = c.prepareStatement("SELECT id, hash, firstSeen, FROM findbugsIssues");
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				int col = 1;
				int id = rs.getInt(col++);
				String hash = rs.getString(col++);
				Date firstSeen = rs.getDate(col++);
				loadDatabaseInfo(hash, id, firstSeen.getTime());
			}
			rs.close();
			ps.close();
			
			ps = c.prepareStatement("SELECT issueId, who, designation, comment, when FROM findbugsVotes");
			rs = ps.executeQuery();

			while (rs.next()) {
				int col = 1;
				int id = rs.getInt(col++);
				String who = rs.getString(col++);
				String designation = rs.getString(col++);
				String comment = rs.getString(col++);
				Date when = rs.getDate(col++);
				BugData data = idMap.get(id);
				if (data != null) {
					BugDesignation bd = new BugDesignation(designation, when.getTime(), comment, who);
					data.designations.add(bd);
				}

			}
			rs.close();
			ps.close();
			c.close();
		} catch (Exception e) {
			displayMessage("problem bulk loading database", e);
			return false;
		}
		return true;
	}

	private String getProperty(String propertyName) {
		return SystemProperties.getProperty("findbugs.jdbc." + propertyName);
	}

	String url, dbUser, dbPassword, findbugsUser, dbName;

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, dbUser, dbPassword);
	}

	private boolean setProperties() {
		String sqlDriver = getProperty("dbDriver");
		url = getProperty("dbUrl");
		dbName = getProperty("dbName");
		dbUser = getProperty("dbUser");
		dbPassword = getProperty("dbPassword");
		findbugsUser = getProperty("findbugsUser");
		if (sqlDriver == null || dbUser == null || url == null || dbPassword == null)
			return false;
		if (findbugsUser == null)
			findbugsUser = System.getProperty("user.name", "");
		try {
			Class.forName(sqlDriver);
			Connection c = getConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from  findbugsIssues");
			boolean result = false;
			if (rs.next()) {
				int count = rs.getInt(1);
				if (!GraphicsEnvironment.isHeadless() && bugs.getProject().isGuiAvaliable()) {
					int choice = bugs.getProject().getGuiCallback().showConfirmDialog(
					        "Store comments in " + dbName + " as user " + findbugsUser + "?", "Connect to database?",
					        JOptionPane.YES_NO_OPTION);
					result = choice == JOptionPane.YES_OPTION;

				} else
					result = true;
			}
			rs.close();
			stmt.close();
			c.close();
			if (result) {
				runnerThread.setDaemon(true);
				runnerThread.start();
			}
			return result;

		} catch (Exception e) {

			displayMessage("Unable to connect to database", e);
			return false;
		}
	}

	final LinkedBlockingQueue<Update> queue = new LinkedBlockingQueue<Update>();

	volatile boolean shutdown = false;

	final DatabaseSyncTask runner = new DatabaseSyncTask();

	final Thread runnerThread = new Thread(runner, "Database synchronization thread");

	public void shutdown() {
		shutdown = true;
		runnerThread.interrupt();
	}

		public void storeUserAnnotation(BugData data, BugDesignation bd) {

		queue.add(new StoreUserAnnotation(data, bd));
		updatedStatus();
	}

	private boolean skipBug(BugInstance bug) {
		return bug.getBugPattern().getCategory().equals("NOISE") || bug.isDead();
	}

	
	private static HashMap<String, Integer> issueId = new HashMap<String, Integer>();

	class DatabaseSyncTask implements Runnable {

		int handled;

		PreparedStatement queryByHash;

		PreparedStatement addNewIssue;

		PreparedStatement updateLastSeen;

		PreparedStatement updateUserAnnotation;

		Connection c;

		public void establishConnection() throws SQLException {
			if (c != null)
				return;
			c = getConnection();
			queryByHash = c
			        .prepareStatement("SELECT id, status, updated, lastSeen, who, comment FROM findbugsIssues WHERE hash=?");
			addNewIssue = c
			        .prepareStatement("INSERT INTO findbugsIssues (firstSeen, lastSeen, updated, who, hash, bugPattern, priority, primaryClass) VALUES (?,?,?,?,?,?,?,?)");
			updateLastSeen = c.prepareStatement("UPDATE findbugsIssues SET lastSeen = ? WHERE id = ?");
			updateUserAnnotation = c
			        .prepareStatement("UPDATE findbugsIssues SET status=?, updated=?, who=?, comment=?, lastSeen=? WHERE id=?");
		}

		public void closeConnection() throws SQLException {
			if (c == null)
				return;
			queryByHash.close();
			addNewIssue.close();
			updateLastSeen.close();
			c.close();
			c = null;
		}

		public void run() {
			try {
				while (!shutdown) {
					Update u = queue.poll(10, TimeUnit.SECONDS);
					if (u == null) {
						closeConnection();
						continue;
					}
					establishConnection();
					u.execute(this);
					if ((handled++) % 100 == 0 || queue.isEmpty()) {
						updatedStatus();
					}

				}
			} catch (RuntimeException e) {
				displayMessage("Runtime exception; database connection shutdown", e);
			} catch (SQLException e) {
				displayMessage("SQL exception; database connection shutdown", e);
			} catch (InterruptedException e) {
				assert true;
			}
			try {
				closeConnection();
			} catch (SQLException e) {

			}

		}

		public void newBug(BugData bug, long timestamp) {
			try {
				if (bug.id != -1) return;
				BugInstance b = bug.bugs.iterator().next();
				
				PreparedStatement insertBugData =  
				        c.prepareStatement("INSERT INTO findbugsIssues (firstSeen, lastSeen, hash, bugPattern, priority, primaryClass) VALUES (?,?,?,?,?,?); SELECT scope_identity()");
				Date date = new Date(timestamp);
				int col = 1;
				insertBugData.setDate(col++, date);
				insertBugData.setDate(col++, date);
				insertBugData.setString(col++, bug.instanceHash);
				insertBugData.setString(col++, b.getBugPattern().getType());
				insertBugData.setInt(col++, b.getPriority());
				insertBugData.setString(col++, b.getPrimaryClass().getClassName());
				ResultSet rs = queryByHash.executeQuery();
				bug.id = rs.getInt(1);
				rs.close();

			} catch (Exception e) {
				displayMessage("Problems looking up user annotations", e);
			}

		}

		private void addEntry(java.sql.Date now, BugInstance bug) throws SQLException {
			int col = 1;
			addNewIssue.setDate(col++, now);
			addNewIssue.setDate(col++, now);
			addNewIssue.setDate(col++, now);
			addNewIssue.setString(col++, findbugsUser);
			addNewIssue.setString(col++, bug.getInstanceHash());
			addNewIssue.setString(col++, bug.getBugPattern().getType());
			addNewIssue.setInt(col++, bug.getPriority());
			ClassAnnotation primaryClass = bug.getPrimaryClass();
			String className;
			if (primaryClass == null)
				className = "UNKNOWN";
			else
				className = primaryClass.getClassName();
			addNewIssue.setString(col++, className);
			addNewIssue.execute();
		}

		private void updatedUserAnnotation(BugData data, BugDesignation bd) throws SQLException {
			if (bd == null)
				return;
			if (!bd.isDirty()) 
				return;
			bd.cleanDirty();
			
		}
	}

	static interface Update {
		void execute(DatabaseSyncTask t) throws SQLException;
	}
	static class StoreNewBug implements Update {

		BugData bugData;
		public void execute(DatabaseSyncTask t) throws SQLException {
	        // TODO Auto-generated method stub
	        
        }
	}
		
	
	static class StoreUserAnnotation implements Update {
		public StoreUserAnnotation(BugData data, BugDesignation designation) {
	        super();
	        this.data = data;
	        this.designation = designation;
        }

		public void execute(DatabaseSyncTask t) throws SQLException {
			// t.updatedUserAnnotation(bug, timestamp);
		}


		final BugData data;

		final BugDesignation designation;
	}

	private void displayMessage(String msg, Exception e) {
		AnalysisContext.logError(msg, e);
		if (!GraphicsEnvironment.isHeadless() && bugs.getProject().isGuiAvaliable()) {
			bugs.getProject().getGuiCallback().showMessageDialog(msg + e.getMessage());
		} else {
			System.err.println(msg);
			e.printStackTrace(System.err);
		}
	}

	private void displayMessage(String msg) {
		if (!GraphicsEnvironment.isHeadless() && bugs.getProject().isGuiAvaliable()) {
			bugs.getProject().getGuiCallback().showMessageDialog(msg);
		} else {
			System.err.println(msg);
		}
	}
	
    public String getUser() {
	   return findbugsUser;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#getFirstSeen(edu.umd.cs.findbugs.BugInstance)
     */
    public long getFirstSeen(BugInstance b) {
	   return getBugData(b).firstSeen;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#getUser()
     */

    public UserDesignation getUserDesignation(BugInstance b) {
    	BugDesignation bd =  getBugData(b).getPrimaryDesignation();
    	if (bd == null) return UserDesignation.UNCLASSIFIED;
    	return UserDesignation.valueOf(bd.getDesignationKey());
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#getUserEvaluation(edu.umd.cs.findbugs.BugInstance)
     */
    public String getUserEvaluation(BugInstance b) {
    	BugDesignation bd =  getBugData(b).getPrimaryDesignation();
    	if (bd == null) return "";
    	return bd.getAnnotationText();
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#getUserTimestamp(edu.umd.cs.findbugs.BugInstance)
     */
    public long getUserTimestamp(BugInstance b) {
    	BugDesignation bd =  getBugData(b).getPrimaryDesignation();
    	if (bd == null) return Long.MAX_VALUE;
    	return bd.getTimestamp();
    	
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#setUserDesignation(edu.umd.cs.findbugs.BugInstance, edu.umd.cs.findbugs.cloud.UserDesignation, long)
     */
    public void setUserDesignation(BugInstance b, UserDesignation u, long timestamp) {
    		
    	BugData data = getBugData(b);
    	
	    BugDesignation bd = data.getUserDesignation();
	    if (bd == null) {
	    	if (u == UserDesignation.UNCLASSIFIED) 
	    		return;
	    	bd = data.getNonnullUserDesignation();
	    }
	    bd.setDesignationKey(u.name());
	    if (bd.isDirty()) {
	    	bd.setTimestamp(timestamp);
	    	storeUserAnnotation(data, bd);
	    }
	    	
	    
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#setUserEvaluation(edu.umd.cs.findbugs.BugInstance, java.lang.String, long)
     */
    public void setUserEvaluation(BugInstance b, String e, long timestamp) {
	    // TODO Auto-generated method stub
	    
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#setUserTimestamp(edu.umd.cs.findbugs.BugInstance, long)
     */
    public void setUserTimestamp(BugInstance b, long timestamp) {
	    // TODO Auto-generated method stub
	    
    }
    
	public String getCloudReport(BugInstance b) {
		StringBuilder builder = new StringBuilder();
		BugData bd = getBugData(b);
		long firstSeen = bd.firstSeen;
		if (firstSeen < Long.MAX_VALUE) {
			builder.append(String.format("First seen %s\n", new Date(firstSeen)));
		}
		BugDesignation primaryDesignation = bd.getPrimaryDesignation();
		boolean canSeeCommentsByOthers = bd.canSeeCommentsByOthers();
		for(BugDesignation d : bd.designations) 
			if (d != primaryDesignation 
					&& (canSeeCommentsByOthers || d.getUser().equals(findbugsUser))) {
				builder.append(String.format("%s: %s\n", d.getUser(), d.getDesignationKey()));
				if (d.getAnnotationText().length() > 0) {
					builder.append(d.getAnnotationText());
					builder.append("\n\n");
				}
			}
		return builder.toString();
	}
}
