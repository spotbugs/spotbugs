/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2008, University of Maryland
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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.obl.MatchMethodEntry;
import edu.umd.cs.findbugs.ba.obl.MatchObligationParametersEntry;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseActionType;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseEntry;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.ml.SplitCamelCaseIdentifier;
import edu.umd.cs.findbugs.util.AnyTypeMatcher;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.ContainsCamelCaseWordStringMatcher;
import edu.umd.cs.findbugs.util.ExactStringMatcher;
import edu.umd.cs.findbugs.util.RegexStringMatcher;
import edu.umd.cs.findbugs.util.SubtypeTypeMatcher;
import org.apache.bcel.generic.ObjectType;

/**
 * Build the ObligationPolicyDatabase used by ObligationAnalysis.
 * We preload the database with some known resources types
 * needing to be released, and augment the database with
 * additional resource types discovered through scanning
 * referenced classes for annotations.
 * 
 * @author David Hovemeyer
 */
public class BuildObligationPolicyDatabase implements Detector2, NonReportingDetector {
	
	private static final boolean INFER_CLOSE_METHODS = SystemProperties.getBoolean("oa.inferclose", true);
	private static final boolean DEBUG_ANNOTATIONS = SystemProperties.getBoolean("oa.debug.annotations");
	
	private BugReporter reporter;
	private ObligationPolicyDatabase database;
	
	private ClassDescriptor willClose;
//	private ClassDescriptor willNotClose;
//	private ClassDescriptor willCloseWhenClosed;
	
	public BuildObligationPolicyDatabase(BugReporter bugReporter) {
		this.reporter = bugReporter;
		this.willClose = DescriptorFactory.instance().getClassDescriptor("javax/annotation/WillClose");
//		this.willNotClose = DescriptorFactory.instance().getClassDescriptor("javax/annotation/WillNotClose");
//		this.willCloseWhenClosed = DescriptorFactory.instance().getClassDescriptor("javax/annotation/WillClose");
	}

	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (database == null) {
			// Create and install the database
			
			database = new ObligationPolicyDatabase();
			addBuiltInPolicies();
			
			Global.getAnalysisCache().eagerlyPutDatabase(ObligationPolicyDatabase.class, database);
		}
		
		// Scan methods for uses of obligation-related annotations
		XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescriptor);
		for (XMethod xmethod : xclass.getXMethods()) {
			for (int i = 0; i < xmethod.getNumParams(); i++) {
				if (xmethod.getParameterAnnotation(i, willClose) != null) {
					addParameterDeletesObligationDatabaseEntry(xmethod, i);
				}
			}
		}
	}

	public void finishPass() {
		if (ObligationPolicyDatabase.DEBUG) {
			System.out.println("======= Completed ObligationPolicyDatabase ======= ");
			for (ObligationPolicyDatabaseEntry entry : database.getEntries()) {
				System.out.println("  * " + entry);
			}
			System.out.println("================================================== ");
		}
	}

	public String getDetectorClassName() {
		return this.getClass().getName();
	}

	private void addBuiltInPolicies() {
		// Add the database entries describing methods that add and delete
		// file stream/reader obligations.
		addFileStreamEntries("InputStream");
		addFileStreamEntries("OutputStream");
		addFileStreamEntries("Reader");
		addFileStreamEntries("Writer");
		
		if (INFER_CLOSE_METHODS) {
			// Experiment: assume that any method with the word "close" in
			// its camel-cased identifier taking an obligation type
			// as a parameter deletes an instance of that obligation.
			// The hope is that this will at least partially handle wrapper
			// methods for closing resouces.
			database.addEntry(new MatchObligationParametersEntry(
				new AnyTypeMatcher(),
				new ContainsCamelCaseWordStringMatcher("close"),
				ObligationPolicyDatabaseActionType.DEL));
		}
		
		// Database obligation types
		Obligation connection = database.getFactory().addObligation("java.sql.Connection");
		Obligation statement = database.getFactory().addObligation("java.sql.Statement");
		Obligation resultSet = database.getFactory().addObligation("java.sql.ResultSet");
		
		// Add factory method entries for database obligation types
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.DriverManager")),
			new ExactStringMatcher("getConnection"),
			new RegexStringMatcher("^.*\\)Ljava/sql/Connection;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			connection));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Connection")),
			new ExactStringMatcher("createStatement"),
			new RegexStringMatcher("^.*\\)Ljava/sql/Statement;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			statement));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Connection")),
			new ExactStringMatcher("prepareStatement"),
			new RegexStringMatcher("^.*\\)Ljava/sql/PreparedStatement;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			statement));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Statement")),
			new ExactStringMatcher("executeQuery"),
			new RegexStringMatcher("^.*\\)Ljava/sql/ResultSet;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			resultSet));
		
		// Add close method entries for database obligation types
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Connection")),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			connection));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Statement")),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			statement));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.ResultSet")),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			resultSet));
	}

	/**
	 * General method for adding entries for File InputStream/OutputStream/Reader/Writer classes.
	 */
	private void addFileStreamEntries(String kind) {
		Obligation obligation = database.getFactory().addObligation("java.io." + kind);
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.io.File" + kind)),
			new ExactStringMatcher("<init>"),
			new RegexStringMatcher(".*"),
			false, ObligationPolicyDatabaseActionType.ADD,
			obligation));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.io." + kind)),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL, obligation));
	}

	/**
	 * Add an appropriate policy database entry for
	 * parameters marked with the WillClose annotation.
	 * 
	 * @param xmethod a method
	 * @param i       a parameter of the method (marked with a WillClose annotation)
	 */
	private void addParameterDeletesObligationDatabaseEntry(XMethod xmethod, int i) {
		if (INFER_CLOSE_METHODS) {
			// We're automatically inferring close methods based on the
			// method name, so don't do anything if this is one of those
			// methods.
			SplitCamelCaseIdentifier splitter = new SplitCamelCaseIdentifier(xmethod.getName());
			if (splitter.split().contains("close")) {
				return;
			}
		}

		// Find the type of the i'th parameter
		String sig = xmethod.getSignature();
		SignatureParser sigParser = new SignatureParser(sig);
		String paramSig = sigParser.getParameter(i);
		if (!paramSig.startsWith("L") || !paramSig.endsWith(";")) {
			// Hmm...not a class type.
			// Probably should complain about this somehow.
			return;
		}
		ObjectType paramType = ObjectType.getInstance(ClassName.toDottedClassName(paramSig.substring(1, paramSig.length() - 1)));
		
		if (DEBUG_ANNOTATIONS) {
			System.out.println("Method " + xmethod.toString() + " param " + i + " discharges obligation");
		}
		
		try {
			// See if the parameter is of an obligation type.
			// FIXME: should complain somehow if it's not
			Obligation obligation = database.getFactory().getObligationByType(paramType);			
			if (obligation != null) {
				// Add a policy database entry noting that this method
				// will delete one instance of the obligation type.
				ObligationPolicyDatabaseEntry entry = new MatchMethodEntry(
					new SubtypeTypeMatcher(ObjectType.getInstance(xmethod.getClassDescriptor().toDottedClassName())),
					new ExactStringMatcher(xmethod.getName()),
					new ExactStringMatcher(xmethod.getSignature()),
					xmethod.isStatic(),
					ObligationPolicyDatabaseActionType.DEL,
					obligation);
				database.addEntry(entry);
				if (DEBUG_ANNOTATIONS) {
					System.out.println("Added entry: " + entry);
				}
			}
		} catch (ClassNotFoundException e) {
			reporter.reportMissingClass(e);
		}
	}
}
