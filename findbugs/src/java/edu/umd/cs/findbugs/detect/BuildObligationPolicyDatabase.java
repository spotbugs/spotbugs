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
import edu.umd.cs.findbugs.ba.obl.MatchMethodEntry;
import edu.umd.cs.findbugs.ba.obl.MatchObligationParametersEntry;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationFactory;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseActionType;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.util.AnyTypeMatcher;
import edu.umd.cs.findbugs.util.ContainsCamelCaseWordStringMatcher;

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
	
	private BugReporter reporter;
	private boolean createdDatabase;
	
	public BuildObligationPolicyDatabase(BugReporter bugReporter) {
		this.reporter = bugReporter;
		this.createdDatabase = false;
	}

	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (!createdDatabase) {
			// Create and install the database
			
			ObligationPolicyDatabase database = new ObligationPolicyDatabase();
			addBuiltInPolicies(database);
			
			Global.getAnalysisCache().eagerlyPutDatabase(ObligationPolicyDatabase.class, database);
		}
		
		// TODO: scan methods for uses of obligation-related annotations
	}

	public void finishPass() {
		// nothing to do
	}

	public String getDetectorClassName() {
		return this.getClass().getName();
	}

	private void addBuiltInPolicies(ObligationPolicyDatabase database) {
		ObligationFactory factory = database.getFactory();

		// Create the Obligation types
		Obligation inputStreamObligation = factory.addObligation("java.io.InputStream");
		Obligation outputStreamObligation = factory.addObligation("java.io.OutputStream");

		// Add the database entries describing methods that add and delete
		// obligations.
		database.addEntry(new MatchMethodEntry("java.io.FileInputStream", "<init>", "(Ljava/lang/String;)V", false,
				ObligationPolicyDatabaseActionType.ADD, inputStreamObligation));
		database.addEntry(new MatchMethodEntry("java.io.FileOutputStream", "<init>", "(Ljava/lang/String;)V", false,
				ObligationPolicyDatabaseActionType.ADD, outputStreamObligation));
		database.addEntry(new MatchMethodEntry("java.io.InputStream", "close", "()V", false,
				ObligationPolicyDatabaseActionType.DEL, inputStreamObligation));
		database.addEntry(new MatchMethodEntry("java.io.OutputStream", "close", "()V", false,
				ObligationPolicyDatabaseActionType.DEL, outputStreamObligation));
		
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
}
