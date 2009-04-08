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

package edu.umd.cs.findbugs.userAnnotations.ri;

import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.userAnnotations.UserAnnotationPlugin;

/**
 * @author pwilliam
 */
public class JDBCUserAnnotationPlugin implements UserAnnotationPlugin {

	int bulkSynchronizations = 0;

	static private <T> Collection<T> addTo(Collection<T> c, T t) {
		if (c == null)
			return Collections.singleton(t);
		if (c.size() == 1)
			c = new ArrayList<T>(c);
		c.add(t);
		return c;

	}

	private int bulkLoad(BugCollection b) {
		HashMap<String, Collection<BugInstance>> bugs = new HashMap<String, Collection<BugInstance>>();
		for (BugInstance bug : b) {
			final String hash = bug.getInstanceHash();
			Collection<BugInstance> c = bugs.get(hash);
			bugs.put(hash, addTo(c, bug));
		}
		bulkSynchronizations += bugs.size();
		int count = 0;
		int updated = 0;
		try {
			Connection c = getConnection();
			PreparedStatement ps = c
			        .prepareStatement("SELECT id, status, updated, lastSeen, who, comment, hash FROM findbugsIssues WHERE who = ?");
			ps.setString(1, findbugsUser);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				int col = 1;
				int id = rs.getInt(col++);
				String designationString = rs.getString(col++);
				Date when = rs.getDate(col++);
				Date lastSeen = rs.getDate(col++);
				String who = rs.getString(col++);
				String comment = rs.getString(col++);
				String hash = rs.getString(col++);
				Collection<BugInstance> collection = bugs.get(hash);
				if (collection != null && designationString.length() != 0)
					for (BugInstance bug : collection) {
						BugDesignation bd = new BugDesignation(designationString, when.getTime(), comment, who);
						bug.setUserDesignation(bd);
						updatedIssue(bug);
						updated++;
					}
				count++;
			}
			rs.close();
			ps.close();
			c.close();
		} catch (Exception e) {
			displayMessage("problem bulk loading database", e);
		}
		return count;
	}

	private final Project project;

	/**
	 * @param project
	 *            never null
	 */
	public JDBCUserAnnotationPlugin(Project project) {
		if (project == null) {
			throw new IllegalArgumentException("Project must be non null");
		}
		this.project = project;
	}

	public static JDBCUserAnnotationPlugin getPlugin(Project project) {

		JDBCUserAnnotationPlugin plugin = new JDBCUserAnnotationPlugin(project);
		if (!plugin.setProperties())
			return null;
		return plugin;
	}

	CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<Listener>();

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	private void updatedStatus() {
		for (Listener listener : listeners)
			listener.statusUpdated();
	}

	private void updatedIssue(BugInstance bug) {
		for (Listener listener : listeners)
			listener.issueUpdate(bug);
	}

	//dbDriver=com.mysql.jdbc.Driver,dbUrl=jdbc:mysql://localhost/findbugs,dbUser
	// =root,dbPassword=

	private String getProperty(String propertyName) {
		return SystemProperties.getProperty("findbugs.jdbc." + propertyName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.Plugin#setProperties(java.util.Map)
	 */
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
				if (!GraphicsEnvironment.isHeadless() && project.isGuiAvaliable()) {
					int choice = project.getGuiCallback().showConfirmDialog(
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

	/**
	 * @param e
	 */
	private void displayMessage(String msg, Exception e) {
		AnalysisContext.logError(msg, e);
		if (!GraphicsEnvironment.isHeadless() && project.isGuiAvaliable()) {
			project.getGuiCallback().showMessageDialog(msg + e.getMessage());
		} else {
			System.err.println(msg);
			e.printStackTrace(System.err);
		}
	}

	private void displayMessage(String msg) {
		if (!GraphicsEnvironment.isHeadless() && project.isGuiAvaliable()) {
			project.getGuiCallback().showMessageDialog(msg);
		} else {
			System.err.println(msg);
		}
	}

	String url, dbUser, dbPassword, findbugsUser, dbName;

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, dbUser, dbPassword);
	}

	static interface Update  {
		 void execute(DatabaseSyncTask t) throws SQLException;
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

	final LinkedBlockingQueue<Update> queue = new LinkedBlockingQueue<Update>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.Plugin#loadUserAnnotations(edu.umd
	 * .cs.findbugs.BugCollection)
	 */
	public void loadUserAnnotations(BugCollection bugs) {
		bulkLoad(bugs);
		updatedStatus();
	}

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
				queue.add(new StoreUserAnnotation(bug,  now));
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

		public void newBug(BugInstance bug, long timestamp) {
			try {
				BugDesignation bd = bug.getUserDesignation();
				queryByHash.setString(1, bug.getInstanceHash());
				ResultSet rs = queryByHash.executeQuery();
				if (rs.next()) {
					int col = 1;
					int id = rs.getInt(col++);
					issueId.put(bug.getInstanceHash(), id);
					String designationString = rs.getString(col++);
					Date when = rs.getDate(col++);
					Date lastSeen = rs.getDate(col++);
					String who = rs.getString(col++);
					String comment = rs.getString(col++);

					boolean updateDatabase = bd != null && when.getTime() < bd.getTimestamp()
					        && !bd.getDesignationKey().equals("UNCLASSIFIED");
					if (updateDatabase) {
						col = 1;
						updateUserAnnotation.setString(col++, bd.getDesignationKey());
						updateUserAnnotation.setDate(col++, new java.sql.Date(bd.getTimestamp()));
						updateUserAnnotation.setString(col++, findbugsUser);
						String annotationText = bd.getNonnullAnnotationText();
						updateUserAnnotation.setString(col++, annotationText);
						updateUserAnnotation.setDate(col++, new java.sql.Date(Math.max(bd.getTimestamp(), when.getTime())));
						updateUserAnnotation.setInt(col++, id);

						updateUserAnnotation.execute();

					} else {
						if (designationString.length() != 0) {
							bd = new BugDesignation(designationString, when.getTime(), comment, who);
							bug.setUserDesignation(bd);
							updatedIssue(bug);
						}
						if (timestamp - lastSeen.getTime() > TimeUnit.MILLISECONDS.convert(7 * 24 * 3600, TimeUnit.SECONDS)) {
							// More than one week old, update last seen
							updateLastSeen.setInt(2, id);
							updateLastSeen.setDate(1, new java.sql.Date(timestamp));
							updateLastSeen.execute();
						}
					}

				} else {
					addEntry(new java.sql.Date(timestamp), bug);
				}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.UserAnnotationPlugin#getStatusMsg()
	 */
	public String getStatusMsg() {
		int size = queue.size();
		if (!runnerThread.isAlive())
			return "failed to synchronized with " + dbName + "(" + size + " issues unsynchronized)";
		if (size == 0)
			return "Synchronized " + (bulkSynchronizations + runner.handled) + " times with " + dbName;
		return "need to synchronize " + size + " issues with " + dbName;

	}

}
