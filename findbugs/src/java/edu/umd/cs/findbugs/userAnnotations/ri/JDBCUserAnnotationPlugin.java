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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.userAnnotations.Plugin;

/**
 * @author pwilliam
 */
public class JDBCUserAnnotationPlugin implements Plugin {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.userAnnotations.Plugin#getPropertyNames()
	 */
	public Set<String> getPropertyNames() {
		HashSet<String> result = new HashSet<String>();
		result.add("dbDriver");
		result.add("dbUrl");
		result.add("dbUser");
		result.add("dbPassword");
		result.add("findbugsUser");
		return result;
	}

	// dbDriver=com.mysql.jdbc.Driver,dbUrl=jdbc:mysql://localhost/findbugs,dbUser=root,dbPassword=

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.Plugin#setProperties(java.util.Map)
	 */
	public boolean setProperties(Map<String, String> properties) {
		String sqlDriver = properties.get("dbDriver");
		url = properties.get("dbUrl");
		dbUser = properties.get("dbUser");
		dbPassword = properties.get("dbPassword");
		findbugsUser = properties.get("findbugsUser");
		if (findbugsUser == null)
			findbugsUser = System.getProperty("user.name", "");
		try {
			Class.forName(sqlDriver);
			Connection c = getConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT COUNT (hash) from  findbugsIssues");
			if (rs.next()) {
				int count = rs.getInt(1);
				System.out.println("Count is " + count);
				return true;
			}

		} catch (Exception e) {
			AnalysisContext.logError("Unable to connect to database", e);
		}
		return false;
	}

	String url, dbUser, dbPassword, findbugsUser;

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, dbUser, dbPassword);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.Plugin#loadUserAnnotations(edu.umd
	 * .cs.findbugs.BugCollection)
	 */
	public void loadUserAnnotations(BugCollection bugs) {
		try {
			Connection c = getConnection();
			PreparedStatement stmt = c.prepareStatement("SELECT status, updated, who, comment from findbugsIssues where hash=?");
			for (BugInstance bug : bugs.getCollection()) {
				stmt.setString(1, bug.getInstanceHash());
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					String designationString = rs.getString(1);
					Date when = rs.getDate(2);
					String who = rs.getString(3);
					String comment = rs.getString(4);
					if (designationString.length() != 0) {
						BugDesignation bd = new BugDesignation(designationString, when.getTime(), comment, who);
						bug.setUserDesignation(bd);
					}

				} else {
					PreparedStatement stmt2 = c
					        .prepareStatement("INSERT into findbugsIssues (firstSeen, who, hash) VALUES (?,?,?)");
					stmt2.setDate(1, new java.sql.Date(System.currentTimeMillis()));
					stmt2.setString(2, findbugsUser);
					stmt2.setString(3, bug.getInstanceHash());
					stmt2.execute();
					stmt2.close();
				}
				rs.close();
				stmt.close();
			}
			c.close();
		} catch (SQLException e) {
			AnalysisContext.logError("Problems looking up user annotations", e);
		}

	}

	public void storeUserAnnotation(Connection c, BugInstance bug) throws SQLException {
		BugDesignation bd = bug.getUserDesignation();
		if (bd == null)
			return;
		PreparedStatement stmt = c
		        .prepareStatement("INSERT into  findbugsIssues (status, firstSeen, who, comment, hash) VALUES (?,?,?,?,?)");
		stmt.setString(1, bd.getDesignationKey());
		stmt.setDate(2, new java.sql.Date(bd.getTimestamp()));
		stmt.setString(3, bd.getUser());
		stmt.setString(4, bd.getAnnotationText());
		stmt.setString(5, bug.getInstanceHash());
		stmt.execute();
		stmt.close();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.Plugin#storeUserAnnotation(edu.umd
	 * .cs.findbugs.BugInstance)
	 */
	public void storeUserAnnotation(BugInstance bug) {
		try {
			Connection c = getConnection();
			storeUserAnnotation(c, bug);
			c.close();
		} catch (SQLException e) {
			AnalysisContext.logError("Problems looking up user annotations", e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.findbugs.userAnnotations.Plugin#storeUserAnnotations(edu.umd
	 * .cs.findbugs.BugCollection)
	 */
	public void storeUserAnnotations(BugCollection bugs) {
		try {
			Connection c = getConnection();
			for (BugInstance bug : bugs.getCollection()) {
				storeUserAnnotation(c, bug);
			}
			c.close();
		} catch (SQLException e) {
			AnalysisContext.logError("Problems looking up user annotations", e);
		}

	}

}
