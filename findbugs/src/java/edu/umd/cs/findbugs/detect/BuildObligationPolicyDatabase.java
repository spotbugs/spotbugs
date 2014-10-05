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

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.WillClose;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.ba.obl.MatchMethodEntry;
import edu.umd.cs.findbugs.ba.obl.Obligation;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseActionType;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseEntry;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabaseEntryType;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ExactStringMatcher;
import edu.umd.cs.findbugs.util.RegexStringMatcher;
import edu.umd.cs.findbugs.util.SplitCamelCaseIdentifier;
import edu.umd.cs.findbugs.util.SubtypeTypeMatcher;

/**
 * Build the ObligationPolicyDatabase used by ObligationAnalysis. We preload the
 * database with some known resources types needing to be released, and augment
 * the database with additional entries discovered through scanning referenced
 * classes for annotations.
 *
 * @author David Hovemeyer
 */
public class BuildObligationPolicyDatabase implements Detector2, NonReportingDetector {

    static class AuxilaryObligationPropertyDatabase extends MethodPropertyDatabase<String> {

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.ba.interproc.PropertyDatabase#decodeProperty(
         * java.lang.String)
         */
        @Override
        protected String decodeProperty(String propStr) throws PropertyDatabaseFormatException {
            return propStr;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.ba.interproc.PropertyDatabase#encodeProperty(
         * java.lang.Object)
         */
        @Override
        protected String encodeProperty(String property) {
            return property;
        }

    }

    public static final boolean INFER_CLOSE_METHODS = SystemProperties.getBoolean("oa.inferclose", true);

    private static final boolean DEBUG_ANNOTATIONS = SystemProperties.getBoolean("oa.debug.annotations");

    private static final boolean DUMP_DB = SystemProperties.getBoolean("oa.dumpdb");

    private final BugReporter reporter;

    private final ObligationPolicyDatabase database;

    private final ClassDescriptor willClose;

    private final ClassDescriptor willNotClose;

    private final ClassDescriptor willCloseWhenClosed;

    private final ClassDescriptor cleanupObligation;

    private final ClassDescriptor createsObligation;

    private final ClassDescriptor dischargesObligation;

    /**
     * Did we see any WillClose, WillNotClose, or WillCloseWhenClosed
     * annotations in application code?
     */
    private boolean sawAnnotationsInApplicationCode;

    public BuildObligationPolicyDatabase(BugReporter bugReporter) {
        this.reporter = bugReporter;
        final DescriptorFactory instance = DescriptorFactory.instance();
        this.willClose = instance.getClassDescriptor(WillClose.class);
        this.willNotClose = instance.getClassDescriptor(WillNotClose.class);
        this.willCloseWhenClosed = instance.getClassDescriptor(WillCloseWhenClosed.class);
        this.cleanupObligation = instance.getClassDescriptor(CleanupObligation.class);
        this.createsObligation = instance.getClassDescriptor(CreatesObligation.class);
        this.dischargesObligation = instance.getClassDescriptor(DischargesObligation.class);

        database = new ObligationPolicyDatabase();
        addBuiltInPolicies();
        URL u = DetectorFactoryCollection.getCoreResource("obligationPolicy.db");
        try {
            if (u != null) {
                AuxilaryObligationPropertyDatabase db = new AuxilaryObligationPropertyDatabase();
                db.read(u.openStream());
                for (Map.Entry<MethodDescriptor, String> e : db.entrySet()) {
                    String[] v = e.getValue().split(",");
                    Obligation obligation = database.getFactory().getObligationByName(v[2]);
                    if (obligation == null) {
                        obligation = database.getFactory().addObligation(v[2]);
                    }
                    database.addEntry(new MatchMethodEntry(e.getKey(), ObligationPolicyDatabaseActionType.valueOf(v[0]),
                            ObligationPolicyDatabaseEntryType.valueOf(v[1]), obligation));
                }

            }
        } catch (Exception e) {
            AnalysisContext.logError("Unable to read " + u, e);
        }
        scanForResourceTypes();

        Global.getAnalysisCache().eagerlyPutDatabase(ObligationPolicyDatabase.class, database);
    }

    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {

        XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescriptor);

        // Is this class an obligation type?
        Obligation thisClassObligation = database.getFactory().getObligationByType(xclass.getClassDescriptor());

        // Scan methods for uses of obligation-related annotations
        for (XMethod xmethod : xclass.getXMethods()) {
            // Is this method marked with @CreatesObligation?
            if (thisClassObligation != null) {
                if (xmethod.getAnnotation(createsObligation) != null) {
                    database.addEntry(new MatchMethodEntry(xmethod, ObligationPolicyDatabaseActionType.ADD,
                            ObligationPolicyDatabaseEntryType.STRONG, thisClassObligation));
                }

                // Is this method marked with @DischargesObligation?
                if (xmethod.getAnnotation(dischargesObligation) != null) {
                    database.addEntry(new MatchMethodEntry(xmethod, ObligationPolicyDatabaseActionType.DEL,
                            ObligationPolicyDatabaseEntryType.STRONG, thisClassObligation));
                }
            }

            addObligations(xmethod);
        }
    }

    /**
     * @param xmethod
     */
    public void addObligations(XMethod xmethod) {
        // See what obligation parameters there are
        Obligation[] paramObligationTypes = database.getFactory().getParameterObligationTypes(xmethod);

        //
        // Check for @WillCloseWhenClosed, @WillClose, @WillNotClose, or
        // other
        // indications of how obligation parameters are handled.
        //

        boolean methodHasCloseInName = false;
        if (INFER_CLOSE_METHODS) {
            SplitCamelCaseIdentifier splitter = new SplitCamelCaseIdentifier(xmethod.getName());
            methodHasCloseInName = splitter.split().contains("close");
        }

        for (int i = 0; i < xmethod.getNumParams(); i++) {
            Obligation obligationType = paramObligationTypes[i];
            if (obligationType != null) {
                if (xmethod.getParameterAnnotation(i, willCloseWhenClosed) != null) {
                    //
                    // Calling this method deletes a parameter obligation
                    // and
                    // creates a new obligation for the object returned by
                    // the method.
                    //
                    handleWillCloseWhenClosed(xmethod, obligationType);
                } else if (xmethod.getParameterAnnotation(i, willClose) != null) {
                    addParameterDeletesObligationDatabaseEntry(xmethod, obligationType,
                            ObligationPolicyDatabaseEntryType.STRONG);
                    sawAnnotationsInApplicationCode = true;
                } else if (xmethod.getParameterAnnotation(i, willNotClose) != null) {
                    // No database entry needs to be added
                    sawAnnotationsInApplicationCode = true;
                } else if (INFER_CLOSE_METHODS && methodHasCloseInName) {
                    // Method has "close" in its name.
                    // Assume that it deletes the obligation.
                    addParameterDeletesObligationDatabaseEntry(xmethod, obligationType,
                            ObligationPolicyDatabaseEntryType.STRONG);
                } else {
                    /*
                     * // Interesting case: we have a parameter which is // an
                     * Obligation type, but no annotation or other indication //
                     * what is done by the method with the obligation. // We'll
                     * create a "weak" database entry deleting the //
                     * obligation. If strict checking is performed, // weak
                     * entries are ignored.
                     */
                    if ("<init>".equals(xmethod.getName()) || xmethod.isStatic()
                            || xmethod.getName().toLowerCase().indexOf("close") >= 0
                            || xmethod.getSignature().toLowerCase().indexOf("Closeable") >= 0) {
                        addParameterDeletesObligationDatabaseEntry(xmethod, obligationType,
                                ObligationPolicyDatabaseEntryType.WEAK);
                    }
                }
            }
        }


    }

    @Override
    public void finishPass() {
        //
        // If we saw any obligation-related annotations in the application
        // code, then we enable strict checking.
        // Otherwise, we disable it.
        //
        database.setStrictChecking(sawAnnotationsInApplicationCode);

        if (DUMP_DB || ObligationPolicyDatabase.DEBUG) {
            System.out.println("======= Completed ObligationPolicyDatabase ======= ");
            System.out.println("Strict checking is " + (database.isStrictChecking() ? "ENABLED" : "disabled"));
            for (ObligationPolicyDatabaseEntry entry : database.getEntries()) {
                System.out.println("  * " + entry);
            }
            System.out.println("================================================== ");
        }
    }

    @Override
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

        Obligation javaIoInputStreamObligation = database.getFactory().getObligationByName("java.io.InputStream");
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.lang.Class")),
                new ExactStringMatcher("getResourceAsStream"),
                new ExactStringMatcher("(Ljava/lang/String;)Ljava/io/InputStream;"), false,
                ObligationPolicyDatabaseActionType.ADD, ObligationPolicyDatabaseEntryType.STRONG, javaIoInputStreamObligation));
        Obligation javaIoOutputStreamObligation = database.getFactory().getObligationByName("java.io.OutputStream");
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil
                .getObjectTypeInstance("java.util.logging.StreamHandler")), new ExactStringMatcher("setOutputStream"),
                new ExactStringMatcher("(Ljava/io/OutputStream;)V"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, javaIoOutputStreamObligation));

        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil
                .getObjectTypeInstance("java.io.FileOutputStream")), new ExactStringMatcher("getChannel"),
                new ExactStringMatcher("()Ljava/nio/channels/FileChannel;"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, javaIoOutputStreamObligation));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil
                .getObjectTypeInstance("java.io.FileInputStream")), new ExactStringMatcher("getChannel"),
                new ExactStringMatcher("()Ljava/nio/channels/FileChannel;"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, javaIoInputStreamObligation));


        // Database obligation types
        Obligation connection = database.getFactory().addObligation("java.sql.Connection");
        Obligation statement = database.getFactory().addObligation("java.sql.Statement");
        Obligation resultSet = database.getFactory().addObligation("java.sql.ResultSet");

        // Add factory method entries for database obligation types
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.DriverManager")),
                new ExactStringMatcher("getConnection"), new RegexStringMatcher("^.*\\)Ljava/sql/Connection;$"), false,
                ObligationPolicyDatabaseActionType.ADD, ObligationPolicyDatabaseEntryType.STRONG, connection));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.Connection")),
                new ExactStringMatcher("createStatement"), new RegexStringMatcher("^.*\\)Ljava/sql/Statement;$"), false,
                ObligationPolicyDatabaseActionType.ADD, ObligationPolicyDatabaseEntryType.STRONG, statement));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.Connection")),
                new ExactStringMatcher("prepareStatement"), new RegexStringMatcher("^.*\\)Ljava/sql/PreparedStatement;$"), false,
                ObligationPolicyDatabaseActionType.ADD, ObligationPolicyDatabaseEntryType.STRONG, statement));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.Statement")),
                new ExactStringMatcher("executeQuery"), new RegexStringMatcher("^.*\\)Ljava/sql/ResultSet;$"), false,
                ObligationPolicyDatabaseActionType.ADD, ObligationPolicyDatabaseEntryType.STRONG, resultSet));

        // Add close method entries for database obligation types
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.Connection")),
                new ExactStringMatcher("close"), new ExactStringMatcher("()V"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, connection));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.Statement")),
                new ExactStringMatcher("close"), new ExactStringMatcher("()V"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, statement, resultSet));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.sql.ResultSet")),
                new ExactStringMatcher("close"), new ExactStringMatcher("()V"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, resultSet));
    }

    /**
     * General method for adding entries for File
     * InputStream/OutputStream/Reader/Writer classes.
     */
    private void addFileStreamEntries(String kind) {
        Obligation obligation = database.getFactory().addObligation("java.io." + kind);
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.io.File" + kind)),
                new ExactStringMatcher("<init>"), new RegexStringMatcher(".*"), false, ObligationPolicyDatabaseActionType.ADD,
                ObligationPolicyDatabaseEntryType.STRONG, obligation));
        database.addEntry(new MatchMethodEntry(new SubtypeTypeMatcher(BCELUtil.getObjectTypeInstance("java.io." + kind)),
                new ExactStringMatcher("close"), new ExactStringMatcher("()V"), false, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, obligation));
    }

    /**
     * Add an appropriate policy database entry for parameters marked with the
     * WillClose annotation.
     *
     * @param xmethod
     *            a method
     * @param obligation
     *            the Obligation deleted by the method
     * @param entryType
     *            type of entry (STRONG or WEAK)
     */
    private void addParameterDeletesObligationDatabaseEntry(XMethod xmethod, Obligation obligation,
            ObligationPolicyDatabaseEntryType entryType) {
        ObligationPolicyDatabaseEntry entry = database.addParameterDeletesObligationDatabaseEntry(xmethod, obligation, entryType);
        if (DEBUG_ANNOTATIONS) {
            System.out.println("Added entry: " + entry);
        }
    }

    /**
     * Handle a method with a WillCloseWhenClosed parameter annotation.
     */
    private void handleWillCloseWhenClosed(XMethod xmethod, Obligation deletedObligation) {
        if (deletedObligation == null) {
            if (DEBUG_ANNOTATIONS) {
                System.out.println("Method " + xmethod.toString() + " is marked @WillCloseWhenClosed, "
                        + "but its parameter is not an obligation");
            }
            return;
        }

        // See what type of obligation is being created.
        Obligation createdObligation = null;
        if ("<init>".equals(xmethod.getName())) {
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
        database.addEntry(new MatchMethodEntry(xmethod, ObligationPolicyDatabaseActionType.DEL,
                ObligationPolicyDatabaseEntryType.STRONG, deletedObligation));
        database.addEntry(new MatchMethodEntry(xmethod, ObligationPolicyDatabaseActionType.ADD,
                ObligationPolicyDatabaseEntryType.STRONG, createdObligation));
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
            for (Iterator<Obligation> i = database.getFactory().obligationIterator(); i.hasNext();) {
                Obligation obligation = i.next();
                System.out.println("  " + obligation);
            }
        }
    }
}
