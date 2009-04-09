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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
public abstract class DBCloud extends AbstractCloud {

	class BugData {
		final String instanceHash;

		public BugData(String instanceHash) {
			this.instanceHash = instanceHash;
		}

		int id;

		Date firstSeen;

		Collection<BugDesignation> designations = new TreeSet<BugDesignation>();

		Collection<BugInstance> bugs = new ArrayList<BugInstance>();
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

	void loadDatabaseInfo(String hash, int id, Date firstSeen) {
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
			if (skipBug(b))
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
				loadDatabaseInfo(hash, id, firstSeen);
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

	public void storeUserAnnotation(BugInstance bug) {
		if (skipBug(bug))
			return;
		queue.add(new StoreUserAnnotation(bug, System.currentTimeMillis()));
		updatedStatus();
	}

	private boolean skipBug(BugInstance bug) {
		return bug.getBugPattern().getCategory().equals("NOISE") || bug.isDead();
	}

	public void storeUserAnnotations(BugCollection bugs) {
		long now = System.currentTimeMillis();
		for (BugInstance bug : bugs.getCollection()) {
			if (skipBug(bug))
				continue;
			BugDesignation bd = bug.getUserDesignation();
			if (bd != null && bd.isDirty())
				queue.add(new StoreUserAnnotation(bug, now));
		}
		updatedStatus();
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

		private void updatedUserAnnotation(BugInstance bug, long timestamp) throws SQLException {
			BugDesignation bd = bug.getUserDesignation();
			if (bd == null)
				return;
			bd.cleanDirty();
			queryByHash.setString(1, bug.getInstanceHash());
			ResultSet rs = queryByHash.executeQuery();
			if (!rs.next()) {
				rs.close();
				addEntry(new java.sql.Date(System.currentTimeMillis()), bug);
				rs = queryByHash.executeQuery();

			}

			int id = rs.getInt(1);
			Date lastSeen = rs.getDate(4);

			int col = 1;
			updateUserAnnotation.setString(col++, bd.getDesignationKey());
			updateUserAnnotation.setDate(col++, new java.sql.Date(bd.getTimestamp()));
			updateUserAnnotation.setString(col++, findbugsUser);
			String annotationText = bd.getNonnullAnnotationText();
			updateUserAnnotation.setString(col++, annotationText);
			updateUserAnnotation.setDate(col++, new java.sql.Date(Math.max(lastSeen.getTime(), timestamp)));
			updateUserAnnotation.setInt(col++, id);
			boolean result = updateUserAnnotation.execute();
			return;
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
		public void execute(DatabaseSyncTask t) throws SQLException {
			t.updatedUserAnnotation(bug, timestamp);
		}

		public StoreUserAnnotation(BugInstance bug, long timestamp) {
			this.bug = bug;
			this.timestamp = timestamp;
		}

		final BugInstance bug;

		final long timestamp;
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
}
