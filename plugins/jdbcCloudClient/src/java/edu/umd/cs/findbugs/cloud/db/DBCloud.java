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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.StartTime;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.OnlineCloud;
import edu.umd.cs.findbugs.cloud.BugFilingCommentHelper;
import edu.umd.cs.findbugs.cloud.CloudFactory;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pwilliam
 */
public class DBCloud extends AbstractCloud implements OnlineCloud {

    public static final String FINDBUGS_USER_PROPERTY = "findbugsUser";

    static final long FIRST_LIGHT = FindBugs.MINIMUM_TIMESTAMP;

    static final long ONE_DAY = 24L * 60 * 60 * 1000;

    static final String USER_NAME = "user.name";

    private SigninState signinState = SigninState.SIGNING_IN;

    class BugData {
        final String instanceHash;

        public BugData(String instanceHash) {
            this.instanceHash = instanceHash;
        }

        int id;

        boolean inDatabase;

        long firstSeen = bugCollection.getTimestamp();

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

        @CheckForNull
        BugDesignation getUserDesignation() {
            for (BugDesignation d : designations)
                if (findbugsUser.equals(d.getUser()))
                    return new BugDesignation(d);
            return null;
        }

        Collection<BugDesignation> getUniqueDesignations() {
            if (designations.isEmpty())
                return Collections.emptyList();
            HashSet<String> reviewers = new HashSet<String>();
            ArrayList<BugDesignation> result = new ArrayList<BugDesignation>(designations.size());
            for (BugDesignation d : designations)
                if (reviewers.add(d.getUser()))
                    result.add(d);
            return result;
        }

        Set<String> getReviewers() {
            HashSet<String> reviewers = new HashSet<String>();
            for (BugDesignation bd : designations)
                reviewers.add(bd.getUser());
            reviewers.remove("");
            reviewers.remove(null);
            return reviewers;
        }

        boolean isClaimed() {
            for (BugDesignation bd : getUniqueDesignations()) {
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
            switch (getMode()) {
            case SECRET:
                return false;
            case COMMUNAL:
                return true;
            case VOTING:
                return hasVoted();
            }
            throw new IllegalStateException();
        }

        public boolean hasVoted() {
            for (BugDesignation bd : designations)
                if (findbugsUser.equals(bd.getUser()))
                    return true;
            return false;
        }
    }

    int updatesSentToDatabase;

    Date lastUpdate = new Date();

    Date resync;

    Date attemptedResync;

    IPAddressLookup ipAddressLookup;

    int resyncCount;

    final Map<String, BugData> sendToDatabase = new HashMap<String, BugData>();

    final Map<Integer, BugData> fromDatabase = new HashMap<Integer, BugData>();

    final IdentityHashMap<BugDesignation, Integer> bugDesignationId = new IdentityHashMap<BugDesignation, Integer>();

    BugData getBugData(String instanceHash) {
        BugData bd = sendToDatabase.get(instanceHash);
        if (bd == null) {
            bd = new BugData(instanceHash);
            sendToDatabase.put(instanceHash, bd);
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

    @SuppressWarnings("boxing")
    void loadDatabaseInfo(String hash, int id, long firstSeen, long lastSeen) {
        BugData bd = sendToDatabase.get(hash);
        firstSeen = sanityCheckFirstSeen(firstSeen);
        lastSeen = sanityCheckLastSeen(lastSeen);
        if (bd == null)
            return;
        if (fromDatabase.containsKey(id)) {
            assert bd == fromDatabase.get(id);
            assert bd.id == id;
            assert bd.firstSeen == firstSeen;
        } else {
            bd.id = id;
            bd.firstSeen = firstSeen;
            bd.lastSeen = lastSeen;

            bd.inDatabase = true;
            fromDatabase.put(id, bd);
        }
        if (bd.firstSeen < FIRST_LIGHT)
            throw new IllegalStateException("Bug has first seen of " + new Date(bd.firstSeen));
    }

    private BugFilingCommentHelper bugFilingCommentHelper = new BugFilingCommentHelper(this);

    final long now;

    public DBCloud(CloudPlugin plugin, BugCollection bugs, Properties properties) {
        super(plugin, bugs, properties);
        sqlDriver = getJDBCProperty("dbDriver");
        url = getJDBCProperty("dbUrl");
        dbName = getJDBCProperty("dbName");
        dbUser = getJDBCProperty("dbUser");
        dbPassword = getJDBCProperty("dbPassword");
        findbugsUser = getCloudProperty(FINDBUGS_USER_PROPERTY);
        if (PROMPT_FOR_USER_NAME)
            ipAddressLookup = new IPAddressLookup();
        this.now = System.currentTimeMillis();
    }

    long sanityCheckFirstSeen(long time) {
        if (time < FIRST_LIGHT)
            return now;
        return time;
    }

    long sanityCheckLastSeen(long time) {
        if (time > now + ONE_DAY)
            return now;
        return time;
    }

    public boolean availableForInitialization() {
        String msg = String.format("%s %s %s %s", sqlDriver, dbUser, url, dbPassword);
        if (CloudFactory.DEBUG) {
            System.out.println("DB properties: " + msg);
        }
        if (sqlDriver == null || dbUser == null || url == null || dbPassword == null) {
            if (CloudFactory.DEBUG) {
                bugCollection.getProject().getGuiCallback().showMessageDialog(msg);
            }
            signinState = SigninState.SIGNIN_FAILED;
            if (THROW_EXCEPTION_IF_CANT_CONNECT)
                throw new RuntimeException("Unable to load database properties");
            return false;
        }
        return true;
    }

    final Pattern FORBIDDEN_PACKAGE_PREFIXES = Pattern.compile(properties.getProperty("findbugs.forbiddenPackagePrefixes",
            " none ").replace(',', '|'));

    final boolean PROMPT_FOR_USER_NAME = properties.getBoolean("findbugs.cloud.promptForUserName", false);

    int sessionId = -1;

    final CountDownLatch initialSyncDone = new CountDownLatch(1);

    final CountDownLatch bugsPopulated = new CountDownLatch(1);

    AtomicBoolean communicationInitiated = new AtomicBoolean(false);

    public void bugsPopulated() {
        bugsPopulated.countDown();
    }

    public void initiateCommunication() {
        bugsPopulated();
        if (communicationInitiated.compareAndSet(false, true))
            queue.add(new PopulateBugs(true));
    }

    /**
     * Returns true if communication has already been initiated (and perhaps completed).
     * 
     */
    @Override
    public boolean communicationInitiated() {
            return bugsPopulated.getCount() == 0 && communicationInitiated.get();
    }

    private static final long LAST_SEEN_UPDATE_WINDOW = TimeUnit.MILLISECONDS.convert(7 * 24 * 3600, TimeUnit.SECONDS);

    long boundDuration(long milliseconds) {
        if (milliseconds < 0)
            return 0;
        if (milliseconds > 1000 * 1000)
            return 1000 * 1000;
        return milliseconds;
    }

    static boolean invocationRecorded;
    
    static final boolean LOG_BUG_UPLOADS = SystemProperties.getBoolean("cloud.buguploads.log");

    volatile boolean sendToDatabasePopulated = false;

    class PopulateBugs implements Update {
        /**
         * True if this is the initial load from the database, false if we are
         * getting updates for an already loaded database.
         */
        final boolean performFullLoad;

        PopulateBugs(boolean performFullLoad) {
            this.performFullLoad = performFullLoad;
        }

        @SuppressWarnings("boxing")
        public void execute(DatabaseSyncTask t) throws SQLException {

            if (startShutdown)
                return;
            String commonPrefix = null;
            int updates = 0;
            if (performFullLoad) {
                for (BugInstance b : bugCollection.getCollection())
                    if (!skipBug(b)) {
                        commonPrefix = Util.commonPrefix(commonPrefix, b.getPrimaryClass().getClassName());
                        getBugData(b.getInstanceHash()).bugs.add(b);
                    }
                sendToDatabasePopulated = true;
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
                    if (issuesInDatabase > 10 * sendToDatabase.size()) {
                        if (CloudFactory.DEBUG) {
                            System.out.printf("Loading %d individual bugs from database%n", sendToDatabase.size());
                        }
                        for (String hash : sendToDatabase.keySet()) {
                            ps = c.prepareStatement("SELECT id, firstSeen, lastSeen FROM findbugs_issue WHERE hash=?");
                            ps.setString(1, hash);
                            rs = ps.executeQuery();
                            if (rs.next()) {
                                int col = 1;
                                int id = rs.getInt(col++);
                                Timestamp firstSeen = rs.getTimestamp(col++);
                                Timestamp lastSeen = rs.getTimestamp(col++);
                                loadDatabaseInfo(hash, id, firstSeen.getTime(),
                                        lastSeen.getTime());
                            }
                            rs.close();
                            ps.close();

                        }
                    } else {
                        if (CloudFactory.DEBUG) {
                            System.out.printf("Bulk loading all %d bugs from database%n", issuesInDatabase);
                        }
                        ps = c.prepareStatement("SELECT id, hash, firstSeen, lastSeen FROM findbugs_issue");
                        rs = ps.executeQuery();

                        while (rs.next()) {
                            int col = 1;
                            int id = rs.getInt(col++);
                            String hash = rs.getString(col++);
                            Timestamp firstSeen = rs.getTimestamp(col++);
                            Timestamp lastSeen = rs.getTimestamp(col++);

                            loadDatabaseInfo(hash, id, firstSeen.getTime(),
                                    lastSeen.getTime());
                        }
                        rs.close();
                        ps.close();
                    }
                }
                if (startShutdown)
                    return;

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
                    BugData data = fromDatabase.get(issueId);

                    if (data != null) {
                        BugDesignation bd = new BugDesignation(designation, when.getTime(), comment, who);
                        if (data.designations.add(bd)) {
                            bugDesignationId.put(bd, id);
                            updates++;
                            for (BugInstance bug : data.bugs) {
                                updatedIssue(bug);
                            }
                        }

                    }

                }
                rs.close();
                ps.close();
                if (startShutdown)
                    return;

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

                    BugData data = sendToDatabase.get(hash);

                    if (data != null) {
                        if (Util.nullSafeEquals(data.bugLink, bugReportId) && Util.nullSafeEquals(data.filedBy, whoFiled)
                                && data.bugFiled == whenFiled.getTime() && Util.nullSafeEquals(data.bugAssignedTo, assignedTo)
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
                        for (BugInstance bug : data.bugs) {
                            updatedIssue(bug);
                        }
                    }

                }
                rs.close();
                ps.close();
                if (startShutdown)
                    return;

                if (!invocationRecorded) {
                    long jvmStartTime = StartTime.START_TIME - StartTime.VM_START_TIME;
                    SortedBugCollection sbc = (SortedBugCollection) bugCollection;
                    long findbugsStartTime = sbc.getTimeStartedLoading() - StartTime.START_TIME;

                    URL findbugsURL = DetectorFactoryCollection.getCoreResource("findbugs.xml");
                    String loadURL = findbugsURL == null ? "" : findbugsURL.toString();

                    long initialLoadTime = sbc.getTimeFinishedLoading() - sbc.getTimeStartedLoading();
                    // long lostTime = startTime - sbc.getTimeStartedLoading();

                    long initialSyncTime = System.currentTimeMillis() - sbc.getTimeFinishedLoading();

                    String os = SystemProperties.getProperty("os.name", "");
                    String osVersion = SystemProperties.getProperty("os.version");
                    String jvmVersion = SystemProperties.getProperty("java.runtime.version");
                    if (osVersion != null)
                        os = os + " " + osVersion;
                    PreparedStatement insertSession = c
                            .prepareStatement(
                                    "INSERT INTO findbugs_invocation (who, ipAddress, entryPoint, dataSource, fbVersion, os, jvmVersion, jvmLoadTime, findbugsLoadTime, analysisLoadTime, initialSyncTime, numIssues, startTime, commonPrefix)"
                                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                    @SuppressWarnings("hiding")
                    Timestamp now = new Timestamp(startTime);
                    int col = 1;
                    insertSession.setString(col++, findbugsUser);
                    String ipAddress = PROMPT_FOR_USER_NAME ? ipAddressLookup.get() : "self-authenticated";
                    insertSession.setString(col++, ipAddress);
                    insertSession.setString(col++, limitToMaxLength(loadURL, 128));
                    insertSession.setString(col++, limitToMaxLength(sbc.getDataSource(), 128));
                    insertSession.setString(col++, Version.RELEASE);
                    insertSession.setString(col++, limitToMaxLength(os, 128));
                    insertSession.setString(col++, limitToMaxLength(jvmVersion, 64));
                    insertSession.setLong(col++, boundDuration(jvmStartTime));
                    insertSession.setLong(col++, boundDuration(findbugsStartTime));
                    insertSession.setLong(col++, boundDuration(initialLoadTime));
                    insertSession.setLong(col++, boundDuration(initialSyncTime));
                    insertSession.setInt(col++, bugCollection.getCollection().size());
                    insertSession.setTimestamp(col++, now);
                    insertSession.setString(col++, commonPrefix);
                    insertSession.executeUpdate();
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
            if (startShutdown)
                return;

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
                            if (LOG_BUG_UPLOADS) 
                                System.out.printf("NEW %tD: %s%n", new Date(getLocalFirstSeen(b)), b.getMessage());
                        } else {
                            long firstSeenLocally = getLocalFirstSeen(b);

                            if (FindBugs.validTimestamp(firstSeenLocally)
                                    && (firstSeenLocally < bd.firstSeen || !FindBugs.validTimestamp(bd.firstSeen))) {
                                if (LOG_BUG_UPLOADS) 
                                    System.out.printf("BACKDATED %tD -> %tD: %s%n", new Date(bd.firstSeen), new Date(firstSeenLocally), b.getMessage());
               
                                bd.firstSeen = firstSeenLocally;
                                storeFirstSeen(bd);
                            } else if (FindBugs.validTimestamp(stillPresentAt)
                                    && stillPresentAt > bd.lastSeen + LAST_SEEN_UPDATE_WINDOW) {
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

                long delay = 10 * 60 * 1000; // 10 minutes
                if (!scheduled) {
                    try {

                        resyncTimer.schedule(new TimerTask() {

                            @Override
                            public void run() {
                                if (attemptedResync == null || lastUpdate.after(attemptedResync) || numSkipped++ > 6) {
                                    numSkipped = 0;
                                    queue.add(new PopulateBugs(false));
                                }
                            }
                        }, delay, delay);
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

    private String getJDBCProperty(String propertyName) {
        String override = System.getProperty("findbugs.override-jdbc." + propertyName);
        if (override != null) {
            System.out.println("Using override value for " + propertyName + ":" + override);
            return override;
        }
        return properties.getProperty("findbugs.jdbc." + propertyName);
    }

    final int MAX_DB_RANK = properties.getInt("findbugs.db.maxrank", 14);

    final String url, dbUser, dbPassword, dbName;

    String findbugsUser;

    ProjectPackagePrefixes projectMapping = new ProjectPackagePrefixes();

    Map<String, String> prefixBugComponentMapping = new HashMap<String, String>();

    private final String sqlDriver;

    Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    volatile int issuesInDatabase;
    @Override
    public boolean initialize() throws IOException {
        if (CloudFactory.DEBUG)
            System.out.println("Starting DBCloud initialization");
        if (initializationIsDoomed()) {
            signinState = SigninState.SIGNIN_FAILED;
            return false;
        }

        signinState = SigninState.SIGNED_IN;
        if (CloudFactory.DEBUG) 
            System.out.println("DBCloud initialization preflight checks completed");
       
        loadBugComponents();
        Connection c = null;
        try {
            Class<?> driverClass;
            try {
                driverClass = this.getClass().getClassLoader().loadClass(sqlDriver);
            } catch (ClassNotFoundException e) {
                try {
                    driverClass = plugin.getClassLoader().loadClass(sqlDriver);
                } catch (ClassNotFoundException e2) {
                    driverClass = Class.forName(sqlDriver);
                }
            }
            if (CloudFactory.DEBUG) {
                System.out.println("Loaded " + driverClass.getName());
            }
            DriverManager.registerDriver(new DriverShim((java.sql.Driver) driverClass.newInstance()));
            c = getConnection();
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from  findbugs_issue");
            boolean result = false;
            if (rs.next()) {
                issuesInDatabase = rs.getInt(1);
                result = true;
            }
            rs.close();
            stmt.close();
            c.close();
            if (CloudFactory.DEBUG) {
                System.out.printf("%d issues in database%n", issuesInDatabase);
            }
                
            if (result) {
                runnerThread.setDaemon(true);
                runnerThread.start();
                return true;
            } else if (THROW_EXCEPTION_IF_CANT_CONNECT) {
                throw new RuntimeException("Unable to get database results");

            } else {
                if (CloudFactory.DEBUG)
                    System.out.println("Unable to connect to database");
                signinState = SigninState.SIGNIN_FAILED;
                return false;
            }

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



    private boolean initializationIsDoomed() throws IOException {
        if (!super.initialize())
            return true;
        if (!availableForInitialization())
            return true;

        findbugsUser = getUsernameLookup().getUsername();

        if (findbugsUser == null)
            return true;
        return false;
    }

    private String getBugComponent(@SlashedClassName String className) {

        int longestMatch = -1;
        String result = null;
        for (Map.Entry<String, String> e : prefixBugComponentMapping.entrySet()) {
            String key = e.getKey();
            if (className.startsWith(key) && longestMatch < key.length()) {
                longestMatch = key.length();
                result = e.getValue();
            }
        }
        return result;
    }

    private void loadBugComponents() {
        try {
            URL u = DetectorFactoryCollection.getCoreResource("bugComponents.properties");
            if (u != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
                while (true) {
                    String s = in.readLine();
                    if (s == null)
                        break;
                    if (s.trim().length() == 0)
                        continue;
                    int x = s.indexOf(' ');
                    if (x == -1) {
                        if (!prefixBugComponentMapping.containsKey(""))
                            prefixBugComponentMapping.put("", s);
                    } else {
                        String prefix = s.substring(x + 1);
                        if (!prefixBugComponentMapping.containsKey(prefix))
                            prefixBugComponentMapping.put(prefix, s.substring(0, x));
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
        if (!shutdown)
            return;
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
        queue.add(new Update() {

            public void execute(DatabaseSyncTask t) throws SQLException {
                t.storeFirstSeen(bd);

            }
        });
    }

    public void storeLastSeen(final BugData bd, final long timestamp) {
        checkForShutdown();
        queue.add(new Update() {

            public void execute(DatabaseSyncTask t) throws SQLException {
                t.storeLastSeen(bd, timestamp);

            }
        });
    }

    public BugDesignation getPrimaryDesignation(BugInstance b) {
        return getBugData(b).getPrimaryDesignation();
    }

    public void storeUserAnnotation(BugData data, BugDesignation bd) {
        checkForShutdown();
        queue.add(new StoreUserAnnotation(data, bd));
        updatedStatus();
        if (firstTimeDoing(HAS_CLASSIFIED_ISSUES)) {
            String msg = "Classification and comments have been sent to database.\n"
                    + "You'll only see this message the first time your classifcations/comments are sent\n" + "to the database.";
            if (getMode() == Mode.VOTING)
                msg += "\nOnce you've classified an issue, you can see how others have classified it.";
            msg += "\nYour classification and comments are independent from filing a bug using an external\n"
                    + "bug reporting system.";

            bugCollection.getProject().getGuiCallback().showMessageDialog(msg);
        }

    }

    private static final String HAS_SKIPPED_BUG = "has_skipped_bugs";

    private boolean skipBug(BugInstance bug) {
        BugPattern bugPattern = bug.getBugPattern();
        boolean result = bugPattern.getCategory().equals("NOISE") || bug.isDead()
                || bugPattern.getType().equals("UNKNOWN")
                || BugRanker.findRank(bug) > MAX_DB_RANK;
        if (result && firstTimeDoing(HAS_SKIPPED_BUG)) {
            bugCollection
                    .getProject()
                    .getGuiCallback()
                    .showMessageDialog(
                            "To limit database load, some issues are not persisted to database.\n"
                                    + "For example, issues with rank greater than " + MAX_DB_RANK
                                    + " are not stored in the db.\n"
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

        @SuppressWarnings("boxing")
        public void newEvaluation(BugData data, BugDesignation bd) {
            if (!data.inDatabase)
                return;
            try {
                data.designations.add(bd);
                if (bd.getUser() == null)
                    bd.setUser(findbugsUser);
                if (bd.getAnnotationText() == null)
                    bd.setAnnotationText("");

                PreparedStatement insertEvaluation = c
                        .prepareStatement(
                                "INSERT INTO findbugs_evaluation (issueId, who, invocationId, designation, comment, time) VALUES (?,?,?,?,?,?)",
                                Statement.RETURN_GENERATED_KEYS);
                Timestamp date = new Timestamp(bd.getTimestamp());
                int col = 1;
                insertEvaluation.setInt(col++, data.id);
                insertEvaluation.setString(col++, bd.getUser());
                insertEvaluation.setInt(col++, sessionId);
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

                PreparedStatement insertBugData = c
                        .prepareStatement(
                                "INSERT INTO findbugs_issue (firstSeen, lastSeen, hash, bugPattern, priority, primaryClass) VALUES (?,?,?,?,?,?)",
                                Statement.RETURN_GENERATED_KEYS);
                int col = 1;
                insertBugData.setTimestamp(col++, new Timestamp(bug.firstSeen));
                insertBugData.setTimestamp(col++, new Timestamp(bug.lastSeen));
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
            if (bug.firstSeen <= FIRST_LIGHT)
                return;
            try {

                PreparedStatement insertBugData = c.prepareStatement("UPDATE  findbugs_issue SET firstSeen = ? WHERE id = ?");
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
            if (bug.lastSeen >= now + ONE_DAY)
                return;

            try {

                PreparedStatement insertBugData = c.prepareStatement("UPDATE  findbugs_issue SET lastSeen = ? WHERE id = ?");
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
            query = c.prepareStatement("SELECT  id, bugReportId, whoFiled, whenFiled FROM findbugs_bugreport WHERE hash=?");
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
            Util.closeSilently(rs);
            Util.closeSilently(query);
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
        public void execute(DatabaseSyncTask t) {
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

            long timestamp = getLocalFirstSeen(bug);

            if (timestamp < FIRST_LIGHT)
                timestamp = analysisTime;
            timestamp = sanityCheckFirstSeen(sanityCheckLastSeen(timestamp));
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
            bugCollection.getProject().getGuiCallback()
                    .showMessageDialog(String.format("%s - %s%n%s", msg, e.getMessage(), stackTraceWriter.toString()));
        } else {
            System.err.println(msg);
            e.printStackTrace(System.err);
        }
    }

    public SigninState getSigninState() {
        return signinState;
    }

    public void setSaveSignInInformation(boolean save) {
        // not saved anyway
    }

    public boolean isSavingSignInInformationEnabled() {
        return false;
    }

    public void signIn() {
        if (getSigninState() != SigninState.SIGNED_IN)
            throw new UnsupportedOperationException("Unable to sign in");
    }

    public void signOut() {
        throw new UnsupportedOperationException();
    }

    public String getUser() {
        return findbugsUser;
    }

    @Override
    public long getFirstSeen(BugInstance b) {
        return getBugData(b).firstSeen;
    }

    @Override
    public void addDateSeen(BugInstance b, long when) {
        if (when <= 0) return;
        when = sanityCheckFirstSeen(when);
        BugData bd = getBugData(b);
        if (bd.firstSeen < when)
            return;
        bd.firstSeen = when;
        storeFirstSeen(bd);
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
    public double getClassificationScore(BugInstance b) {
        BugData bd = getBugData(b);
        if (bd == null)
            return 0;
        Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
        double total = 0;
        int count = 0;
        for (BugDesignation d : uniqueDesignations) {
            UserDesignation designation = UserDesignation.valueOf(d.getDesignationKey());
            if (designation.nonVoting())
                continue;
            total += designation.score();
            count++;
        }
        return total / count;
    }

    @Override
    public double getPortionObsoleteClassifications(BugInstance b) {
        BugData bd = getBugData(b);
        if (bd == null)
            return 0;

        int count = 0;
        Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
        for (BugDesignation d : uniqueDesignations)
            if (UserDesignation.valueOf(d.getDesignationKey()) == UserDesignation.OBSOLETE_CODE)
                count++;
        return ((double) count) / uniqueDesignations.size();
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
        for (BugDesignation d : uniqueDesignations) {
            UserDesignation designation = UserDesignation.valueOf(d.getDesignationKey());
            if (designation.nonVoting())
                continue;
            int score = designation.score();
            total += score;
            totalSquares += score * score;
            count++;
        }

        double average = total / count;
        return totalSquares / count - average * average;
    }

    @Override
    public double getClassificationDisagreement(BugInstance b) {
        BugData bd = getBugData(b);
        if (bd == null)
            return 0;
        Collection<BugDesignation> uniqueDesignations = bd.getUniqueDesignations();
        int shouldFix = 0;
        int dontFix = 0;
        for (BugDesignation d : uniqueDesignations) {
            UserDesignation designation = UserDesignation.valueOf(d.getDesignationKey());
            if (designation.nonVoting())
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

    final String BUG_LINK_FORMAT = properties.getProperty("findbugs.filebug.link");

    final String BUG_LOGIN_LINK = properties.getProperty("findbugs.filebug.login");

    final String BUG_LOGIN_MSG = properties.getProperty("findbugs.filebug.loginMsg");

    final String COMPONENT_FOR_BAD_ANALYSIS = properties.getProperty("findbugs.filebug.badAnalysisComponent");

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
                int answer = getBugCollection()
                        .getProject()
                        .getGuiCallback()
                        .showConfirmDialog(
                                "Sorry, but since the time we last received updates from the database,\n"
                                        + "someone else already filed a bug report. Would you like to view the bug report?",
                                "Someone else already filed a bug report", "Yes", "No");
                if (answer == 0)
                    return null;
                return getBugViewLink(bugReportId);
            }
            rs.close();
            ps.close();

            if (pendingFiledAt != null) {
                bd.bugLink = PENDING;
                bd.bugFiled = pendingFiledAt.getTime();
                getBugCollection()
                        .getProject()
                        .getGuiCallback()
                        .showMessageDialog(
                                "Sorry, but since the time we last received updates from the database,\n"
                                        + "someone else already has started a bug report for this issue. ");
                return null;

            }

            // OK, not in database
            if (status == BugFilingStatus.FILE_BUG) {

                URL u = getBugFilingLink(b);
                if (u != null && firstTimeDoing(HAS_FILED_BUGS)) {
                    String bugFilingNote = String.format(properties.getProperty("findbugs.filebug.note", ""));
                    int response = bugCollection
                            .getProject()
                            .getGuiCallback()
                            .showConfirmDialog(
                                    "This looks like the first time you've filed a bug from this machine. Please:\n"
                                            + " * Please check the component the issue is assigned to; we sometimes get it wrong.\n"
                                            + " * Try to figure out the right person to assign it to.\n"
                                            + " * Provide the information needed to understand the issue.\n"
                                            + bugFilingNote
                                            + "Note that classifying an issue is distinct from (and lighter weight than) filing a bug.",
                                    "Do you want to file a bug report", "Yes", "No");
                    if (response != 0)
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
    private @CheckForNull
    URL getBugViewLink(String bugNumber) {
        String viewLinkPattern = properties.getProperty("findbugs.viewbug.link");
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

    private void displaySupplementalBugReport(String supplemental) {
        supplemental = "[Can't squeeze this information into the URL used to prepopulate the bug entry\n"
                + " please cut and paste into the bug report as appropriate]\n\n" + supplemental;
        bugCollection.getProject().getGuiCallback()
                .displayNonmodelMessage("Cut and paste as needed into bug entry", supplemental);
    }

    private URL getBugFilingLink(BugInstance b) throws MalformedURLException {
        if (BUG_LINK_FORMAT == null)
            return null;

        int maxURLLength = MAX_URL_LENGTH;
        if (firstBugRequest) {
            if (BUG_LOGIN_LINK != null && BUG_LOGIN_MSG != null) {
                URL u = new URL(String.format(BUG_LOGIN_LINK));
                if (!bugCollection.getProject().getGuiCallback().showDocument(u))
                    return null;
                int r = bugCollection.getProject().getGuiCallback()
                        .showConfirmDialog(BUG_LOGIN_MSG, "Logging into bug tracker...", "OK", "Cancel");
                if (r != 0)
                    return null;
            } else
                maxURLLength = maxURLLength * 2 / 3;
        }
        firstBugRequest = false;

        String component;
        if (getUserDesignation(b) == UserDesignation.BAD_ANALYSIS && COMPONENT_FOR_BAD_ANALYSIS != null)
            component = COMPONENT_FOR_BAD_ANALYSIS;
        else
            component = getBugComponent(b.getPrimaryClass().getClassName().replace('.', '/'));
        String summary = bugFilingCommentHelper.getBugReportSummary(b);
        String report = bugFilingCommentHelper.getBugReportText(b);
        String u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
        if (u.length() > maxURLLength) {
            String head = bugFilingCommentHelper.getBugReportHead(b);
            String sourceCode = bugFilingCommentHelper.getBugReportSourceCode(b);
            String tail = bugFilingCommentHelper.getBugReportTail(b);
            report = head + sourceCode + tail;
            String lineTerminatedUserEvaluation = bugFilingCommentHelper.getLineTerminatedUserEvaluation(b);
            String explanation = bugFilingCommentHelper.getBugPatternExplanation(b);
            String supplemental = lineTerminatedUserEvaluation + explanation;
            u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
            if (u.length() > maxURLLength) {
                report = head + tail;
                supplemental = sourceCode + lineTerminatedUserEvaluation + explanation;
                u = String.format(BUG_LINK_FORMAT, component, urlEncode(summary), urlEncode(report));
                if (u.length() > maxURLLength) {
                    // Last resort: Just make the link work with a minimal
                    // report and by shortening the summary
                    supplemental = head + sourceCode + lineTerminatedUserEvaluation + explanation;
                    report = tail;
                    // (assuming BUG_URL_FORMAT + component + report tail is
                    // always < maxUrlLength)
                    String urlEncodedSummary = urlEncode(summary);
                    String urlEncodedReport = urlEncode(report);
                    String urlEncodedComponent = urlEncode(component);
                    int maxSummaryLength = maxURLLength - BUG_LINK_FORMAT.length() + 6 /*
                                                                                        * 3
                                                                                        * %
                                                                                        * s
                                                                                        * placeholders
                                                                                        */
                            - urlEncodedReport.length() - urlEncodedComponent.length();
                    if (urlEncodedSummary.length() > maxSummaryLength) {
                        urlEncodedSummary = urlEncodedSummary.substring(0, maxSummaryLength - 1);
                        // Chop of any incomplete trailing percent encoded part
                        if ("%".equals(urlEncodedSummary.substring(urlEncodedSummary.length() - 1))) {
                            urlEncodedSummary = urlEncodedSummary.substring(0, urlEncodedSummary.length() - 2);
                        } else if ("%".equals(urlEncodedSummary.substring(urlEncodedSummary.length() - 2,
                                urlEncodedSummary.length() - 1))) {
                            urlEncodedSummary = urlEncodedSummary.substring(0, urlEncodedSummary.length() - 3);
                        }
                    }
                    u = String.format(BUG_LINK_FORMAT, urlEncodedComponent, urlEncodedSummary, urlEncodedReport);
                }
            }
            displaySupplementalBugReport(supplemental);
        }
        return new URL(u);
    }

    @Override
    public boolean supportsCloudReports() {
        return true;
    }

    @Override
    public boolean supportsBugLinks() {
        return BUG_LINK_FORMAT != null;
    }

    public void storeUserAnnotation(BugInstance bugInstance) {

        storeUserAnnotation(getBugData(bugInstance), bugInstance.getNonnullUserDesignation());
        updatedIssue(bugInstance);

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

        return BugFilingStatus.NA;
    }

    @Override
    public String getBugStatus(BugInstance b) {
        String status = getBugData(b).bugStatus;
        if (status != null)
            return status;
        return "Unknown";
    }

    private boolean pendingStatusHasExpired(long whenFiled) {
        return System.currentTimeMillis() - whenFiled > 60 * 60 * 1000L;
    }

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
                return errorMsg + "; " + getStatusMsg0();
        }
        return getStatusMsg0();
    }

    @SuppressWarnings("boxing")
    public String getStatusMsg0() {
        if (!communicationInitiated.get() || !sendToDatabasePopulated) {
            return String.format("communications with database not yet initialized");
        }
        
        SimpleDateFormat format = new SimpleDateFormat("h:mm a");
        int numToSync = queue.size();
        if (numToSync > 0)
            return String.format("%d remain to be synchronized", numToSync);
        else if (resync != null && resync.after(lastUpdate))
            return String.format("%d updates received from db at %s", resyncCount, format.format(resync));
        else if (updatesSentToDatabase == 0) {
            int skipped = bugCollection.getCollection().size() - sendToDatabase.size();
            if (skipped == 0)
                return String.format("%d issues synchronized with database", fromDatabase.size());
            else
                return String.format("%d issues synchronized with database, %d low rank issues not synchronized", 
                            fromDatabase.size(), skipped);
        } else
            return String.format("%d classifications/bug filings sent to db, last updated at %s", updatesSentToDatabase,
                    format.format(lastUpdate));

    }

    @Override
    public boolean getIWillFix(BugInstance b) {
        if (super.getIWillFix(b))
            return true;
        BugData bd = getBugData(b);
        return bd != null && findbugsUser.equals(bd.bugAssignedTo);
    }

    @Override
    public boolean getBugIsUnassigned(BugInstance b) {
        BugData bd = getBugData(b);
        return bd != null && bd.inDatabase && getBugLinkStatus(b) == BugFilingStatus.VIEW_BUG
                && ("NEW".equals(bd.bugStatus) || bd.bugAssignedTo == null || bd.bugAssignedTo.length() == 0);
    }

    @Override
    public boolean getWillNotBeFixed(BugInstance b) {
        BugData bd = getBugData(b);
        return bd != null
                && bd.inDatabase
                && getBugLinkStatus(b) == BugFilingStatus.VIEW_BUG
                && ("WILL_NOT_FIX".equals(bd.bugStatus) || "OBSOLETE".equals(bd.bugStatus)
                        || "WORKS_AS_INTENDED".equals(bd.bugStatus) || "NOT_FEASIBLE".equals(bd.bugStatus));

    }

    @Override
    public boolean supportsCloudSummaries() {
        return true;
    }

    @Override
    public boolean canStoreUserAnnotation(BugInstance bugInstance) {
        return !skipBug(bugInstance);
    }

    @Override
    public @CheckForNull
    String claimedBy(BugInstance b) {
        BugData bd = getBugData(b);
        if (bd == null)
            return null;
        for (BugDesignation designation : bd.getUniqueDesignations()) {
            if ("I_WILL_FIX".equals(designation.getDesignationKey()))
                return designation.getUser();
        }
        return null;

    }

    @Override
    protected Iterable<BugDesignation> getLatestDesignationFromEachUser(BugInstance bd) {
        BugData bugData = sendToDatabase.get(bd.getInstanceHash());
        if (bugData == null)
            return Collections.emptyList();
        return bugData.getUniqueDesignations();
    }

    @Override
    public BugInstance getBugByHash(String hash) {
        Collection<BugInstance> bugs = sendToDatabase.get(hash).bugs;
        return bugs.isEmpty() ? null : bugs.iterator().next();
    }

    public Collection<String> getProjects(String className) {
        return projectMapping.getProjects(className);
    }

    public boolean isInCloud(BugInstance b) {
        if (b == null)
            throw new NullPointerException("null bug");
        String instanceHash = b.getInstanceHash();

        BugData bugData = sendToDatabase.get(instanceHash);
        return bugData != null && bugData.inDatabase;
    }

    @Override
    public String notInCloudMsg(BugInstance b) {
        if (isInCloud(b)) {
            assert false;
            return "Is in cloud";
        }
        int rank = BugRanker.findRank(b);
        if (rank > MAX_DB_RANK)
            return
            String.format("This issue is rank %d, only issues up to rank %d are recorded in the cloud",
                    rank, MAX_DB_RANK);
        return "Issue is not recorded in cloud";
    }

    public boolean isOnlineCloud() {
        return true;
    }

    @Override
    public URL fileBug(BugInstance bug) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.cloud.Cloud#waitUntilNewIssuesUploaded()
     */
    public void waitUntilNewIssuesUploaded() {
        try {
            initiateCommunication();
            initialSyncDone.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit)
            throws InterruptedException {
        initiateCommunication();
        return initialSyncDone.await(timeout, unit);
    }

    public void waitUntilIssueDataDownloaded() {
        initiateCommunication();
        try {
            initialSyncDone.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit)
            throws InterruptedException {
        initiateCommunication();
        return initialSyncDone.await(timeout, unit);
    }

}
