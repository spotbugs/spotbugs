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

package edu.umd.cs.findbugs.cloud.username;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.util.LaunchBrowser;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class WebCloudNameLookup implements NameLookup {

    private static final String APPENGINE_HOST_PROPERTY_NAME = "webcloud.host";
    private static final String KEY_SAVE_SESSION_INFO = "save_session_info";
    static final String KEY_APPENGINECLOUD_SESSION_ID = "webcloud_session_id";
    /** if "true", prevents session info from being saved between launches. */
    private static final String SYSPROP_NEVER_SAVE_SESSION = "webcloud.never_save_session";

    private static final Logger LOGGER = Logger.getLogger(WebCloudNameLookup.class.getName());


    private static final int USER_SIGNIN_TIMEOUT_SECS = 60;


    private Long sessionId;
    private String username;
    private String url;

    @Override
    public boolean signIn(CloudPlugin plugin, BugCollection bugCollection) throws IOException {
        loadProperties(plugin);

        if (softSignin()) {
            return true;
        }

        if (sessionId == null) {
            sessionId = loadOrCreateSessionId();
        }

        LOGGER.info("Opening browser for session " + sessionId);
        URL u = new URL(url + "/browser-auth/" + sessionId);
        LaunchBrowser.showDocument(u);

        // wait 1 minute for the user to sign in
        for (int i = 0; i < USER_SIGNIN_TIMEOUT_SECS; i++) {
            if (checkAuthorized(getAuthCheckUrl(sessionId))) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        LOGGER.info("Sign-in timed out for " + sessionId);
        throw new IOException("Sign-in timed out");
    }

    public void loadProperties(CloudPlugin plugin) {
        PropertyBundle pluginProps = plugin.getProperties();
        url = pluginProps.getProperty(APPENGINE_HOST_PROPERTY_NAME);
        if (url == null) {
            throw new IllegalStateException("Host not specified for " + plugin.getId());
        }
    }

    /**
     * If the user can be authenticated due to an existing session id, do so
     *
     * @return true if we could authenticate the user
     * @throws IOException
     */
    public boolean softSignin() throws IOException {
        if (url == null) {
            throw new IllegalStateException("Null host");
        }

        checkResolveHost();

        if (sessionId != null) {
            if (checkAuthorized(getAuthCheckUrl(sessionId))) {
                LOGGER.fine("Skipping soft init; session ID already exists - " + sessionId);
                return true;
            } else {
                sessionId = null;
            }
        }
        // check the previously used session ID
        long id = loadSessionId();
        if (id == 0) {
            return false;
        }
        boolean authorized = checkAuthorized(getAuthCheckUrl(id));
        if (authorized) {
            LOGGER.info("Authorized with session ID: " + id);
            this.sessionId = id;
        }

        return authorized;
    }

    public void checkResolveHost() throws UnknownHostException {
        try {
            String host = new URL(url).getHost();
            InetAddress.getByName(host);
        } catch (MalformedURLException e) {
            assert true;
            /* this will come out later */
        }
    }

    private URL getAuthCheckUrl(long sessionId) throws MalformedURLException {
        return new URL(url + "/check-auth/" + sessionId);
    }

    public static void setSaveSessionInformation(boolean save) {
        Preferences prefs = Preferences.userNodeForPackage(WebCloudNameLookup.class);
        prefs.putBoolean(KEY_SAVE_SESSION_INFO, save);
        if (!save) {
            clearSavedSessionInformation();
        }
    }

    public static boolean isSavingSessionInfoEnabled() {
        return !Boolean.getBoolean(SYSPROP_NEVER_SAVE_SESSION)
                && Preferences.userNodeForPackage(WebCloudNameLookup.class).getBoolean(KEY_SAVE_SESSION_INFO, true);
    }

    public static void clearSavedSessionInformation() {
        Preferences prefs = Preferences.userNodeForPackage(WebCloudNameLookup.class);
        prefs.remove(KEY_APPENGINECLOUD_SESSION_ID);
    }

    public static void saveSessionInformation(long sessionId) {
        assert sessionId != 0;
        Preferences.userNodeForPackage(WebCloudNameLookup.class).putLong(KEY_APPENGINECLOUD_SESSION_ID, sessionId);
    }

    public Long getSessionId() {
        return sessionId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getHost() {
        return url;
    }

    // ======================= end of public methods =======================

    private static SecureRandom secureRandom = new SecureRandom();

    private long loadOrCreateSessionId() {
        long id = loadSessionId();
        if (id != 0) {
            LOGGER.info("Using saved session ID: " + id);
            return id;
        }
        while (id == 0) {
            id = secureRandom.nextLong();
        }

        if (isSavingSessionInfoEnabled()) {
            saveSessionInformation(id);
        }

        return id;
    }

    /**
     * @return session id if already exists, or 0 if it doesn't
     */
    private long loadSessionId() {
        Preferences prefs = Preferences.userNodeForPackage(WebCloudNameLookup.class);
        return prefs.getLong(KEY_APPENGINECLOUD_SESSION_ID, 0);
    }

    private boolean checkAuthorized(URL response) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) response.openConnection();

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            InputStream in = connection.getInputStream();
            BufferedReader reader = UTF8.bufferedReader(in);
            try {
                String status = reader.readLine();
                sessionId = Long.parseLong(reader.readLine());
                username = reader.readLine();
                Util.closeSilently(reader);
                if ("OK".equals(status)) {
                    LOGGER.info("Authorized session " + sessionId);
                    return true;
                }
            } finally {
                reader.close();
            }

        }
        connection.disconnect();
        return false;
    }
}
