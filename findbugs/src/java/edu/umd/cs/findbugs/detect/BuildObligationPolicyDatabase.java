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

import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseEntryType;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.obl.MatchMethodEntry;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseActionType;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseEntry;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.ml.SplitCamelCaseIdentifier;
import edu.umd.cs.findbugs.util.ExactStringMatcher;
import edu.umd.cs.findbugs.util.RegexStringMatcher;
import edu.umd.cs.findbugs.util.SubtypeTypeMatcher;
import java.util.Collection;
import java.util.Iterator;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * Build the ObligationPolicyDatabase used by ObligationAnalysis.
 * We preload the database with some known resources types
 * needing to be released, and augment the database with
 * additional entries discovered through scanning
 * referenced classes for annotations.
 * 
 * @author David Hovemeyer
 */
public class BuildObligationPolicyDatabase implements Detector2, NonReportingDetector {
	
	private static final boolean INFER_CLOSE_METHODS = SystemProperties.getBoolean("oa.inferclose", true);
	private static final boolean DEBUG_ANNOTATIONS = SystemProperties.getBoolean("oa.debug.annotations");
	private static final boolean DUMP_DB = SystemProperties.getBoolean("oa.dumpdb");
	
	private BugReporter reporter;
	private ObligationPolicyDatabase database;
	
	private ClassDescriptor willClose;
	private ClassDescriptor willNotClose;
	private ClassDescriptor willCloseWhenClosed;
	private ClassDescriptor cleanupObligation;
	private ClassDescriptor createsObligation;
	private ClassDescriptor dischargesObligation;
	
	/**
	 * Did we see any WillClose, WillNotClose, or WillCloseWhenClosed annotations
	 * in application code?
	 */
	private boolean sawAnnotationsInApplicationCode;
	
	public BuildObligationPolicyDatabase(BugReporter bugReporter) {
		this.reporter = bugReporter;
		this.willClose = DescriptorFactory.instance().getClassDescriptor("javax/annotation/WillClose");
		this.willNotClose = DescriptorFactory.instance().getClassDescriptor("javax/annotation/WillNotClose");
		this.willCloseWhenClosed = DescriptorFactory.instance().getClassDescriptor("javax/annotation/WillCloseWhenClosed");
		this.cleanupObligation = DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/CleanupObligation");
		this.createsObligation = DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/CreatesObligation");
		this.dischargesObligation = DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotations/DischargesObligation");
	}

	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (database == null) {
			// Create and install the database

			database = new ObligationPolicyDatabase();
			addBuiltInPolicies();
			scanForResourceTypes();

			Global.getAnalysisCache().eagerlyPutDatabase(ObligationPolicyDatabase.class, database);
		}

		XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescriptor);
		
		// Is this class an obligation type?
		Obligation thisClassObligation = database.getFactory().getObligationByType(xclass.getClassDescriptor());

		// Scan methods for uses of obligation-related annotations
		for (XMethod xmethod : xclass.getXMethods()) {
			// Is this method marked with @CreatesObligation?
			if (thisClassObligation != null) {
				if (xmethod.getAnnotation(createsObligation) != null) {
					database.addEntry(new MatchMethodEntry(
						xmethod,
						ObligationPolicyDatabaseActionType.ADD,
						thisClassObligation,
						ObligationPolicyDatabaseEntryType.STRONG));
				}

				// Is this method marked with @DischargesObligation?
				if (xmethod.getAnnotation(dischargesObligation) != null) {
					database.addEntry(new MatchMethodEntry(
						xmethod,
						ObligationPolicyDatabaseActionType.DEL,
						thisClassObligation,
						ObligationPolicyDatabaseEntryType.STRONG));
				}
			}

			// See what obligation parameters there are
			Obligation[] paramObligationTypes = database.getFactory().getParameterObligationTypes(xmethod);
			
			//
			// Check for @WillCloseWhenClosed, @WillClose, @WillNotClose, or other
			// indications of how obligation parameters are handled.
			//

			boolean methodHasCloseInName = false;
			if (INFER_CLOSE_METHODS) {
				SplitCamelCaseIdentifier splitter = new SplitCamelCaseIdentifier(xmethod.getName());
				methodHasCloseInName = splitter.split().contains("close");
			}

			for (int i = 0; i < xmethod.getNumParams(); i++) {
				if (xmethod.getParameterAnnotation(i, willCloseWhenClosed) != null) {
					//
					// Calling this method deletes a parameter obligation and
					// creates a new obligation for the object returned by
					// the method.
					//
					handleWillCloseWhenClosed(xmethod, paramObligationTypes[i]);
				} else if (xmethod.getParameterAnnotation(i, willClose) != null) {
					if (paramObligationTypes[i] == null) {
						// Hmm...
						if (DEBUG_ANNOTATIONS) {
							System.out.println("Method " + xmethod.toString() + " has param " + i + " annotated @WillClose, "
								+ "but its type is not an obligation type");
						}
					} else {
						addParameterDeletesObligationDatabaseEntry(
							xmethod, paramObligationTypes[i], ObligationPolicyDatabaseEntryType.STRONG);
					}
					sawAnnotationsInApplicationCode = true;
				} else if (xmethod.getParameterAnnotation(i, willNotClose) != null) {
					// No database entry needs to be added
					sawAnnotationsInApplicationCode = true;
				} else if (paramObligationTypes[i] != null) {
					if (INFER_CLOSE_METHODS && methodHasCloseInName) {
						// Method has "close" in its name.
						// Assume that it deletes the obligation.
						addParameterDeletesObligationDatabaseEntry(
							xmethod, paramObligationTypes[i], ObligationPolicyDatabaseEntryType.STRONG);
					} else {
						// not yet...
						
						/*
						// Interesting case: we have a parameter which is
						// an Obligation type, but no annotation or other indication
						// what is done by the method with the obligation.
						// We'll create a "weak" database entry deleting the
						// obligation.  If strict checking is performed,
						// weak entries are ignored.
						addParameterDeletesObligationDatabaseEntry(
							xmethod, paramObligationTypes[i], ObligationPolicyDatabaseEntryType.WEAK);
						*/
					}
				}

			}
		}
	}

	public void finishPass() {
		//
		// If we saw any obligation-related annotations in the application
		// code, then we enable strict checking.
		// Otherwise, we disable it.
		//
		database.setStrictChecking(sawAnnotationsInApplicationCode);
		
		if (DUMP_DB || ObligationPolicyDatabase.DEBUG) {
			System.out.println("======= Completed ObligationPolicyDatabase ======= ");
			System.out.println("Strict checking is " + (database.isStrictChecking() ? "ENABLED": "disabled"));
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
			connection,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Connection")),
			new ExactStringMatcher("createStatement"),
			new RegexStringMatcher("^.*\\)Ljava/sql/Statement;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			statement,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Connection")),
			new ExactStringMatcher("prepareStatement"),
			new RegexStringMatcher("^.*\\)Ljava/sql/PreparedStatement;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			statement,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Statement")),
			new ExactStringMatcher("executeQuery"),
			new RegexStringMatcher("^.*\\)Ljava/sql/ResultSet;$"),
			false,
			ObligationPolicyDatabaseActionType.ADD,
			resultSet,
			ObligationPolicyDatabaseEntryType.STRONG));
		
		// Add close method entries for database obligation types
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Connection")),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			connection,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.Statement")),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			statement,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.sql.ResultSet")),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			resultSet,
			ObligationPolicyDatabaseEntryType.STRONG));
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
			obligation,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			new SubtypeTypeMatcher(ObjectType.getInstance("java.io." + kind)),
			new ExactStringMatcher("close"),
			new ExactStringMatcher("()V"),
			false,
			ObligationPolicyDatabaseActionType.DEL,
			obligation,
			ObligationPolicyDatabaseEntryType.STRONG));
	}

	/**
	 * Add an appropriate policy database entry for
	 * parameters marked with the WillClose annotation.
	 * 
	 * @param xmethod    a method
	 * @param obligation the Obligation deleted by the method
	 * @param entryType  type of entry (STRONG or WEAK)
	 */
	private void addParameterDeletesObligationDatabaseEntry(XMethod xmethod, Obligation obligation, ObligationPolicyDatabaseEntryType entryType) {
		// Add a policy database entry noting that this method
		// will delete one instance of the obligation type.
		ObligationPolicyDatabaseEntry entry = new MatchMethodEntry(
			xmethod,
			ObligationPolicyDatabaseActionType.DEL,
			obligation,
			entryType);
		database.addEntry(entry);
		if (DEBUG_ANNOTATIONS) {
			System.out.println("Added entry: " + entry);
		}
	}

	/**
	 * Handle a method with a WillCloseWhenClosed parameter annotation.
	 */
	private void handleWillCloseWhenClosed(XMethod xmethod, Obligation deletedObligation) {
		if (deletedObligation == null)  {
			if (DEBUG_ANNOTATIONS) {
				System.out.println("Method " + xmethod.toString() + " is marked @WillCloseWhenClosed, "
					+ "but its parameter is not an obligation");
			}
			return;
		}
		
		// See what type of obligation is being created.
		Obligation createdObligation = null;
		if (xmethod.getName().equals("<init>")) {
			// Constructor - obligation type is the type of object being created
			// (or some supertype)
			createdObligation = database.getFactory().getObligationByType(xmethod.getClassDescriptor());
		} else {
			// Factory method - obligation type is the return type
			Type returnType = Type.getReturnType(xmethod.getSignature());
			if (returnType instanceof ObjectType) {
				try {
					createdObligation = database.getFactory().getObligationByType((ObjectType) returnType);
				} catch (ClassNotFoundException e) {
					reporter.reportMissingClass(e);
					return;
				}
			}

		}
		if (createdObligation == null) {
			if (DEBUG_ANNOTATIONS) {
				System.out.println("Method " + xmethod.toString() + " is marked @WillCloseWhenClosed, "
					+ "but its return type is not an obligation");
			}
			return;
		}
		
		// Add database entries:
		// - parameter obligation is deleted
		// - return value obligation is added
		database.addEntry(new MatchMethodEntry(
			xmethod,
			ObligationPolicyDatabaseActionType.DEL,
			deletedObligation,
			ObligationPolicyDatabaseEntryType.STRONG));
		database.addEntry(new MatchMethodEntry(
			xmethod,
			ObligationPolicyDatabaseActionType.ADD,
			createdObligation,
			ObligationPolicyDatabaseEntryType.STRONG));
	}

	private void scanForResourceTypes() {

		Subtypes2 subtypes2 = Global.getAnalysisCache().getDatabase(Subtypes2.class);
		Collection<XClass> knownClasses = subtypes2.getXClassCollection();
		
		for (XClass xclass : knownClasses) {
			// Is this class a resource type?
			if (xclass.getAnnotation(cleanupObligation) != null) {
				// Add it as an obligation type
				database.getFactory().addObligation(xclass.getClassDescriptor().toDottedClassName());
			}
		}
		
		if (DEBUG_ANNOTATIONS) {
			System.out.println("After scanning for resource types:");
			for (Iterator<Obligation> i = database.getFactory().obligationIterator(); i.hasNext(); ) {
				Obligation obligation = i.next();
				System.out.println("  " + obligation);
			}
		}
	}
}
