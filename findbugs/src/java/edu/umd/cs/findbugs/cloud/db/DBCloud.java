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

package edu.umd.cs.findbugs.cloud.db;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StartTime;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Multiset;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pwilliam
 */
public  class DBCloud extends AbstractCloud {

	static final boolean THROW_EXCEPTION_IF_CANT_CONNECT = false;
	/**
     * 
     */
    private static final String USER_NAME = "user.name";
	Mode mode = Mode.COMMUNAL;
	
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
		long bugFiled = Long.MAX_VALUE;
		SortedSet<BugDesignation> designations = new TreeSet<BugDesignation>();
		Collection<BugInstance> bugs = new LinkedHashSet<BugInstance>();
		long lastSeen;
		
		@CheckForNull
		BugDesignation getPrimaryDesignation() {
			for (BugDesignation bd : designations)
				if (findbugsUser.equals(bd.getUser()))
					return bd;

			return null;
		}


		@CheckForNull BugDesignation getUserDesignation() {
			for(BugDesignation d : designations) 
				if (findbugsUser.equals(d.getUser()))
					return new BugDesignation(d);
			return null;
		}
		
		Collection<BugDesignation> getUniqueDesignations() {
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
			for(BugDesignation bd : getUniqueDesignations()) {
				if (bd.getDesignationKey().equals(UserDesignation.I_WILL_FIX.name()))
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
	Date resync;
	Date attemptedResync;
	
	int resyncCount;
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
		try {
	        initialSyncDone.await();
        } catch (InterruptedException e) {
	       throw new RuntimeException(e);
        }
		BugData bugData = getBugData(bug.getInstanceHash());
		bugData.bugs.add(bug);
		return bugData;

	}

	void loadDatabaseInfo(String hash, int id, long firstSeen, long lastSeen) {
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
			bd.lastSeen = lastSeen;
			
			bd.inDatabase = true;
			idMap.put(id, bd);
		}
	}

	
	public DBCloud(BugCollection bugs) {
		super(bugs);
		sqlDriver = getProperty("dbDriver");
		url = getProperty("dbUrl");
		dbName = getProperty("dbName");
		dbUser = getProperty("dbUser");
		dbPassword = getProperty("dbPassword");
		findbugsUser = getProperty("findbugsUser");
	}
	
	public boolean availableForInitialization() {
		if (sqlDriver == null || dbUser == null || url == null || dbPassword == null) {
			if (THROW_EXCEPTION_IF_CANT_CONNECT)
				throw new RuntimeException("Unable to load database properties");
			return false;
		}
		return true;
	}
	static final Pattern FORBIDDEN_PACKAGE_PREFIXES = Pattern.compile(SystemProperties.getProperty("findbugs.forbiddenPackagePrefixes", " none ").replace(',','|'));
	static final boolean PROMPT_FOR_USER_NAME = SystemProperties.getBoolean("findbugs.db.promptForUserName", false);
	int sessionId = -1;
	final CountDownLatch initialSyncDone = new CountDownLatch(1);
	public void bugsPopulated() {
		queue.add(new PopulateBugs(true));
		
	}
	
	private static final long LAST_SEEN_UPDATE_WINDOW = TimeUnit.MILLISECONDS.convert(7*24*3600, TimeUnit.SECONDS);
	long boundDuration(long milliseconds) {
		if (milliseconds < 0) 
			return 0;
		if (milliseconds > 1000*1000) 
			return 1000*1000;
		return milliseconds;
	}
	static boolean invocationRecorded;
	
	class PopulateBugs implements Update {
		final boolean performFullLoad;
		
		PopulateBugs(boolean performFullLoad) {
			this.performFullLoad = performFullLoad;
		}
	    public void execute(DatabaseSyncTask t) throws SQLException {

	    	if (startShutdown) return;
	    	String commonPrefix = null;
			int updates = 0;
			if (performFullLoad) {
				for (BugInstance b : bugCollection.getCollection())
					if (!skipBug(b)) {
						commonPrefix = Util.commonPrefix(commonPrefix, b.getPrimaryClass().getClassName());
						getBugData(b.getInstanceHash()).bugs.add(b);
					}
				if (commonPrefix == null)
					commonPrefix = "<no bugs>";
				else if (commonPrefix.length() > 128)
					commonPrefix = commonPrefix.substring(0, 128);
			}
			try {
				long startTime = System.currentTimeMillis();

				Connection c = getConnection();
				PreparedStatement ps;
				ResultSet rs;
				if (performFullLoad) {
					ps = c.prepareStatement("SELECT id, hash, firstSeen, lastSeen FROM findbugs_issue");
					rs = ps.executeQuery();

					while (rs.next()) {
						int col = 1;
						int id = rs.getInt(col++);
						String hash = rs.getString(col++);
						Timestamp firstSeen = rs.getTimestamp(col++);
						Timestamp lastSeen = rs.getTimestamp(col++);
						
						loadDatabaseInfo(hash, id, firstSeen.getTime(), lastSeen.getTime());
					}
					rs.close();
					ps.close();
				}
				if (startShutdown) return;
		    	
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
						if (data.designations.add(bd)) {
							bugDesignationId.put(bd, id);
							updates++;
							for(BugInstance bug : data.bugs) {
								updatedIssue(bug);
							}
						}

					}

				}
				rs.close();
				ps.close();
				if (startShutdown) return;
		    	
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
						if (Util.nullSafeEquals(data.bugLink, bugReportId)
							&& Util.nullSafeEquals(data.filedBy, whoFiled)
							&& data.bugFiled == whenFiled.getTime()
							&& Util.nullSafeEquals(data.bugAssignedTo, assignedTo)
							&& Util.nullSafeEquals(data.bugStatus, status)
							&& Util.nullSafeEquals(data.bugComponentName, componentName))
							continue;
						
						data.bugLink = bugReportId;
						data.filedBy = whoFiled;
						data.bugFiled = whenFiled.getTime();
						data.bugAssignedTo = assignedTo;
						data.bugStatus = status;
						data.bugComponentName = componentName;
						updates++;
						for(BugInstance bug : data.bugs) {
							updatedIssue(bug);
						}
					}

				}
				rs.close();
				ps.close();
				if (startShutdown) return;
		    	
				if (!invocationRecorded) {
					long jvmStartTime = StartTime.START_TIME - StartTime.VM_START_TIME;
					SortedBugCollection sbc = (SortedBugCollection) bugCollection;
					long findbugsStartTime = sbc.getTimeStartedLoading() - StartTime.START_TIME;

					URL findbugsURL = PluginLoader.getCoreResource("findbugs.xml");
					String loadURL = findbugsURL == null ? "" : findbugsURL.toString();

					long initialLoadTime = sbc.getTimeFinishedLoading() - sbc.getTimeStartedLoading();
					long lostTime = startTime - sbc.getTimeStartedLoading();

					long initialSyncTime = System.currentTimeMillis() - sbc.getTimeFinishedLoading();
					
					
					
					
					String os = SystemProperties.getProperty("os.name", "");
					String osVersion = SystemProperties.getProperty("os.version");
					String jvmVersion = SystemProperties.getProperty("java.runtime.version");
					if (osVersion != null)
						os = os +" " + osVersion;
					PreparedStatement insertSession = c
					        .prepareStatement(
					                "INSERT INTO findbugs_invocation (who, entryPoint, dataSource, fbVersion, os, jvmVersion, jvmLoadTime, findbugsLoadTime, analysisLoadTime, initialSyncTime, numIssues, startTime, commonPrefix)"
					                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
					Timestamp now = new Timestamp(startTime);
					int col = 1;
					insertSession.setString(col++, findbugsUser);
					insertSession.setString(col++, limitToMaxLength(loadURL, 128));
					insertSession.setString(col++, limitToMaxLength(sbc.getDataSource(), 128));
					insertSession.setString(col++, Version.RELEASE);
					insertSession.setString(col++, limitToMaxLength(os,128));
					insertSession.setString(col++, limitToMaxLength(jvmVersion,64));
					insertSession.setLong(col++, boundDuration(jvmStartTime));
					insertSession.setLong(col++, boundDuration(findbugsStartTime));
					insertSession.setLong(col++, boundDuration(initialLoadTime));
					insertSession.setLong(col++, boundDuration(initialSyncTime));
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
					invocationRecorded = true;
				}
				c.close();

			} catch (Exception e) {
				e.printStackTrace();
				displayMessage("problem bulk loading database", e);
			}
			if (startShutdown) return;
	    	
			if (!performFullLoad) {
				attemptedResync = new Date();
				if (updates > 0) { 
					resync = attemptedResync;
					resyncCount = updates;
				}
			} else {
				long stillPresentAt = bugCollection.getTimestamp();
				for (BugInstance b : bugCollection.getCollection())
					if (!skipBug(b)) {
						BugData bd = getBugData(b.getInstanceHash());
						if (!bd.inDatabase) {
							storeNewBug(b, stillPresentAt);
						} else {
							long firstVersion = b.getFirstVersion();
							long firstSeen = bugCollection.getAppVersionFromSequenceNumber(firstVersion).getTimestamp();
							if (FindBugs.validTimestamp(firstSeen) && (firstSeen < bd.firstSeen || !FindBugs.validTimestamp(bd.firstSeen))) {
								bd.firstSeen = firstSeen;
								storeFirstSeen(bd);
							} else if (FindBugs.validTimestamp(stillPresentAt) && stillPresentAt > bd.lastSeen + LAST_SEEN_UPDATE_WINDOW) {
								storeLastSeen(bd, stillPresentAt);
							}

							BugDesignation designation = bd.getPrimaryDesignation();
							if (designation != null)
								b.setUserDesignation(new BugDesignation(designation));
						}
					}
				initialSyncDone.countDown();
				assert !scheduled;
				
				if (startShutdown)
					return;
				
				long delay = 10*60*1000; // 10 minutes
				if (!scheduled) {
					try {
				
					resyncTimer.schedule(new TimerTask() {

					@Override
                    public void run() {
						if (attemptedResync == null || lastUpdate.after(attemptedResync) || numSkipped++ > 6) {
							numSkipped = 0;
							queue.add(new PopulateBugs(false));
						}
                    }}, delay, delay);
					} catch (Exception e) {
						AnalysisContext.logError("Error scheduling resync", e);
					}
				}
				scheduled = true;
			}
			updatedStatus();
		}
	}
	 
	boolean scheduled = false;
	int numSkipped = 0;
	private static String limitToMaxLength(String s, int maxLength) {
		if (s.length() <= maxLength)
			return s;
		return s.substring(0, maxLength);
	}

	private String getProperty(String propertyName) {
		return SystemProperties.getProperty("findbugs.jdbc." + propertyName);
	}

	final static int MAX_DB_RANK = SystemProperties.getInt("findbugs.db.maxrank", 12);
	final String url, dbUser, dbPassword, dbName;
	String findbugsUser;
	@CheckForNull Pattern sourceFileLinkPattern;
	String sourceFileLinkFormat;
	String sourceFileLinkFormatWithLine;
	
	String sourceFileLinkToolTip;
	ProjectPackagePrefixes projectMapping = new ProjectPackagePrefixes();
	Map<String,String> prefixBugComponentMapping = new HashMap<String,String>();
	private final String sqlDriver;

	Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, dbUser, dbPassword);
	}


	public boolean initialize() {
		if (!availableForInitialization())
			return false;
		
		String mode = getProperty("votingmode");
		if (mode != null)
			setMode(Mode.valueOf(mode.toUpperCase()));
		
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
				if (THROW_EXCEPTION_IF_CANT_CONNECT)
					throw e;
			}
		}
		
		
		if (findbugsUser == null) {
			if (PROMPT_FOR_USER_NAME) {
				Preferences prefs = Preferences.userNodeForPackage(DBCloud.class);
				findbugsUser = prefs.get(USER_NAME,  null);
				findbugsUser = bugCollection.getProject().getGuiCallback().showQuestionDialog(
						 "Name/handle/email for recording your evaluations?\n"
						 + "(sorry, no authentication or confidentiality currently provided)",
						 "Name for recording your evaluations", 
						 findbugsUser == null ? "" : findbugsUser);
				if (findbugsUser != null)
					prefs.put(USER_NAME, findbugsUser);
			}
			else if (findbugsUser == null)
				findbugsUser = System.getProperty(USER_NAME, "");
			
			if (findbugsUser == null) {
				if (THROW_EXCEPTION_IF_CANT_CONNECT)
					throw new RuntimeException("Unable to get reviewer user name for database");
				return false;
			}
		}
		
		loadBugComponents();
		Connection c = null;
		try {
			Class.forName(sqlDriver);
			c = getConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from  findbugs_issue");
			boolean result = false;
			if (rs.next()) {
				result = true;
			}
			rs.close();
			stmt.close();
			c.close();
			if (result) {
				runnerThread.setDaemon(true);
				runnerThread.start();
				return true;
			} else if (THROW_EXCEPTION_IF_CANT_CONNECT) {
				throw new RuntimeException("Unable to get database results");
				
			} else
				return false;
			

		} catch (RuntimeException e) {
			displayMessage("Unable to connect to " + dbName, e);
			
			if (THROW_EXCEPTION_IF_CANT_CONNECT)
				throw e;
			return false;	
		} catch (Exception e) {
			
			displayMessage("Unable to connect to " + dbName, e);
			if (THROW_EXCEPTION_IF_CANT_CONNECT)
				throw new RuntimeException("Unable to connect to database", e);
			return false;
		} finally {
			Util.closeSilently(c);
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
				if (x == -1) {
					if (!prefixBugComponentMapping.containsKey(""))
						prefixBugComponentMapping.put("", s);
				} else {
					String prefix = s.substring(x+1);
					if (!prefixBugComponentMapping.containsKey(prefix))
						prefixBugComponentMapping.put(prefix, s.substring(0,x));
				}
				
			}
			in.close();
		}
		} catch (IOException e) {
			AnalysisContext.logError("Unable to load bug component properties", e);
		}
    }

	final LinkedBlockingQueue<Update> queue = new LinkedBlockingQueue<Update>();

	volatile boolean shutdown = false;
	volatile boolean startShutdown = false;

	final DatabaseSyncTask runner = new DatabaseSyncTask();

	final Thread runnerThread = new Thread(runner, "Database synchronization thread");

	final Timer resyncTimer = new Timer("Resync scheduler", true);
	@Override
    public void shutdown() {
		
		try {
		startShutdown = true;
		resyncTimer.cancel();
		queue.add(new ShutdownTask());
		Connection c = null;
		try {
			c = getConnection();
			PreparedStatement setEndTime = c.prepareStatement("UPDATE  findbugs_invocation SET endTime = ? WHERE id = ?");
			Timestamp date = new Timestamp(System.currentTimeMillis());
			int col = 1;
			setEndTime.setTimestamp(col++, date);
			setEndTime.setInt(col++, sessionId);
			setEndTime.execute();
			setEndTime.close();
		} catch (Throwable e) {
			// we're in shutdown mode, not going to complain
			assert true;
		} finally {
			Util.closeSilently(c);
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
		} finally {
			shutdown = true;
			runnerThread.interrupt(); 
		} 
	}


	private RuntimeException shutdownException = new RuntimeException("DBCloud shutdown");
	private void checkForShutdown() {
		if (!shutdown) return;
		IllegalStateException e = new IllegalStateException("DBCloud has already been shutdown");
		e.initCause(shutdownException);
		throw e;
	}
	
	public void storeNewBug(BugInstance bug, long analysisTime) {

		checkForShutdown();
		queue.add(new StoreNewBug(bug, analysisTime));
	}

	public void storeFirstSeen(final BugData bd) {
		checkForShutdown();
		queue.add(new Update(){

			public void execute(DatabaseSyncTask t) throws SQLException {
	           t.storeFirstSeen(bd);
	            
            }});
	}
	public void storeLastSeen(final BugData bd, final long timestamp) {
		checkForShutdown();
		queue.add(new Update(){

			public void execute(DatabaseSyncTask t) throws SQLException {
	           t.storeLastSeen(bd, timestamp);
	            
            }});
	}
	public void storeUserAnnotation(BugData data, BugDesignation bd) {
		checkForShutdown();
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

	 private static final String HAS_SKIPPED_BUG = "has_skipped_bugs";
	  
		
	private boolean skipBug(BugInstance bug) {
		boolean result = bug.getBugPattern().getCategory().equals("NOISE") || bug.isDead() || BugRanker.findRank(bug) > MAX_DB_RANK;
		if (result && firstTimeDoing(HAS_SKIPPED_BUG)) {
			bugCollection.getProject().getGuiCallback().showMessageDialog(
						    "To limit database load, some issues are not persisted to database.\n"
							+ "For example, issues with rank greater than " + MAX_DB_RANK + " are not stored in the db.\n"
					        + "One of more of the issues you are reviewing will not be persisted,\n"
					        + "and you will not be able to record an evalution of those issues.\n"
					        + "As we scale up the database, we hope to relax these restrictions");
		}
		return result;
	}

	
	public static final String PENDING = "-- pending --";
	public static final String NONE = "none";

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
				insertEvaluation.close();

			} catch (Exception e) {
				displayMessage("Problems looking up user annotations", e);
			}
			lastUpdate = new Date();
			updatesSentToDatabase++;
		}

		
		public void newBug(BugInstance b) {
			try {
				BugData bug = getBugData(b.getInstanceHash());
				
				if (bug.inDatabase)
					return;
				
				PreparedStatement insertBugData =  
				        c.prepareStatement("INSERT INTO findbugs_issue (firstSeen, lastSeen, hash, bugPattern, priority, primaryClass) VALUES (?,?,?,?,?,?)",  
				        		Statement.RETURN_GENERATED_KEYS);
				int col = 1;
				insertBugData.setTimestamp(col++, new Timestamp(bug.firstSeen));
				insertBugData.setTimestamp(col++,  new Timestamp(bug.lastSeen));
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
		public void storeLastSeen(BugData bug, long timestamp) {
			try {
				
				PreparedStatement insertBugData =  
				        c.prepareStatement("UPDATE  findbugs_issue SET lastSeen = ? WHERE id = ?");
				Timestamp date = new Timestamp(timestamp);
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
        		insertPendingRecord(c, bug, bug.bugFiled, bug.filedBy);
			} catch (Exception e) {
				displayMessage("Problem filing bug", e);
			}
			lastUpdate = new Date();
			updatesSentToDatabase++;
        }


	}

	/**
     * @param bug
     * @return
     * @throws SQLException
     */
     private void insertPendingRecord(Connection c, BugData bug, long when, String who) throws SQLException {
		int pendingId = -1;
		PreparedStatement query = null;
		ResultSet rs = null;
		boolean needsUpdate = false;
		try {
		 query = c
		        .prepareStatement("SELECT  id, bugReportId, whoFiled, whenFiled FROM findbugs_bugreport where hash=?");
		query.setString(1, bug.instanceHash);
		 rs = query.executeQuery();

		while (rs.next()) {
			int col = 1;
			int id = rs.getInt(col++);
			String bugReportId = rs.getString(col++);
			String whoFiled = rs.getString(col++);
			Timestamp whenFiled = rs.getTimestamp(col++);
			if (!bugReportId.equals(PENDING) || !who.equals(whoFiled) && !pendingStatusHasExpired(whenFiled.getTime())) {
				rs.close();
				query.close();
				throw new IllegalArgumentException(whoFiled + " already filed bug report " + bugReportId + " for "
				        + bug.instanceHash);
			}
			pendingId = id;
			needsUpdate = !who.equals(whoFiled);
		}
		} catch (SQLException e) {
			String msg = "Problem inserting pending record for " + bug.instanceHash;
			AnalysisContext.logError(msg, e);
			return;
		} finally {
			rs.close();
			query.close();
		}

		if (pendingId == -1) {
			PreparedStatement insert = c
			        .prepareStatement("INSERT INTO findbugs_bugreport (hash, bugReportId, whoFiled, whenFiled)"
			                + " VALUES (?, ?, ?, ?)");

			Timestamp date = new Timestamp(when);
			int col = 1;
			insert.setString(col++, bug.instanceHash);
			insert.setString(col++, PENDING);
			insert.setString(col++, who);
			insert.setTimestamp(col++, date);
			insert.executeUpdate();
			insert.close();
		}

		else if (needsUpdate) {

			PreparedStatement updateBug = c
			        .prepareStatement("UPDATE  findbugs_bugreport SET whoFiled = ?,  whenFiled = ? WHERE id = ?");
			try {
				int col = 1;
				updateBug.setString(col++, bug.filedBy);
				updateBug.setTimestamp(col++, new Timestamp(bug.bugFiled));
				updateBug.setInt(col++, pendingId);
				updateBug.executeUpdate();
			} catch (SQLException e) {
				String msg = "Problem inserting pending record for id " + pendingId + ", bug hash " + bug.instanceHash;
				AnalysisContext.logError(msg, e);
			} finally {
				updateBug.close();
			}
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
        public StoreNewBug(BugInstance bug, long analysisTime) {
	        this.bug = bug;
	        this.analysisTime = analysisTime;
        }
		final BugInstance bug;
		final long analysisTime;
		public void execute(DatabaseSyncTask t) throws SQLException {
	        BugData data = getBugData(bug.getInstanceHash());
	        if (data.lastSeen < analysisTime && FindBugs.validTimestamp(analysisTime))
	        	data.lastSeen = analysisTime;
	       
	         long timestamp = bugCollection.getAppVersionFromSequenceNumber(bug.getFirstVersion()).getTimestamp();
	        data.firstSeen = timestamp;
	        if (data.inDatabase) 
	        	return;
	       
	        t.newBug(bug);
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
		if (bugCollection != null && bugCollection.getProject().isGuiAvaliable()) {
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
    @Override
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
    	if (bd == null) 
    		return UserDesignation.UNCLASSIFIED;
    	return UserDesignation.valueOf(bd.getDesignationKey());
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#getUserEvaluation(edu.umd.cs.findbugs.BugInstance)
     */
    public String getUserEvaluation(BugInstance b) {
    	BugDesignation bd =  getBugData(b).getPrimaryDesignation();
    	if (bd == null) return "";
    	String result =  bd.getAnnotationText();
    	if (result == null)
    		return "";
    	return result;
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
    	
    	if (data == null)
    		return;
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

	private String getLineTerminatedUserEvaluation(BugInstance b) {
		UserDesignation designation = getUserDesignation(b);
		
		String result;
		if (designation != UserDesignation.UNCLASSIFIED)
			result = "Classified as: " +  designation.toString() + "\n";
		else 
			result = "";
		String eval = getUserEvaluation(b).trim();
		if (eval.length() > 0) 
			result = result +  eval + "\n";
		return result;
	}
	String getBugReport(BugInstance b) {
		return getBugReportHead(b) + getBugReportSourceCode(b) + getLineTerminatedUserEvaluation(b) + getBugPatternExplanation(b) + getBugReportTail(b);
	}
	String getBugReportShorter(BugInstance b) {
		return getBugReportHead(b) + getBugReportSourceCode(b) + getLineTerminatedUserEvaluation(b) +  getBugPatternExplanationLink(b) + getBugReportTail(b);
	}
	String getBugReportAbridged(BugInstance b) {
		return getBugReportHead(b) + getBugPatternExplanationLink(b) + getBugReportTail(b);
	}

	String getBugReportSourceCode(BugInstance b) {
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
				SourceFile sourceFile = getBugCollection().getProject().getSourceFinder().findSourceFile(primarySource);
				BufferedReader in = new BufferedReader(new InputStreamReader(sourceFile.getInputStream()));
				int lineNumber = 1;
				String commonWhiteSpace = null;
				List<SourceLine> source = new ArrayList<SourceLine>();
				while (lineNumber <= lastLine + 4) {
					String txt = in.readLine();
					if (txt == null)
						break;
					if (lineNumber >= firstLine - 4) {
						String trimmed = txt.trim();
						if (trimmed.length() == 0) {
							if (lineNumber > lastLine)
								break;
							txt = trimmed;
							
						}
						source.add(new SourceLine(lineNumber, txt));
						commonWhiteSpace = commonLeadingWhitespace(commonWhiteSpace, txt);
					}
					lineNumber++;
				}
				in.close();
				
				out.println("\nRelevant source code:");
				for(SourceLine s : source) {
					if (s.text.length() == 0)
							out.printf("%5d: \n", s.line);
					else 
						out.printf("%5d:   %s\n", s.line, s.text.substring(commonWhiteSpace.length()));
				}
				
				
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

	String commonLeadingWhitespace(String soFar, String txt) {
		if (txt.length() == 0)
			return soFar;
		if (soFar == null) 
			return txt;
		soFar = Util.commonPrefix(soFar, txt);
		for(int i = 0; i < soFar.length(); i++) {
			if (!Character.isWhitespace(soFar.charAt(i)))
					return soFar.substring(0,i);
		}
		return soFar;
		
		
	}
	static class SourceLine {
        public SourceLine(int line, String text) {
	        this.line = line;
	        this.text = text;
        }
		final int line;
		final String text;
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
    
    @Override
    public int getNumberReviewers(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return 0;
    	Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
    	return uniqueDesignations.size();
	  }
    
    @Override
    public double getClassificationScore(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return 0;
    	Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
    	double total = 0;
    	int count = 0;
    	for(BugDesignation d : uniqueDesignations) {
    		UserDesignation designation = UserDesignation.valueOf(d.getDesignationKey());
    		if (nonVoting(designation)) 
    			continue;
			total += designation.score();
			count++;
    	}
    	return total /  count++;
    }
	/**
     * @param designation
     * @return
     */
    private boolean nonVoting(UserDesignation designation) {
	    return designation == UserDesignation.OBSOLETE_CODE
	      || designation == UserDesignation.NEEDS_STUDY
	      || designation == UserDesignation.UNCLASSIFIED;
    }
    
	@Override
    public  double getPortionObsoleteClassifications(BugInstance b) {
		BugData bd = getBugData(b);
    	if (bd == null)
    		return 0;

		int count = 0;
		Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
		for(BugDesignation d : uniqueDesignations) 
			if (UserDesignation.valueOf(d.getDesignationKey()) ==  UserDesignation.OBSOLETE_CODE)
			  count++;
		return ((double)count)/uniqueDesignations.size();
	}
	
    @Override
    public double getClassificationVariance(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return 0;
    	Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
    	double total = 0;
    	double totalSquares = 0;
    	int count = 0;
    	for(BugDesignation d : uniqueDesignations) {
    		UserDesignation designation = UserDesignation.valueOf(d.getDesignationKey());
    		if (nonVoting(designation)) 
    			continue;
    		int score = designation.score();
			total += score;
			totalSquares += score*score;
			count++;
    	}
    	
    	double average = total/count;
    	return totalSquares / count - average*average;
    }
    @Override
    public double getClassificationDisagreement(BugInstance b) {
    	BugData bd = getBugData(b);
    	if (bd == null)
    		return 0;
    	Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
    	int shouldFix = 0;
    	int dontFix = 0;
    	for(BugDesignation d : uniqueDesignations) {
    		UserDesignation designation = UserDesignation.valueOf(d.getDesignationKey());
    		if (nonVoting(designation)) 
    			continue;
    		int score = designation.score();
    		if (score > 0)
    			shouldFix++;
    		else 
    			dontFix++;
    	}
    	return Math.min(shouldFix, dontFix) / (double) (shouldFix + dontFix);
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
    private static void alreadyDone(String activity) {
		Preferences prefs = Preferences.userNodeForPackage(DBCloud.class);
		prefs.putBoolean(activity, true);
	}
    private boolean firstBugRequest = true;
    static final String POSTMORTEM_NOTE = SystemProperties.getProperty("findbugs.postmortem.note");
	static final int POSTMORTEM_RANK = SystemProperties.getInt("findbugs.postmortem.maxRank", 4);
	static final String BUG_LINK_FORMAT = SystemProperties.getProperty("findbugs.filebug.link");
	static final String BUG_LOGIN_LINK = SystemProperties.getProperty("findbugs.filebug.login");
	static final String BUG_LOGIN_MSG = SystemProperties.getProperty("findbugs.filebug.loginMsg");
	
	static final String COMPONENT_FOR_BAD_ANALYSIS = SystemProperties.getProperty("findbugs.filebug.badAnalysisComponent");
	
    @Override
    @CheckForNull
	public URL getBugLink(BugInstance b) {
		BugData bd = getBugData(b);

		String bugNumber = bd.bugLink;
		BugFilingStatus status = getBugLinkStatus(b);
		if (status == BugFilingStatus.VIEW_BUG)
			return getBugViewLink(bugNumber);

		Connection c = null;
		try {
			c = getConnection();
			PreparedStatement ps = c
			        .prepareStatement("SELECT bugReportId, whoFiled, whenFiled, status, assignedTo, componentName FROM findbugs_bugreport WHERE hash=?");
			ps.setString(1, b.getInstanceHash());
			ResultSet rs = ps.executeQuery();

			Timestamp pendingFiledAt = null;

			while (rs.next()) {
				int col = 1;
				String bugReportId = rs.getString(col++);
				String whoFiled = rs.getString(col++);
				Timestamp whenFiled = rs.getTimestamp(col++);
				String statusString = rs.getString(col++);
				String assignedTo = rs.getString(col++);
				String componentName = rs.getString(col++);
				if (bugReportId.equals(PENDING)) {
					if (!findbugsUser.equals(whoFiled) && !pendingStatusHasExpired(whenFiled.getTime()))
						pendingFiledAt = whenFiled;
					continue;
				}
				if (bugReportId.equals(NONE)) 
					continue;
				
				rs.close();
				ps.close();
				bd.bugLink = bugReportId;
				bd.filedBy = whoFiled;
				bd.bugFiled = whenFiled.getTime();
				bd.bugAssignedTo = assignedTo;
				bd.bugStatus = statusString;
				bd.bugComponentName = componentName;
				int answer = getBugCollection().getProject().getGuiCallback().showConfirmDialog(
				        "Sorry, but since the time we last received updates from the database,\n"
				                + "someone else already filed a bug report. Would you like to view the bug report?",
				        "Someone else already filed a bug report", JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.NO_OPTION)
					return null;
				return getBugViewLink(bugReportId);
			}
			rs.close();
			ps.close();

			if (pendingFiledAt != null) {
				bd.bugLink = PENDING;
				bd.bugFiled = pendingFiledAt.getTime();
				getBugCollection().getProject().getGuiCallback().showMessageDialog(
				        "Sorry, but since the time we last received updates from the database,\n"
				                + "someone else already has started a bug report for this issue. ");
				return null;

			}

			// OK, not in database
			if (status == BugFilingStatus.FILE_BUG) {

				URL u = getBugFilingLink(b);
				if (u != null && firstTimeDoing(HAS_FILED_BUGS)) {
					String bugFilingNote = String.format(SystemProperties.getProperty("findbugs.filebug.note", ""));
					int response = bugCollection.getProject().getGuiCallback().showConfirmDialog(
					        "This looks like the first time you've filed a bug from this machine. Please:\n"
					                + " * Please check the component the issue is assigned to; we sometimes get it wrong.\n"
					                + " * Try to figure out the right person to assign it to.\n"
					                + " * Provide the information needed to understand the issue.\n" + bugFilingNote
					                + "Note that classifying an issue is distinct from (and lighter weight than) filing a bug.",
					        "Do you want to file a bug report", JOptionPane.YES_NO_OPTION);
					if (response != JOptionPane.YES_OPTION)
						return null;
				}
				if (u != null)
					insertPendingRecord(c, bd, System.currentTimeMillis(), findbugsUser);
				return u;
			}

			else {
				assert status == BugFilingStatus.FILE_AGAIN;
				alreadyDone(HAS_FILED_BUGS);
				URL u = getBugFilingLink(b);
				if (u != null)
					insertPendingRecord(c, bd, System.currentTimeMillis(), findbugsUser);
				return u;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			Util.closeSilently(c);
		}

		return null;
	}
	/**
     * @param bugNumber
     * @return
     * @throws MalformedURLException
     */
    private @CheckForNull URL getBugViewLink(String bugNumber)  {
	    String viewLinkPattern = SystemProperties.getProperty("findbugs.viewbug.link");
	    if (viewLinkPattern == null)
	    	return null;
	    firstBugRequest = false;
	    String u = String.format(viewLinkPattern, bugNumber);
	    try {
	        return new URL(u);
        } catch (MalformedURLException e) {
	       return null;
        }
    }
	/**
     * @param b
     * @return
     * @throws MalformedURLException
     */
    private URL getBugFilingLink(BugInstance b) throws MalformedURLException {
	    {

	    	if (BUG_LINK_FORMAT == null)
	    		return null;
	    	String report = getBugReport(b);
	    	String component;
	    	if (getUserDesignation(b) == UserDesignation.BAD_ANALYSIS 
	    			&& COMPONENT_FOR_BAD_ANALYSIS != null)
	    		component = COMPONENT_FOR_BAD_ANALYSIS;
	    	else
	    		component = getBugComponent(b.getPrimaryClass().getClassName().replace('.', '/'));
	    	String summary = b.getMessageWithoutPrefix() + " in " + b.getPrimaryClass().getSourceFileName();
	    	
	    	int maxURLLength = MAX_URL_LENGTH;
	    	if (firstBugRequest) {
	    		if (BUG_LOGIN_LINK != null && BUG_LOGIN_MSG != null) {
	    			URL u = new URL(String.format(BUG_LOGIN_LINK));
	    			if (!bugCollection.getProject().getGuiCallback().showDocument(u))
	    				return null;
	    			int r = bugCollection.getProject().getGuiCallback().showConfirmDialog(BUG_LOGIN_MSG, "Logging into bug tracker...", JOptionPane.OK_CANCEL_OPTION);
	    			if (r == JOptionPane.CANCEL_OPTION)
	    				return null;
	    		}
	    		else 
	    			maxURLLength = maxURLLength *2/3;
	    	}
	    	firstBugRequest = false;
	    	String u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
	    	if (u.length() > maxURLLength) {
	    		report = getBugReportShorter(b);
	    		u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
	    		if (u.length() > maxURLLength) {
	    			report = getBugReportAbridged(b);
	    			u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
	    			String supplemental = "[Can't squeeze this information into the URL used to prepopulate the bug entry\n"
	    				                   +" please cut and paste into the bug report as appropriate]\n\n"
	    				                   + getBugReportSourceCode(b) 
	    								 +  getLineTerminatedUserEvaluation(b)
	    								 + getBugPatternExplanation(b);
	    			bugCollection.getProject().getGuiCallback().displayNonmodelMessage(
	    					"Cut and paste as needed into bug entry",
	    					supplemental);
	    			
	    		}
	    	}
	    	return new URL(u);
	    }
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
		SimpleDateFormat format = new SimpleDateFormat("MM/dd, yyyy");
		StringBuilder builder = new StringBuilder();
		BugData bd = getBugData(b);
		long firstSeen = bd.firstSeen;
		if (firstSeen < Long.MAX_VALUE) {
			builder.append(String.format("First seen %s\n", format.format(new Timestamp(firstSeen))));
		}
		
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
			if (findbugsUser.equals(d.getUser())|| canSeeCommentsByOthers ) {
				builder.append(String.format("%s @ %s: %s\n", d.getUser(), format.format(new Timestamp(d.getTimestamp())), 
						i18n.getUserDesignation(d.getDesignationKey())));
				String annotationText = d.getAnnotationText();
				if (annotationText != null && annotationText.length() > 0) {
					builder.append(annotationText);
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
	    updatedIssue(bugInstance);
	    
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
            else {
	            long whenFiled = bd.bugFiled;
	            if (pendingStatusHasExpired(whenFiled))
	            	return BugFilingStatus.FILE_BUG; 
	            else 
	            	return BugFilingStatus.BUG_PENDING;
            }
    	}
    	try {
    		Integer.parseInt(link);
    		return BugFilingStatus.VIEW_BUG;
    		
    	} catch (RuntimeException e) {
    		assert true;
    	}
    	
    	return  BugFilingStatus.NA;
    }
	/**
     * @param whenFiled
     * @return
     */
    private boolean pendingStatusHasExpired(long whenFiled) {
	    return System.currentTimeMillis() - whenFiled > 60*60*1000L;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#bugFiled(edu.umd.cs.findbugs.BugInstance, java.lang.Object)
     */
    public void bugFiled(BugInstance b, Object bugLink) {
    	checkForShutdown();
		
    	if (bugAlreadyFiled(b)) {
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
    	if (numToSync > 0)
    		return  String.format("%d remain to be synchronized", numToSync);
    	else if (resync != null && resync.after(lastUpdate))
    		return  String.format("%d updates received from db at %s", 
    				resyncCount, format.format(resync)); 
    	else if (updatesSentToDatabase == 0)
    		return  String.format("%d issues synchronized with database", 
    				idMap.size());
    	else
    		return  String.format("%d classifications/bug filings sent to db, last updated at %s", 
    				updatesSentToDatabase, format.format(lastUpdate)); 
    		
    }
    
    
    @Override
    public void printCloudSummary(PrintWriter w, Iterable<BugInstance> bugs, String[] packagePrefixes) {
    	
    	Multiset<String> evaluations = new Multiset<String>();
    	Multiset<String> designations = new Multiset<String>();
    	Multiset<String> bugStatus = new Multiset<String>();
    	
    	int issuesWithThisManyReviews [] = new int[100];
    	I18N i18n = I18N.instance();
		Set<String> hashCodes = new HashSet<String>();
		for(BugInstance b : bugs) {
			hashCodes.add(b.getInstanceHash());
		}
		
		int packageCount = 0;
		int classCount = 0;
		int ncss = 0;
		ProjectStats projectStats = bugCollection.getProjectStats();
		for(PackageStats ps : projectStats.getPackageStats()) 
			if (ClassName.matchedPrefixes(packagePrefixes, ps.getPackageName()) &&  ps.size() > 0 && ps.getNumClasses() > 0) {
				packageCount++;
				 ncss += ps.size();
				 classCount += ps.getNumClasses();
		}
		
		
		if (packagePrefixes != null && packagePrefixes.length > 0) {
			String lst = Arrays.asList(packagePrefixes).toString();
			w.println("Code analyzed in " + lst.substring(1, lst.length()-1));
		}
		else 
			w.println("Code analyzed");
		if (classCount == 0)
			w.println("No classes were analyzed");
		else 
			w.printf("%,7d packages\n%,7d classes\n%,7d thousands of lines of non-commenting source statements\n",
					packageCount, classCount, (ncss+999)/1000);
		w.println();
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
    	w.println("People who have performed the most reviews");
    	printLeaderBoard(w, evaluations, 9, findbugsUser, true, "reviewer");
    	
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
     static void printLeaderBoard(PrintWriter w, Multiset<String> evaluations, int maxRows, String alwaysPrint, boolean listRank, String title) {
    	 if (listRank)
 			w.printf("%3s %4s %s\n", "rnk", "num", title);
 		else
 			w.printf("%4s %s\n",  "num", title);
    	printLeaderBoard2(w, evaluations, maxRows, alwaysPrint, listRank ? "%3d %4d %s\n" : "%2$4d %3$s\n"  , title);
    }
     
    static final String LEADERBOARD_BLACKLIST = SystemProperties.getProperty("findbugs.leaderboard.blacklist");
 	static final Pattern LEADERBOARD_BLACKLIST_PATTERN;
 	static {
 		Pattern p = null;
 		if (LEADERBOARD_BLACKLIST != null) 
 			try {
 				p = Pattern.compile(LEADERBOARD_BLACKLIST.replace(',', '|'));
 			} catch (Exception e) {
 				assert true;
 			}
 			LEADERBOARD_BLACKLIST_PATTERN = p;	
 			
 	}

	/**
     * @param w
     * @param evaluations
     * @param maxRows
     * @param alwaysPrint
     * @param listRank
     * @param title
     */
     static void printLeaderBoard2(PrintWriter w, Multiset<String> evaluations, int maxRows, String alwaysPrint,
            String format, String title) {
	    int row = 1;
    	int position = 0;
    	int previousScore = -1;
    	boolean foundAlwaysPrint = false;
    		
    	for(Map.Entry<String,Integer> e : evaluations.entriesInDecreasingFrequency()) {
    		int num = e.getValue();
    		if (num != previousScore) {
    			position = row;
    			previousScore = num;
    		}
    		String key = e.getKey();
    		if (LEADERBOARD_BLACKLIST_PATTERN != null && LEADERBOARD_BLACKLIST_PATTERN.matcher(key).matches())
    			continue;
    		
    		boolean shouldAlwaysPrint = key.equals(alwaysPrint);
			if (row <= maxRows || shouldAlwaysPrint) 
				w.printf(format, position, num, key);
			
			if (shouldAlwaysPrint)
				foundAlwaysPrint = true;
    		row++;
    		if (row >= maxRows) {
    			if (alwaysPrint == null) 
    				break;
    			if (foundAlwaysPrint) {
        			w.printf("Total of %d %ss\n", evaluations.numKeys(), title);
        			break;
        		} 
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
    
    
    
    public boolean getBugIsUnassigned(BugInstance b) {
		BugData bd = getBugData(b);
		return bd != null && bd.inDatabase && getBugLinkStatus(b) == BugFilingStatus.VIEW_BUG
		        && ("NEW".equals(bd.bugStatus) || bd.bugAssignedTo == null || bd.bugAssignedTo.length() == 0);
	}

	public boolean getWillNotBeFixed(BugInstance b) {
		BugData bd = getBugData(b);
		return bd != null && bd.inDatabase && getBugLinkStatus(b) == BugFilingStatus.VIEW_BUG
		        && "WILL_NOT_FIX".equals(bd.bugStatus);
	}
		/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#supportsCloudSummaries()
     */
    @Override
    public boolean supportsCloudSummaries() {
	   return true;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#canStoreUserAnnotation(edu.umd.cs.findbugs.BugInstance)
     */
    @Override
    public boolean canStoreUserAnnotation(BugInstance bugInstance) {
	   return !skipBug(bugInstance);
    }
    
	@Override
    public @CheckForNull String claimedBy(BugInstance b) {
		BugData bd = getBugData(b);
		if (bd == null)
			return null;
		for(BugDesignation designation : bd.getUniqueDesignations()) {
			if ("I_WILL_FIX".equals(designation.getDesignationKey()))
					return designation.getUser();
		}
		return null;
		
	}
	
}
