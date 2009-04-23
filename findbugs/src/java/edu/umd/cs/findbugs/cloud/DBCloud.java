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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.gui2.MainFrame;

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
		String bugLink, filedBy;
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
		BugDesignation getNonnullUserDesignation() {
			BugDesignation d = getUserDesignation();
			if (d != null) 
				return d;
			d = new BugDesignation(UserDesignation.UNCLASSIFIED.name(), System.currentTimeMillis(), "", findbugsUser);
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
        		if (findbugsUser.equals(bd.getUser())) 
        			return true;
        	return false;
        }
	}

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

	void loadDatabaseInfo(String hash, int id, long firstSeen, String bugDatabaseKey, @CheckForNull Timestamp bugFiled, @CheckForNull String filedBy) {
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
			bd.bugLink = bugDatabaseKey;
			if (bugFiled != null)
				bd.bugFiled = bugFiled.getTime();
			else
				bd.bugFiled = Long.MAX_VALUE;
			bd.filedBy = filedBy;
			idMap.put(id, bd);
		}
	}

	
	DBCloud(BugCollection bugs) {
		super(bugs);
	}

	public void bugsPopulated() {

		
		for (BugInstance b : bugCollection.getCollection())
			if (!skipBug(b))
				getBugData(b.getInstanceHash()).bugs.add(b);

		try {
			Connection c = getConnection();
			PreparedStatement ps = c.prepareStatement("SELECT id, hash, firstSeen, bugDatabaseKey, bugFiled, filedBy FROM findbugs_issue");
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				int col = 1;
				int id = rs.getInt(col++);
				String hash = rs.getString(col++);
				Timestamp firstSeen = rs.getTimestamp(col++);
				String bugDatabaseKey = rs.getString(col++);
				Timestamp bugFiled = rs.getTimestamp(col++);
				String filedBy = rs.getString(col++);
				loadDatabaseInfo(hash, id, firstSeen.getTime(), bugDatabaseKey, bugFiled, filedBy);
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
			c.close();
			
			for (BugInstance b : bugCollection.getCollection())
				if (!skipBug(b)) {
					BugData bd  = getBugData(b.getInstanceHash());
					if (!bd.inDatabase) {
						storeNewBug(b);
					} else {
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
			
			
		} catch (Exception e) {
			e.printStackTrace();
			displayMessage("problem bulk loading database", e);
			
		}
		
	}

	private String getProperty(String propertyName) {
		return SystemProperties.getProperty("findbugs.jdbc." + propertyName);
	}

	String url, dbUser, dbPassword, findbugsUser, dbName;
	@CheckForNull Pattern sourceFileLinkPattern;
	String sourceFileLinkFormat;
	String sourceFileLinkFormatWithLine;
	
	String sourceFileLinkToolTip;
	ProjectPackagePrefixes projectMapping = new ProjectPackagePrefixes();

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, dbUser, dbPassword);
	}

	public boolean initialize() {
		if (!bugCollection.getProject().isGuiAvaliable())
			return false;
		
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
		try {
			Class.forName(sqlDriver);
			Connection c = getConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from  findbugs_issue");
			boolean result = false;
			if (rs.next()) {
				int count = rs.getInt(1);
				if (!GraphicsEnvironment.isHeadless() && bugCollection.getProject().isGuiAvaliable()) {
					if (false) {
					findbugsUser = JOptionPane.showInputDialog("Identification for survey", findbugsUser);
					result = true;
					} else {
						findbugsUser = (String) JOptionPane.showInputDialog(MainFrame.getInstance(), "Connect to database as", "Connect to database as", 
								JOptionPane.QUESTION_MESSAGE, null, null, findbugsUser);
						
					
					result = findbugsUser != null;
					}

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
	}

	private boolean skipBug(BugInstance bug) {
		return bug.getBugPattern().getCategory().equals("NOISE") || bug.isDead();
	}

	
	private static HashMap<String, Integer> issueId = new HashMap<String, Integer>();
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

		}

		
		public void newBug(BugInstance b, long timestamp) {
			try {
				BugData bug = getBugData(b.getInstanceHash());
				
				if (bug.inDatabase)
					return;
				
				PreparedStatement insertBugData =  
				        c.prepareStatement("INSERT INTO findbugs_issue (firstSeen, lastSeen, hash, bugPattern, priority, primaryClass, bugDatabaseKey) VALUES (?,?,?,?,?,?,?)",  
				        		Statement.RETURN_GENERATED_KEYS);
				Timestamp date = new Timestamp(timestamp);
				int col = 1;
				insertBugData.setTimestamp(col++, date);
				insertBugData.setTimestamp(col++, date);
				insertBugData.setString(col++, bug.instanceHash);
				insertBugData.setString(col++, b.getBugPattern().getType());
				insertBugData.setInt(col++, b.getPriority());
				insertBugData.setString(col++, b.getPrimaryClass().getClassName());
				insertBugData.setString(col++, NONE);
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
				System.out.println("Filing bug");
				PreparedStatement fileBug =  
				        c.prepareStatement("UPDATE  findbugs_issue SET bugDatabaseKey = ?, firstSeen = ?, filedBy = ? WHERE id = ?");
				Timestamp date = new Timestamp(bug.bugFiled);
				int col = 1;
				fileBug.setString(col++, bug.bugLink);
				fileBug.setTimestamp(col++, date);
				fileBug.setString(col++, bug.filedBy);
				fileBug.setInt(col++, bug.id);
				boolean result = fileBug.execute();
				fileBug.close();
				System.out.println("bug filing result = " + result);
				

			} catch (Exception e) {
				displayMessage("Problem filing bug", e);
			}
	        
        }


	}

	static interface Update {
		void execute(DatabaseSyncTask t) throws SQLException;
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
			bugCollection.getProject().getGuiCallback().showMessageDialog(msg + e.getMessage());
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
    
    
    String getBugReport(BugInstance b) {
    	StringWriter stringWriter = new StringWriter();
    	PrintWriter out = new PrintWriter(stringWriter);
    	out.println("Bug report generated from FindBugs");
    	out.println(b.getMessageWithoutPrefix());
    	out.println();
    	ClassAnnotation primaryClass = b.getPrimaryClass();
    	
    	int firstLine = Integer.MAX_VALUE;
    	int lastLine = Integer.MIN_VALUE;
    	for(BugAnnotation a : b.getAnnotations()) {
    		out.println(a);
    		if (a instanceof SourceLineAnnotation) {
    			SourceLineAnnotation s = (SourceLineAnnotation) a;
    			if (s.getClassName().equals(primaryClass.getClassName()) && s.getStartLine() > 0) {
    				firstLine = Math.min(firstLine, s.getStartLine());
    				lastLine = Math.max(lastLine, s.getEndLine());
    				
    			}
    		}
    	}
    	out.println();
    	
    	Collection<String> projects = projectMapping.getProjects(primaryClass.getClassName());
    	if (projects != null && !projects.isEmpty()) {
    		String projectList = projects.toString();
    		projectList = projectList.substring(1, projectList.length()-1);
    		out.println("Possibly part of: " + projectList);
    		out.println();
    	}
    	SourceLineAnnotation primarySource = primaryClass.getSourceLines();
    	if (primarySource.isSourceFileKnown() && firstLine <= lastLine && MainFrame.isAvailable()) {
    		try {
    		SourceFile sourceFile = MainFrame.getInstance().getSourceFinder().findSourceFile(primarySource);
    		BufferedReader in = new BufferedReader(new InputStreamReader(sourceFile.getInputStream()));
    		int lineNumber = 1;
    		out.println("\nRelevant source code:");
    		while (lineNumber <= lastLine+5) {
    			String txt = in.readLine();
    			if (txt == null) break;
    			if (lineNumber >= firstLine-5) {
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
    		URL link = getSourceLink(b);
    		if (link != null) {
    		
    			out.println(sourceFileLinkToolTip + ": " + link);
    			out.println();
    		}
    	}
    	

    	
    	out.println();
    	out.println("FindBugs issue identifier (do not modify): " + b.getInstanceHash());
    	out.close();
    	return stringWriter.toString();
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
    @CheckForNull 
    public URL getBugLink(BugInstance b) {
		try {
			BugData bd = getBugData(b);
			String bugNumber = bd.bugLink;
			if (PENDING.equals(bugNumber))
				return null;
			if (bugNumber != null && bugNumber.length() > 0  && !bugNumber.equals(NONE)) {
				String viewLinkPattern = SystemProperties.getProperty("findbugs.viewbuglink");
				if (viewLinkPattern == null)
					return null;
				String u = String.format(viewLinkPattern, bugNumber);
				return new URL(u);
			}

			String bugLinkPattern = SystemProperties.getProperty("findbugs.buglink");
			if (bugLinkPattern == null)
				return null;
			String report = getBugReport(b);
			String summary = b.getMessageWithoutPrefix() + " in " + b.getPrimaryClass().getSourceFileName();
			String u = String.format(bugLinkPattern, urlEncode(report), urlEncode(summary));
			return new URL(u);
		} catch (Exception e) {

			return null;
		}
	}
    
    @Override
    public boolean supportsCloudReports() {
		return true;
	}

    
    @Override
    public boolean supportsBugLinks() {
		return true;
	}

	@Override
    public String getCloudReport(BugInstance b) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
		StringBuilder builder = new StringBuilder();
		BugData bd = getBugData(b);
		long firstSeen = bd.firstSeen;
		if (firstSeen < Long.MAX_VALUE) {
			builder.append(String.format("First seen %s\n", format.format(new Timestamp(firstSeen))));
		}
		BugDesignation primaryDesignation = bd.getPrimaryDesignation();
		boolean canSeeCommentsByOthers = bd.canSeeCommentsByOthers();
		for(BugDesignation d : bd.designations) 
			if (d != primaryDesignation 
					&& (canSeeCommentsByOthers || findbugsUser.equals(d.getUser()))) {
				builder.append(String.format("%s @ %s: %s\n", d.getUser(), format.format(new Timestamp(d.getTimestamp())), d.getDesignationKey()));
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
  
    
    public boolean bugLinkEnabled(String label) {
    	return !label.equals("bug pending") && !label.equals("???");
    }
    @Override
    public String getBugLinkLabel(BugInstance b) {
    	BugData bd = getBugData(b);
    	String link = bd.bugLink;
    	if (link == null || link.length() == 0 || link.equals(NONE))
    		return "File bug";
    	if (link.equals(PENDING)) {
    		if (System.currentTimeMillis() - bd.bugFiled > 2*60*60*1000L)
    			return "File bug";
    		else if (findbugsUser.equals(bd.filedBy))
    			return "File again";
    		else return "bug pending";
    	}
    	try {
    		Integer.parseInt(link);
    		return "View bug";
    		
    	} catch (RuntimeException e) {
    		assert true;
    	}
    	
    	return "???";
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.cloud.Cloud#bugFiled(edu.umd.cs.findbugs.BugInstance, java.lang.Object)
     */
    public void bugFiled(BugInstance b, Object bugLink) {
    	System.out.println("requesting bug filed for " + b.getMessage());
    	queue.add(new FileBug(b));
    	updatedStatus();
    	}
	    
    
}
