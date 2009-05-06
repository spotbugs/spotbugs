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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.gui2.MainFrame;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.Multiset;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pwilliam
 */
public  class DBCloud extends AbstractCloud {

	final static long minimumTimestamp = 1000000000000L;
	Mode mode = Mode.VOTING;
	
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
		boolean inDatabase;
		long firstSeen;
		String bugLink = NONE;
		String filedBy;
		String bugStatus;
		String bugAssignedTo;
		String bugComponentName;
		long bugFiled;
		SortedSet<BugDesignation> designations = new TreeSet<BugDesignation>();
		Collection<BugInstance> bugs = new LinkedHashSet<BugInstance>();
		
		@CheckForNull BugDesignation getPrimaryDesignation() {
			BugDesignation primaryDesignation = mode.getPrimaryDesignation(findbugsUser, designations);
			return primaryDesignation;
		}


		@CheckForNull BugDesignation getUserDesignation() {
			for(BugDesignation d : designations) 
				if (findbugsUser.equals(d.getUser()))
					return new BugDesignation(d);
			return null;
		}
		
		Iterable<BugDesignation> getUniqueDesignations() {
			if (designations.isEmpty())
				return Collections.emptyList();
			HashSet<String> reviewers = new HashSet<String>();
			ArrayList<BugDesignation> result = new ArrayList<BugDesignation>(designations.size());
			for(BugDesignation d : designations) 
				if (reviewers.add(d.getUser()))
						result.add(d);
			return result;
		}
		Set<String> getReviewers() {
			HashSet<String> reviewers = new HashSet<String>();
			for(BugDesignation bd : designations)
				reviewers.add(bd.getUser());
			reviewers.remove("");
			reviewers.remove(null);
			return reviewers;
		}
		boolean isClaimed() {
			Set<String> users = new HashSet<String>();
			for(BugDesignation bd : designations) {
				if (users.add(bd.getUser()) 
						&& bd.getDesignationKey().equals(UserDesignation.I_WILL_FIX.name()))
					return true;
				
			}
			return false;
		}
		BugDesignation getNonnullUserDesignation() {
			BugDesignation d = getUserDesignation();
			if (d != null) 
				return d;
			d = new BugDesignation(UserDesignation.UNCLASSIFIED.name(), System.currentTimeMillis(), "", findbugsUser);
			return d;
		}
		/**filebug
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
        		if (findbugsUser.equals(bd.getUser())) 
        			return true;
        	return false;
        }
	}

	int updatesSentToDatabase;
	Date lastUpdate = new Date();
	Map<String, BugData> instanceMap = new HashMap<String, BugData>();

	Map<Integer, BugData> idMap = new HashMap<Integer, BugData>();

	IdentityHashMap<BugDesignation, Integer> bugDesignationId
		= new IdentityHashMap<BugDesignation, Integer>();
	
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
			bd.inDatabase = true;
			bd.bugFiled = Long.MAX_VALUE;
			idMap.put(id, bd);
		}
	}

	
	DBCloud(BugCollection bugs) {
		super(bugs);
	}
	static final Pattern FORBIDDEN_PACKAGE_PREFIXES = Pattern.compile(SystemProperties.getProperty("findbugs.forbiddenPackagePrefixes", " none ").replace(',','|'));
	int sessionId = -1;
	public void bugsPopulated() {
		
		String commonPrefix = null;
		for (BugInstance b : bugCollection.getCollection())
			if (!skipBug(b)) {
				commonPrefix = Util.commonPrefix(commonPrefix, b.getPrimaryClass().getClassName());
				getBugData(b.getInstanceHash()).bugs.add(b);
			}
		if (commonPrefix == null)
			commonPrefix = "<no bugs>";
		else if (commonPrefix.length() > 128)
			commonPrefix = commonPrefix.substring(0, 128);
		try {
			long startTime = System.currentTimeMillis();
			
			Connection c = getConnection();
			PreparedStatement ps = c.prepareStatement("SELECT id, hash, firstSeen FROM findbugs_issue");
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				int col = 1;
				int id = rs.getInt(col++);
				String hash = rs.getString(col++);
				Timestamp firstSeen = rs.getTimestamp(col++);
				loadDatabaseInfo(hash, id, firstSeen.getTime());
			}
			rs.close();
			ps.close();
			ps = c.prepareStatement("SELECT id, issueId, who, designation, comment, time FROM findbugs_evaluation");

			rs = ps.executeQuery();

			while (rs.next()) {
				int col = 1;
				int id = rs.getInt(col++);
				int issueId = rs.getInt(col++);
				String who = rs.getString(col++);
				String designation = rs.getString(col++);
				String comment = rs.getString(col++);
				Timestamp when = rs.getTimestamp(col++);
				BugData data = idMap.get(issueId);
				
				if (data != null) {
					BugDesignation bd = new BugDesignation(designation, when.getTime(), comment, who);
					bugDesignationId.put(bd, id);
					data.designations.add(bd);
				}

			}
			rs.close();
			ps.close();
			
			ps = c.prepareStatement("SELECT hash, bugReportId, whoFiled, whenFiled, status, assignedTo, componentName FROM findbugs_bugreport");

			rs = ps.executeQuery();

			while (rs.next()) {
				int col = 1;
				String hash = rs.getString(col++);
				String bugReportId = rs.getString(col++);
				String whoFiled = rs.getString(col++);
				Timestamp whenFiled = rs.getTimestamp(col++);
				String status = rs.getString(col++);
				String assignedTo = rs.getString(col++);
				String componentName = rs.getString(col++);
				
				BugData data = instanceMap.get(hash);
				
				if (data != null) {
					data.bugLink = bugReportId;
					data.filedBy = whoFiled;
					data.bugFiled = whenFiled.getTime();
					data.bugAssignedTo = assignedTo;
					data.bugStatus = status;
					data.bugComponentName = componentName;
				}

			}
			rs.close();
			ps.close();

			long initialSyncTime = System.currentTimeMillis() - startTime;
			PreparedStatement insertSession =  
				c.prepareStatement("INSERT INTO findbugs_invocation (who, entryPoint, dataSource, fbVersion, initialSyncTime, numIssues, startTime, commonPrefix)"
						+ " VALUES (?,?,?,?,?,?,?,?)",  
						Statement.RETURN_GENERATED_KEYS);
			Timestamp now = new Timestamp(startTime);
			int col = 1;
			insertSession.setString(col++, findbugsUser);
			insertSession.setString(col++, "");
			insertSession.setString(col++,"");
			insertSession.setString(col++, Version.RELEASE);
			insertSession.setLong(col++, initialSyncTime);
			insertSession.setInt(col++, bugCollection.getCollection().size());
			insertSession.setTimestamp(col++, now);
			insertSession.setString(col++, commonPrefix);
			int rowCount = insertSession.executeUpdate();
			rs = insertSession.getGeneratedKeys();
			if (rs.next()) {
				sessionId = rs.getInt(1);	
				
			}
			insertSession.close();
			rs.close();

			c.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			displayMessage("problem bulk loading database", e);
			
		}
		
		for (BugInstance b : bugCollection.getCollection())
			if (!skipBug(b)) {
				BugData bd  = getBugData(b.getInstanceHash());
				if (!bd.inDatabase) {
					storeNewBug(b);
				} else {
					issuesBulkHandled++;
					long firstVersion = b.getFirstVersion();
					long firstSeen = bugCollection.getAppVersionFromSequenceNumber(firstVersion).getTimestamp();
					if (firstSeen < minimumTimestamp) {
						displayMessage("Got timestamp of " + firstSeen + " which is equal to " + new Date(firstSeen));
					}
					else if (firstSeen < bd.firstSeen) {
						bd.firstSeen = firstSeen;
						storeFirstSeen(bd);
					}
					long lastVersion = b.getLastVersion();
					if (lastVersion != -1) {
						long lastSeen = bugCollection.getAppVersionFromSequenceNumber(firstVersion).getTimestamp();
					}
					
					
					BugDesignation designation = bd.getPrimaryDesignation();
					if (designation != null)
						b.setUserDesignation(new BugDesignation(designation));
				}
			}
		
		
	}

	int issuesBulkHandled = 0;
	private String getProperty(String propertyName) {
		return SystemProperties.getProperty("findbugs.jdbc." + propertyName);
	}

	String url, dbUser, dbPassword, findbugsUser, dbName;
	@CheckForNull Pattern sourceFileLinkPattern;
	String sourceFileLinkFormat;
	String sourceFileLinkFormatWithLine;
	
	String sourceFileLinkToolTip;
	ProjectPackagePrefixes projectMapping = new ProjectPackagePrefixes();
	Map<String,String> prefixBugComponentMapping = new HashMap<String,String>();

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, dbUser, dbPassword);
	}

	public boolean initialize() {
		
		String sp = SystemProperties.getProperty("findbugs.sourcelink.pattern");
		String sf = SystemProperties.getProperty("findbugs.sourcelink.format");
		String sfwl = SystemProperties.getProperty("findbugs.sourcelink.formatWithLine");
		
		String stt  = SystemProperties.getProperty("findbugs.sourcelink.tooltip");
		if (sp != null && sf != null) {
			try {
			this.sourceFileLinkPattern = Pattern.compile(sp);
			this.sourceFileLinkFormat = sf;
			this.sourceFileLinkToolTip = stt;
			this.sourceFileLinkFormatWithLine = sfwl;
			} catch (RuntimeException e) {
				AnalysisContext.logError("Could not compile pattern " + sp, e);
			}
		}
		String sqlDriver = getProperty("dbDriver");
		url = getProperty("dbUrl");
		dbName = getProperty("dbName");
		dbUser = getProperty("dbUser");
		dbPassword = getProperty("dbPassword");
		findbugsUser = getProperty("findbugsUser");
		if (sqlDriver == null || dbUser == null || url == null || dbPassword == null)
			return false;
		if (findbugsUser == null) {
			findbugsUser = System.getProperty("user.name", "");
			if (false) try {
				findbugsUser += "@" 
					  + InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {}
		}
		
		loadBugComponents();
		
		try {
			Class.forName(sqlDriver);
			Connection c = getConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from  findbugs_issue");
			boolean result = false;
			if (rs.next()) {
				int count = rs.getInt(1);
				
				result = findbugsUser != null;
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
	
	private String getBugComponent(@SlashedClassName String className) {
		
		int longestMatch = -1;
		String result = null;
		for(Map.Entry<String,String> e : prefixBugComponentMapping.entrySet()) {
			String key = e.getKey();
			if (className.startsWith(key) && longestMatch < key.length()) {
				longestMatch = key.length();
				result = e.getValue();
			}
		}
		return result;
	}
	private void loadBugComponents(){
		try {
	    URL u = PluginLoader.getCoreResource("bugComponents.properties");
		if (u != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			while(true) {
				String s = in.readLine();
				if (s == null) break;
				if (s.trim().length() == 0)
					continue;
				int x = s.indexOf(' ');
				if (x == -1)
					prefixBugComponentMapping.put("", s);
				else
					prefixBugComponentMapping.put(s.substring(x+1), s.substring(0,x));
				
			}
			in.close();
		}
		} catch (IOException e) {
			AnalysisContext.logError("Unable to load bug component properties", e);
		}
    }

	final LinkedBlockingQueue<Update> queue = new LinkedBlockingQueue<Update>();

	volatile boolean shutdown = false;

	final DatabaseSyncTask runner = new DatabaseSyncTask();

	final Thread runnerThread = new Thread(runner, "Database synchronization thread");

	@Override
    public void shutdown() {
		queue.add(new ShutdownTask());
		try {
			Connection c = getConnection();
			PreparedStatement setEndTime = c.prepareStatement("UPDATE  findbugs_invocation SET endTime = ? WHERE id = ?");
			Timestamp date = new Timestamp(System.currentTimeMillis());
			int col = 1;
			setEndTime.setTimestamp(col++, date);
			setEndTime.setInt(col++, sessionId);
			setEndTime.execute();
		} catch (SQLException e) {
			// we're in shutdown mode, not going to complain
			assert true;
		}

		if (!queue.isEmpty() && runnerThread.isAlive()) {
			setErrorMsg("waiting for synchronization to complete before shutdown");
			for (int i = 0; i < 100; i++) {
				if (queue.isEmpty() || !runnerThread.isAlive())
					break;
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		shutdown = true;
		runnerThread.interrupt();
	}

	
	public void storeNewBug(BugInstance bug) {

		queue.add(new StoreNewBug(bug));
		updatedStatus();
	}

	public void storeFirstSeen(final BugData bd) {

		queue.add(new Update(){

			public void execute(DatabaseSyncTask t) throws SQLException {
	           t.storeFirstSeen(bd);
	            
            }});
		updatedStatus();
	}
	public void storeUserAnnotation(BugData data, BugDesignation bd) {

		queue.add(new StoreUserAnnotation(data, bd));
		updatedStatus();
		if (firstTimeDoing(HAS_CLASSIFIED_ISSUES)) {
			String msg = "Classification and comments have been sent to database.\n"
				+ "You'll only see this message the first time your classifcations/comments are sent\n"
				+ "to the database.";
			   if (mode == Mode.VOTING) 
				  msg += "\nOnce you've classified an issue, you can see how others have classified it.";
				msg += "\nYour classification and comments are independent from filing a bug using an external\n"
				      + "bug reporting system.";
			
			bugCollection.getProject().getGuiCallback().showMessageDialog(msg);
		}

	}

	private boolean skipBug(BugInstance bug) {
		return bug.getBugPattern().getCategory().equals("NOISE") || bug.isDead() || BugRanker.findRank(bug) > 12;
	}

	
	private static final String PENDING = "-- pending --";
	private static final String NONE = "none";

	class DatabaseSyncTask implements Runnable {

		int handled;

		Connection c;

		public void establishConnection() throws SQLException {
			if (c != null)
				return;
			c = getConnection();
			}

		public void closeConnection() throws SQLException {
			if (c == null)
				return;
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
			} catch (DatabaseSyncShutdownException e) {
				assert true;
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
		public void newEvaluation(BugData data, BugDesignation bd) {
			if (!data.inDatabase)
				return;
			try {
				data.designations.add(bd);
				if (bd.getUser() == null)
					bd.setUser(findbugsUser);
				if (bd.getAnnotationText() == null)
					bd.setAnnotationText("");
				
				PreparedStatement insertEvaluation =  
				        c.prepareStatement("INSERT INTO findbugs_evaluation (issueId, who, designation, comment, time) VALUES (?,?,?,?,?)",  
				        		Statement.RETURN_GENERATED_KEYS);
				Timestamp date = new Timestamp(bd.getTimestamp());
				int col = 1;
				insertEvaluation.setInt(col++, data.id);
				insertEvaluation.setString(col++, bd.getUser());
				insertEvaluation.setString(col++, bd.getDesignationKey());
				insertEvaluation.setString(col++, bd.getAnnotationText());
				insertEvaluation.setTimestamp(col++, date);
				insertEvaluation.executeUpdate();
				ResultSet rs = insertEvaluation.getGeneratedKeys();
				if (rs.next()) {
					int id = rs.getInt(1);
					bugDesignationId.put(bd, id);
				}
				rs.close();

			} catch (Exception e) {
				displayMessage("Problems looking up user annotations", e);
			}
			lastUpdate = new Date();
			updatesSentToDatabase++;
		}

		
		public void newBug(BugInstance b, long timestamp) {
			try {
				BugData bug = getBugData(b.getInstanceHash());
				
				if (bug.inDatabase)
					return;
				
				PreparedStatement insertBugData =  
				        c.prepareStatement("INSERT INTO findbugs_issue (firstSeen, lastSeen, hash, bugPattern, priority, primaryClass) VALUES (?,?,?,?,?,?)",  
				        		Statement.RETURN_GENERATED_KEYS);
				Timestamp date = new Timestamp(timestamp);
				int col = 1;
				insertBugData.setTimestamp(col++, date);
				insertBugData.setTimestamp(col++, date);
				insertBugData.setString(col++, bug.instanceHash);
				insertBugData.setString(col++, b.getBugPattern().getType());
				insertBugData.setInt(col++, b.getPriority());
				insertBugData.setString(col++, b.getPrimaryClass().getClassName());
				insertBugData.executeUpdate();
				ResultSet rs = insertBugData.getGeneratedKeys();
				if (rs.next()) {
					bug.id = rs.getInt(1);
					bug.inDatabase = true;
				}
				rs.close();
				insertBugData.close();

			} catch (Exception e) {
				displayMessage("Problems looking up user annotations", e);
			}

		}
		public void storeFirstSeen(BugData bug) {
			try {
				
				PreparedStatement insertBugData =  
				        c.prepareStatement("UPDATE  findbugs_issue SET firstSeen = ? WHERE id = ?");
				Timestamp date = new Timestamp(bug.firstSeen);
				int col = 1;
				insertBugData.setTimestamp(col++, date);
				insertBugData.setInt(col++, bug.id);
				insertBugData.executeUpdate();
				insertBugData.close();

			} catch (Exception e) {
				displayMessage("Problems looking up user annotations", e);
			}

		}

		/**
         * @param bd
         */
        public void fileBug(BugData bug) {
        	
        	try {
				PreparedStatement insert = c
				.prepareStatement("INSERT INTO findbugs_bugreport (hash, bugReportId, whoFiled, whenFiled)"
						 + " VALUES (?, ?, ?, ?)");
				
				Timestamp date = new Timestamp(bug.bugFiled);
				int col = 1;
				insert.setString(col++, bug.instanceHash);
				insert.setString(col++, PENDING);
				insert.setString(col++,  bug.filedBy);
				insert.setTimestamp(col++, date);
				
				int count = insert.executeUpdate();
				insert.close();
				if (count == 0) {
					PreparedStatement updateBug =  
				        c.prepareStatement("UPDATE  findbugs_bugreport SET whoFiled = ? and whenFiled = ? WHERE hash = ? and bugReportId = ?");
					col = 1;
					updateBug.setString(col++, bug.filedBy);
					updateBug.setTimestamp(col++, date);
					updateBug.setString(col++, bug.instanceHash);
					updateBug.setString(col++, PENDING);
					count = updateBug.executeUpdate();
					updateBug.close();
				}
				
				

			} catch (Exception e) {
				displayMessage("Problem filing bug", e);
			}
			lastUpdate = new Date();
			updatesSentToDatabase++;
        }


	}

	static interface Update {
		void execute(DatabaseSyncTask t) throws SQLException;
	}
	
	static class ShutdownTask implements Update {
		public void execute(DatabaseSyncTask t)  {
			throw new DatabaseSyncShutdownException();
		}
	}
	
	static class DatabaseSyncShutdownException extends RuntimeException {
		
	}
	
	boolean bugAlreadyFiled(BugInstance b) {
		BugData bd = getBugData(b.getInstanceHash());
		if (bd == null || !bd.inDatabase)
			throw new IllegalArgumentException();
		return bd.bugLink != null && !bd.bugLink.equals(NONE) && !bd.bugLink.equals(PENDING);

	}
	class FileBug implements Update {

        public FileBug(BugInstance bug) {
        	this.bd = getBugData(bug.getInstanceHash());
        	if (bd == null || !bd.inDatabase)
        		throw new IllegalArgumentException();
        	bd.bugFiled = System.currentTimeMillis();
        	bd.bugLink = PENDING;
        	bd.filedBy = findbugsUser;
        }
		final BugData bd;
        public void execute(DatabaseSyncTask t) throws SQLException {
        	  t.fileBug(bd);
 	        
	        
        }
		
	}
	 class StoreNewBug implements Update {
        public StoreNewBug(BugInstance bug) {
	        this.bug = bug;
        }
		final BugInstance bug;
		public void execute(DatabaseSyncTask t) throws SQLException {
	        BugData data = getBugData(bug.getInstanceHash());
	        if (data.inDatabase) 
	        	return;
	        long timestamp = bugCollection.getAppVersionFromSequenceNumber(bug.getFirstVersion()).getTimestamp();
	        t.newBug(bug, timestamp);
	        data.inDatabase = true;
        }
	}
		
	
	 static class StoreUserAnnotation implements Update {
		public StoreUserAnnotation(BugData data, BugDesignation designation) {
	        super();
	        this.data = data;
	        this.designation = designation;
        }

		public void execute(DatabaseSyncTask t) throws SQLException {
			t.newEvaluation(data, new BugDesignation(designation));
		}
		final BugData data;

		final BugDesignation designation;
	}

	private void displayMessage(String msg, Exception e) {
		AnalysisContext.logError(msg, e);
		if (!GraphicsEnvironment.isHeadless() && bugCollection.getProject().isGuiAvaliable()) {
                  StringWriter stackTraceWriter = new StringWriter();
                  PrintWriter printWriter = new PrintWriter(stackTraceWriter);
                  e.printStackTrace(printWriter);
                  bugCollection.getProject().getGuiCallback().showMessageDialog(
                      String.format("%s - %s\n%s", msg, e.getMessage(), stackTraceWriter.toString()));
		} else {
			System.err.println(msg);
			e.printStackTrace(System.err);
		}
	}

	private void displayMessage(String msg) {
		if (!GraphicsEnvironment.isHeadless() && bugCollection.getProject().isGuiAvaliable()) {
			bugCollection.getProject().getGuiCallback().showMessageDialog(msg);
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
    public boolean overallClassificationIsNotAProblem(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return false;
    	int isAProblem = 0;
    	int notAProblem = 0;
    	for(BugDesignation d : bd.getUniqueDesignations() )
    		switch(UserDesignation.valueOf(d.getDesignationKey())) {
    		case I_WILL_FIX:
    		case MUST_FIX:
    		case SHOULD_FIX:
    			isAProblem++;
    			break;
    		case BAD_ANALYSIS:
    		case	NOT_A_BUG: 
    		case 	MOSTLY_HARMLESS:
    		case OBSOLETE_CODE:
    			notAProblem++;
    			break;
    		}
    			
    			
    		return notAProblem > isAProblem;
 	  
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
    	BugData data = getBugData(b);
    	
	    BugDesignation bd = data.getUserDesignation();
	    if (bd == null) {
	    	if (e.length() == 0) 
	    		return;
	    	bd = data.getNonnullUserDesignation();
	    }
	    bd.setAnnotationText(e);
	    if (bd.isDirty()) {
	    	bd.setTimestamp(timestamp);
	    	storeUserAnnotation(data, bd);
	    }
	    
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#setUserTimestamp(edu.umd.cs.findbugs.BugInstance, long)
     */
    public void setUserTimestamp(BugInstance b, long timestamp) {
    	BugData data = getBugData(b);
    	
	    BugDesignation bd = data.getNonnullUserDesignation();

	    bd.setTimestamp(timestamp);
	    storeUserAnnotation(data, bd);
	    
    }
    
    static final String BUG_NOTE = SystemProperties.getProperty("findbugs.bugnote");
	
    String getBugReportHead(BugInstance b) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		out.println("Bug report generated from FindBugs");
		out.println(b.getMessageWithoutPrefix());
		out.println();
		ClassAnnotation primaryClass = b.getPrimaryClass();

		for (BugAnnotation a : b.getAnnotations()) {
			if (a == primaryClass)
				out.println(a);
			else
				out.println("  " + a.toString(primaryClass));
		}
		URL link = getSourceLink(b);

		if (link != null) {
			out.println();
			out.println(sourceFileLinkToolTip + ": " + link);
			out.println();
		}

		if (BUG_NOTE != null) {
			out.println(BUG_NOTE);
			if (POSTMORTEM_NOTE != null && BugRanker.findRank(b) <= POSTMORTEM_RANK && !overallClassificationIsNotAProblem(b))
				out.println(POSTMORTEM_NOTE);
			out.println();
		}

		Collection<String> projects = projectMapping.getProjects(primaryClass.getClassName());
		if (projects != null && !projects.isEmpty()) {
			String projectList = projects.toString();
			projectList = projectList.substring(1, projectList.length() - 1);
			out.println("Possibly part of: " + projectList);
			out.println();
		}
		out.close();
		return stringWriter.toString();
	}

	String getBugPatternExplanation(BugInstance b) {
		String detailPlainText = b.getBugPattern().getDetailPlainText();
		return "Bug pattern explanation:\n" + detailPlainText + "\n\n";
	}

	String getBugPatternExplanationLink(BugInstance b) {
		return "Bug pattern explanation: http://findbugs.sourceforge.net/bugDescriptions.html#" + b.getBugPattern().getType()
		        + "\n";
	}

	String getBugReport(BugInstance b) {
		return getBugReportHead(b) + getBugReportSourceCode(b) + getUserEvaluation(b) + getBugPatternExplanation(b) + getBugReportTail(b);
	}
	String getBugReportShorter(BugInstance b) {
		return getBugReportHead(b) + getBugReportSourceCode(b) + getUserEvaluation(b) +  getBugPatternExplanationLink(b) + getBugReportTail(b);
	}
	String getBugReportAbridged(BugInstance b) {
		return getBugReportHead(b) + getBugPatternExplanationLink(b) + getBugReportTail(b);
	}

	String getBugReportSourceCode(BugInstance b) {
		if (!MainFrame.isAvailable())
			return "";
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		ClassAnnotation primaryClass = b.getPrimaryClass();

		int firstLine = Integer.MAX_VALUE;
		int lastLine = Integer.MIN_VALUE;
		for (BugAnnotation a : b.getAnnotations())
			if (a instanceof SourceLineAnnotation) {
				SourceLineAnnotation s = (SourceLineAnnotation) a;
				if (s.getClassName().equals(primaryClass.getClassName()) && s.getStartLine() > 0) {
					firstLine = Math.min(firstLine, s.getStartLine());
					lastLine = Math.max(lastLine, s.getEndLine());

				}

			}

		SourceLineAnnotation primarySource = primaryClass.getSourceLines();
		if (primarySource.isSourceFileKnown() && firstLine >= 1 && firstLine <= lastLine && lastLine - firstLine < 50) {
			try {
				SourceFile sourceFile = MainFrame.getInstance().getSourceFinder().findSourceFile(primarySource);
				BufferedReader in = new BufferedReader(new InputStreamReader(sourceFile.getInputStream()));
				int lineNumber = 1;
				out.println("\nRelevant source code:");
				while (lineNumber <= lastLine + 4) {
					String txt = in.readLine();
					if (txt == null)
						break;
					if (lineNumber >= firstLine - 4) {
						if (lineNumber > lastLine && txt.trim().length() == 0)
							break;
						out.printf("%4d: %s\n", lineNumber, txt);
					}
					lineNumber++;
				}
				in.close();
				out.println();
			} catch (IOException e) {
				assert true;
			}
			out.close();
			String result = stringWriter.toString();
			return result;

		}
		return "";

	}

	String getBugReportTail(BugInstance b) {
		return "\nFindBugs issue identifier (do not modify or remove): " + b.getInstanceHash();
	}

    
    
    static String urlEncode(String s) {
    	try {
	        return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
	       assert false;
	       return "No utf-8 encoding";
        }
    }
    
    
    public Set<String> getReviewers(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return Collections.emptySet();
    	return bd.getReviewers();
    }
    public boolean isClaimed(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return false;
    	return bd.isClaimed();
    }
    
    static final int MAX_URL_LENGTH = 1999;
    private static final String HAS_FILED_BUGS = "has_filed_bugs";
    private static final String HAS_CLASSIFIED_ISSUES = "has_classified_issues";
    private static boolean firstTimeDoing(String activity) {
		Preferences prefs = Preferences.userNodeForPackage(DBCloud.class);

		if (!prefs.getBoolean(activity, false)) {
			prefs.putBoolean(activity, true);
			return true;
		}
		return false;
	}
    private boolean firstBugRequest = true;
    static final String POSTMORTEM_NOTE = SystemProperties.getProperty("findbugs.postmortem.note");
	static final int POSTMORTEM_RANK = SystemProperties.getInteger("findbugs.postmortem.rank", 4);
	static final String BUG_LINK_FORMAT = SystemProperties.getProperty("findbugs.filebug.link");
	
    @Override
    @CheckForNull 
    public URL getBugLink(BugInstance b) {
		try {
			BugData bd = getBugData(b);
		
			String bugNumber = bd.bugLink;
			BugFilingStatus status = getBugLinkStatus(b);
			switch (status) {
			case VIEW_BUG: {

				String viewLinkPattern = SystemProperties.getProperty("findbugs.viewbug.link");
				if (viewLinkPattern == null)
					return null;
				firstBugRequest = false;
				String u = String.format(viewLinkPattern, bugNumber);
				return new URL(u);
			}
			case FILE_BUG:
			case FILE_AGAIN: {

				if (BUG_LINK_FORMAT == null)
					return null;
				String report = getBugReport(b);
				String component = getBugComponent(b.getPrimaryClass().getClassName().replace('.', '/'));
				String summary = b.getMessageWithoutPrefix() + " in " + b.getPrimaryClass().getSourceFileName();
				if (firstTimeDoing(HAS_FILED_BUGS)) 
					bugCollection.getProject().getGuiCallback().showMessageDialog(
							"This looks like the first time you've filed a bug from this machine. Please:\n"
							+ " * Please check the component the issue is assigned to; we sometimes get it wrong."
							+ " * Try to figure out the right person to assign it to."
							+ " * Provide the information needed to understand the issue."
							+ "Note that classifying an issue is distinct from (and lighter weight than) filing a bug.");
							
							
				
				
				int maxURLLength = MAX_URL_LENGTH;
				if (firstBugRequest) 
					maxURLLength = maxURLLength *2/3;
				firstBugRequest = false;
				String u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
				if (u.length() > MAX_URL_LENGTH) {
					report = getBugReportShorter(b);
					u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
					if (u.length() > MAX_URL_LENGTH) {
						report = getBugReportAbridged(b);
						u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
						String supplemental = "[Can't squeeze this information into the URL used to prepopulate the bug entry\n"
							                   +" please cut and paste into the bug report as appropriate]\n\n"
							                   + getBugReportSourceCode(b) 
											 +  getUserEvaluation(b)
											 + getBugPatternExplanation(b);
						bugCollection.getProject().getGuiCallback().displayNonmodelMessage(
								"Cut and paste into bug entry for " + b.getMessageWithoutPrefix(),
								supplemental);
						
					}
				}
				if (u.length() > MAX_URL_LENGTH - 500)
					setErrorMsg("Bug link length is "+ u.length());
				return new URL(u);
			}
			}
		} catch (Exception e) {

		}
		return null;
	}
    
    @Override
    public boolean supportsCloudReports() {
		return true;
	}

    
    @Override
    public boolean supportsBugLinks() {
		return BUG_LINK_FORMAT != null;
	}

	@Override
    public String getCloudReport(BugInstance b) {
		SimpleDateFormat format = new SimpleDateFormat("MM/dd");
		StringBuilder builder = new StringBuilder();
		BugData bd = getBugData(b);
		long firstSeen = bd.firstSeen;
		if (firstSeen < Long.MAX_VALUE) {
			builder.append(String.format("First seen %s\n", format.format(new Timestamp(firstSeen))));
		}
		
		BugDesignation primaryDesignation = bd.getPrimaryDesignation();
		I18N i18n = I18N.instance();
		boolean canSeeCommentsByOthers = bd.canSeeCommentsByOthers();
		if (canSeeCommentsByOthers) {
			if (bd.bugStatus != null) {
				builder.append(bd.bugComponentName);
				if (bd.bugAssignedTo == null)
					builder.append("\nBug status is " + bd.bugStatus);
				else
					builder.append("\nBug assigned to " + bd.bugAssignedTo + ", status is " + bd.bugStatus);
				
				builder.append("\n\n");
			}
		}
		for(BugDesignation d : bd.getUniqueDesignations()) 
			if (d != primaryDesignation 
					&& (canSeeCommentsByOthers && !findbugsUser.equals(d.getUser()))) {
				builder.append(String.format("%s @ %s: %s\n", d.getUser(), format.format(new Timestamp(d.getTimestamp())), 
						i18n.getUserDesignation(d.getDesignationKey())));
				if (d.getAnnotationText().length() > 0) {
					builder.append(d.getAnnotationText());
					builder.append("\n\n");
				}
			}
		return builder.toString();
	}
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#storeUserAnnotation(edu.umd.cs.findbugs.BugInstance)
     */
    public void storeUserAnnotation(BugInstance bugInstance) {
	    storeUserAnnotation(getBugData(bugInstance), bugInstance.getNonnullUserDesignation());
	    
    }
    
    @Override
    public boolean supportsSourceLinks() {
    	return sourceFileLinkPattern != null;
    	
    }
    
	@Override
    public @CheckForNull
	URL getSourceLink(BugInstance b) {
		if (sourceFileLinkPattern == null)
			return null;

		SourceLineAnnotation src = b.getPrimarySourceLineAnnotation();
		String fileName = src.getSourcePath();
		int startLine = src.getStartLine();

		java.util.regex.Matcher m = sourceFileLinkPattern.matcher(fileName);
		boolean isMatch = m.matches();
		if (isMatch)
			try {
				URL link;
				if (startLine > 0)
					link = new URL(String.format(sourceFileLinkFormatWithLine, m.group(1), startLine, startLine - 10));
				else
					link = new URL(String.format(sourceFileLinkFormat, m.group(1)));
				return link;
			} catch (MalformedURLException e) {
				AnalysisContext.logError("Error generating source link for " + src, e);
			}

		return null;

	}

    @Override
    public String getSourceLinkToolTip(BugInstance b) {
	    return sourceFileLinkToolTip;
    }
  

    @Override
    public BugFilingStatus getBugLinkStatus(BugInstance b) {
    	BugData bd = getBugData(b);
    	String link = bd.bugLink;
    	if (link == null || link.length() == 0 || link.equals(NONE))
    		return BugFilingStatus.FILE_BUG;
    	if (link.equals(PENDING)) {
    		if (findbugsUser.equals(bd.filedBy))
    			return BugFilingStatus.FILE_AGAIN;
    		else if (System.currentTimeMillis() - bd.bugFiled > 2*60*60*1000L)
    			return BugFilingStatus.FILE_BUG; 
    		else 
    			return BugFilingStatus.BUG_PENDING;
    	}
    	try {
    		Integer.parseInt(link);
    		return BugFilingStatus.VIEW_BUG;
    		
    	} catch (RuntimeException e) {
    		assert true;
    	}
    	
    	return  BugFilingStatus.NA;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#bugFiled(edu.umd.cs.findbugs.BugInstance, java.lang.Object)
     */
    public void bugFiled(BugInstance b, Object bugLink) {
    	if (bugAlreadyFiled(b)) {
    		BugData bd = getBugData(b.getInstanceHash());
    		return;
    	}
    	queue.add(new FileBug(b));
    	updatedStatus();
    	}
	    
    
    String errorMsg;
    long errorTime = 0;


    void setErrorMsg(String msg) {
    	errorMsg = msg;
    	errorTime = System.currentTimeMillis();
    	updatedStatus();
    }
    
    void clearErrorMsg() {
    	errorMsg = null;
    	updatedStatus();
    }
    
    @Override
    public String getStatusMsg() {
    	if (errorMsg != null) {
    		if (errorTime + 2 * 60 * 1000 > System.currentTimeMillis()) {
    			errorMsg = null;
    		} else
    			return errorMsg +"; " + getStatusMsg0();
    	}
    	return getStatusMsg0();
    }

    
    public String getStatusMsg0() {
    	SimpleDateFormat format = new SimpleDateFormat("h:mm a");
    	int numToSync = queue.size();
    	int handled = this.issuesBulkHandled + runner.handled;
    	if (numToSync > 0)
    		return  String.format("%d remain to be synchronized", numToSync);
    	else if (updatesSentToDatabase == 0)
    		return  String.format("%d issues synchronized with database", 
    				idMap.size());
    	else return  String.format("%d classifications/bug filings sent to db, last updated at %s", 
    				updatesSentToDatabase, format.format(lastUpdate)); 
    		
    }
    
    
    @Override
    public void printCloudSummary(Iterable<BugInstance> bugs, PrintWriter w) {
    	
    	Multiset<String> evaluations = new Multiset<String>();
    	Multiset<String> designations = new Multiset<String>();
    	Multiset<String> bugStatus = new Multiset<String>();
    	
    	int issuesWithThisManyReviews [] = new int[100];
    	I18N i18n = I18N.instance();
		Set<String> hashCodes = new HashSet<String>();
		for(BugInstance b : bugs) {
			hashCodes.add(b.getInstanceHash());
		}
		
		int count = 0;
		int notInCloud = 0;
		for(String hash : hashCodes) {
			BugData bd = instanceMap.get(hash);
			if (bd == null) { 
				notInCloud++;
				continue;
			}
			count++;
    		HashSet<String> reviewers = new HashSet<String>();
    		if (bd.bugStatus != null)
    			bugStatus.add(bd.bugStatus);
    		for(BugDesignation d : bd.designations) 
    		    if (reviewers.add(d.getUser())) {
    		    	evaluations.add(d.getUser());
    		    	designations.add(i18n.getUserDesignation(d.getDesignationKey()));
    		    }
    		
    		int numReviews = Math.min( reviewers.size(), issuesWithThisManyReviews.length -1);
    		issuesWithThisManyReviews[numReviews]++;
    		
    	}
		if (count == 0) {
			w.printf("None of the %d issues in the current view are in the cloud\n\n", notInCloud);
	    	return;
		}
		if (notInCloud == 0) {
			w.printf("Summary for %d issues that are in the current view\n\n", count);
		}else {
			w.printf("Summary for %d issues that are in the current view and cloud (%d not in cloud)\n\n", count, notInCloud);
		}
    	w.printf("Summary for %d issues that are both in the cloud and current view\n\n", count);
    	w.println("People who have performed the most reviews");
    	printLeaderBoard(w, evaluations, 5, findbugsUser, true, "reviewer");
    	
    	w.println("\nDistribution of evaluations");
    	printLeaderBoard(w, designations, 100, " --- ", false, "designation");
    	
    	w.println("\nDistribution of bug status");
    	printLeaderBoard(w, bugStatus, 100, " --- ", false, "status of filed bug");
    	
    	w.println("\nDistribution of number of reviews");
    	for(int i = 0; i < issuesWithThisManyReviews.length; i++) 
    		if (issuesWithThisManyReviews[i] > 0) {
    		w.printf("%4d  with %3d review", issuesWithThisManyReviews[i], i);
    		if (i != 1) w.print("s");
    		w.println();
    			
    	}
    	
    }
	/**
     * @param w
	 * @param evaluations
	 * @param listRank TODO
	 * @param title TODO
     */
    private void printLeaderBoard(PrintWriter w, Multiset<String> evaluations, int maxRows, String alwaysPrint, boolean listRank, String title) {
	    int row = 1;
    	int position = 0;
    	int previousScore = -1;
    	boolean foundAlwaysPrint = false;
    	if (listRank)
			w.printf("%3s %3s %s\n", "rnk", "num", title);
		else
			w.printf("%3s %s\n",  "num", title);
		
    	for(Map.Entry<String,Integer> e : evaluations.entriesInDecreasingFrequency()) {
    		int num = e.getValue();
    		if (num != previousScore) {
    			position = row;
    			previousScore = num;
    		}
    		String key = e.getKey();
    		
    		boolean shouldAlwaysPrint = key.equals(alwaysPrint);
			if (row <= maxRows || shouldAlwaysPrint) {
				if (listRank) 
					w.printf("%3d %3d %s\n", position, num, key);
				else
					w.printf("%3d %s\n", num, key);
			}
			if (shouldAlwaysPrint)
				foundAlwaysPrint = true;
    		row++;
    		if (foundAlwaysPrint && row >= maxRows) {
    			w.printf("Total of %d %s\n", evaluations.numKeys(), title);
    			break;
    		}
    		
    	}
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#getIWillFix(edu.umd.cs.findbugs.BugInstance)
     */
    @Override
    public boolean getIWillFix(BugInstance b) {
    	if (super.getIWillFix(b))
    		return true;
	   BugData bd =  getBugData(b);
	   return bd != null && findbugsUser.equals(bd.bugAssignedTo);
	   
	   
    }
		/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#supportsCloudSummaries()
     */
    public boolean supportsCloudSummaries() {
	   return true;
    }
}
