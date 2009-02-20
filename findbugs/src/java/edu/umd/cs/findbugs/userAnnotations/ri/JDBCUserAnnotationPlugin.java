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
import edu.umd.cs.findbugs.userAnnotations.UserAnnotationPlugin;

/**
 * @author pwilliam
 */
public class JDBCUserAnnotationPlugin implements UserAnnotationPlugin {

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
			rs.close();
			stmt.close();
			c.close();

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
			java.sql.Date now = new java.sql.Date(bugs.getAnalysisTimestamp());
			PreparedStatement stmt = c.prepareStatement("SELECT id, status, updated, lastSeen, who, comment FROM findbugsIssues WHERE hash=?");
			PreparedStatement stmt2 = c
	        .prepareStatement("INSERT INTO findbugsIssues (firstSeen, lastSeen, updated, who, hash, bugPattern, priority, primaryClass) VALUES (?,?,?,?,?,?,?,?)");
			PreparedStatement stmt3 = 
		        c.prepareStatement("UPDATE findbugsIssues SET lastSeen = ? WHERE id = ?");
		
			int existingIssues = 0;
			int newIssues = 0;
			for (BugInstance bug : bugs.getCollection()) {
				stmt.setString(1, bug.getInstanceHash());
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					int col = 1;
					existingIssues++;
					int id = rs.getInt(col++);
					String designationString = rs.getString(col++);
					Date when = rs.getDate(col++);
					Date lastSeen = rs.getDate(col++);
					String who = rs.getString(col++);
					String comment = rs.getString(col++);
					if (designationString.length() != 0) {
						BugDesignation bd = new BugDesignation(designationString, when.getTime(), comment, who);
						bug.setUserDesignation(bd);
					}
					if (lastSeen.compareTo(now) < 0) {
						stmt3.setInt(2, id);
						stmt3.setDate(1, now);
						stmt3.execute();
					} else {
						System.out.println("Issue lastSeen " + lastSeen + ", now is " + now);
					}
					
				} else {
					newIssues++;
					int col = 1;
					stmt2.setDate(col++, now);
					stmt2.setDate(col++, now);
					stmt2.setDate(col++, now);
					stmt2.setString(col++, findbugsUser);
					stmt2.setString(col++, bug.getInstanceHash());
					stmt2.setString(col++, bug.getBugPattern().getType());
					stmt2.setInt(col++, bug.getPriority());
					stmt2.setString(col++, bug.getPrimaryClass().getClassName());
					stmt2.execute();
				}
				rs.close();
			}
			stmt.close();
			stmt2.close();
			stmt3.close();
			c.close();
			System.out.printf("%d issues are new, %d issues are preexisting\n",
					newIssues, existingIssues);
		} catch (SQLException e) {
			AnalysisContext.logError("Problems looking up user annotations", e);
		}

	}

	public void storeUserAnnotation(Connection c, BugInstance bug) throws SQLException {
		BugDesignation bd = bug.getUserDesignation();
		if (bd == null)
			return;
		PreparedStatement stmt = c
		        .prepareStatement("INSERT into  findbugsIssues (status, updated, who, comment, hash) VALUES (?,?,?,?,?)");
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
