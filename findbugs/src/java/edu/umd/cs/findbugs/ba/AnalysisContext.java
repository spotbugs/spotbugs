/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2006 University of Maryland
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

package edu.umd.cs.findbugs.ba;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import net.jcip.annotations.NotThreadSafe;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisCacheToRepositoryAdapter;
import edu.umd.cs.findbugs.AnalysisLocal;
import edu.umd.cs.findbugs.BugInstance.NoSuchBugPattern;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SuppressionMatcher;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisFeatures.AnalysisFeature;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.ba.jsr305.DirectlyRelevantTypeQualifiersDatabase;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.ReturnValueNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.TypeQualifierNullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldOrMethodDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.detect.UnreadFields;
import edu.umd.cs.findbugs.detect.UnreadFieldsData;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * A context for analysis of a complete project. This serves as the repository
 * for whole-program information and data structures.
 *
 * <p>
 * <b>NOTE</b>: this class is slated to become obsolete. New code should use the
 * IAnalysisCache object returned by Global.getAnalysisCache() to access all
 * analysis information (global databases, class and method analyses, etc.)
 * </p>
 *
 * @author David Hovemeyer
 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache
 * @see edu.umd.cs.findbugs.classfile.Global
 */
@NotThreadSafe
public class AnalysisContext {
    public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.analysiscontext.debug");

    public static final boolean IGNORE_BUILTIN_MODELS = SystemProperties.getBoolean("findbugs.ignoreBuiltinModels");

    public static final String DEFAULT_NONNULL_PARAM_DATABASE_FILENAME = "nonnullParam.db";

    public static final String DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME = "checkForNullParam.db";

    public static final String DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE = "nullReturn.db";

    public static final String UNCONDITIONAL_DEREF_DB_FILENAME = "unconditionalDeref.db";

    public static final String NONNULL_RETURN_DB_FILENAME = "nonnullReturn.db";

    public static final String UNCONDITIONAL_DEREF_DB_RESOURCE = "jdkBaseUnconditionalDeref.db";

    public static final String NONNULL_RETURN_DB_RESOURCE = "jdkBaseNonnullReturn.db";

    public static final String DEFAULT_NULL_RETURN_VALUE_DB_FILENAME = "mayReturnNull.db";

    private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext = new InheritableThreadLocal<AnalysisContext>() {
        @Override
        public AnalysisContext initialValue() {
            // throw new
            // IllegalStateException("currentAnalysisContext should be set by AnalysisContext.setCurrentAnalysisContext");
            return null;
        }
    };

    private static AnalysisLocal<XFactory> currentXFactory = new AnalysisLocal<XFactory>() {
        @Override
        public XFactory initialValue() {
            throw new IllegalStateException("currentXFactory should be set by AnalysisContext.setCurrentAnalysisContext");
        }
    };

    /**
     * save the original SyntheticRepository so we may obtain JavaClass objects
     * which we can reuse. (A URLClassPathRepository gets closed after
     * analysis.)
     */
    private static final org.apache.bcel.util.Repository originalRepository = Repository.getRepository(); // BCEL
    // SyntheticRepository

    /**
     * Default maximum number of ClassContext objects to cache. FIXME: need to
     * evaluate this parameter. Need to keep stats about accesses.
     */
    //    private static final int DEFAULT_CACHE_SIZE = 3;

    // Instance fields
    private BitSet boolPropertySet;

    private String databaseInputDir;

    private String databaseOutputDir;

    boolean missingClassWarningsSuppressed;

    private ClassSummary classSummary;

    private ClassDescriptor classBeingAnalyzed;

    private FieldSummary fieldSummary;

    private UnreadFields unreadFields;

    private TypeQualifierNullnessAnnotationDatabase tqNullnessDatabase;

    private final HashSet<MethodDescriptor> skippedDueToInvokeDynamic;

    private final Project project;

    private final EqualsKindSummary equalsKindSummary;

    private final UnreadFieldsData unreadFieldsData;

    private final SuppressionMatcher suppressionMatcher;

    protected final RepositoryLookupFailureCallback lookupFailureCallback;

    private final Map<MethodInfo, MethodInfo> bridgeTo;

    private final Map<MethodInfo, MethodInfo> bridgeFrom;


    public AnalysisContext(@Nonnull Project project) {
        requireNonNull(project);
        this.project = project;
        this.boolPropertySet = new BitSet();
        this.lookupFailureCallback = new DelegatingRepositoryLookupFailureCallback();
        skippedDueToInvokeDynamic = new HashSet<>();
        equalsKindSummary = new EqualsKindSummary();
        unreadFieldsData = new UnreadFieldsData();
        suppressionMatcher = new SuppressionMatcher();
        bridgeTo = new IdentityHashMap<>();
        bridgeFrom = new IdentityHashMap<>();
    }

    private void clear() {
        boolPropertySet = null;
        databaseInputDir = null;
        databaseOutputDir = null;
    }

    /**
     * Get the AnalysisContext associated with this thread
     */
    static public AnalysisContext currentAnalysisContext() {
        return currentAnalysisContext.get();
    }

    static public XFactory currentXFactory() {
        return currentXFactory.get();
    }

    public ClassDescriptor getClassBeingAnalyzed() {
        return classBeingAnalyzed;
    }

    public void setClassBeingAnalyzed(@Nonnull ClassDescriptor classBeingAnalyzed) {
        this.classBeingAnalyzed = classBeingAnalyzed;
    }

    public void clearClassBeingAnalyzed() {
        this.classBeingAnalyzed = null;
    }

    public ClassSummary getClassSummary() {
        if (classSummary == null) {
            throw new IllegalStateException("ClassSummary not set");
        }
        return classSummary;
    }

    public void setClassSummary(@Nonnull ClassSummary classSummary) {
        if (this.classSummary != null) {
            throw new IllegalStateException("ClassSummary already set");
        }
        this.classSummary = classSummary;
    }

    public EqualsKindSummary getEqualsKindSummary() {
        return equalsKindSummary;
    }

    public FieldSummary getFieldSummary() {
        if (fieldSummary == null) {
            AnalysisContext.logError("Field Summary not set", new IllegalStateException());
            fieldSummary = new FieldSummary();
        }
        return fieldSummary;
    }

    public void setFieldSummary(@Nonnull FieldSummary fieldSummary) {
        if (this.fieldSummary != null) {
            AnalysisContext.logError("Field Summary already set", new IllegalStateException());
        }
        this.fieldSummary = fieldSummary;
    }

    public @Nonnull UnreadFieldsData getUnreadFieldsData() {
        return unreadFieldsData;
    }

    public @Nonnull UnreadFields getUnreadFields() {
        if (!unreadFieldsAvailable()) {
            throw new IllegalStateException();
        }
        return unreadFields;
    }

    public boolean unreadFieldsAvailable() {
        return unreadFields != null;
    }

    public void setUnreadFields(@Nonnull UnreadFields unreadFields) {
        if (this.unreadFields != null) {
            throw new IllegalStateException("UnreadFields detector already set");
        }
        this.unreadFields = unreadFields;
    }

    private static boolean skipReportingMissingClass(@CheckForNull @DottedClassName String missing) {
        return missing == null || missing.length() == 0 || missing.charAt(0) == '[' || missing.endsWith("package-info");
    }

    private static @CheckForNull
    RepositoryLookupFailureCallback getCurrentLookupFailureCallback() {
        AnalysisContext currentAnalysisContext2 = currentAnalysisContext();
        if (currentAnalysisContext2 == null) {
            return null;
        }
        if (currentAnalysisContext2.missingClassWarningsSuppressed) {
            return null;
        }
        return currentAnalysisContext2.getLookupFailureCallback();
    }

    /**
     * file a ClassNotFoundException with the lookupFailureCallback
     *
     * @see #getLookupFailureCallback()
     */
    static public void reportMissingClass(ClassNotFoundException e) {
        requireNonNull(e, "argument is null");
        String missing = AbstractBugReporter.getMissingClassName(e);
        if (skipReportingMissingClass(missing)) {
            return;
        }
        if (!analyzingApplicationClass()) {
            return;
        }

        RepositoryLookupFailureCallback lookupFailureCallback = getCurrentLookupFailureCallback();
        if (lookupFailureCallback != null) {
            lookupFailureCallback.reportMissingClass(e);
        }
    }

    static public void reportMissingClass(edu.umd.cs.findbugs.ba.MissingClassException e) {
        requireNonNull(e, "argument is null");
        reportMissingClass(e.getClassDescriptor());
    }

    static public boolean analyzingApplicationClass() {
        AnalysisContext context = AnalysisContext.currentAnalysisContext();
        if (context == null) {
            return false;
        }
        ClassDescriptor clazz = context.getClassBeingAnalyzed();
        if (clazz == null) {
            return false;
        }
        return context.isApplicationClass(clazz);
    }

    static public void reportMissingClass(edu.umd.cs.findbugs.classfile.MissingClassException e) {
        requireNonNull(e, "argument is null");
        reportMissingClass(e.getClassDescriptor());
    }

    static public void reportMissingClass(ClassDescriptor c) {
        requireNonNull(c, "argument is null");
        if (!analyzingApplicationClass()) {
            return;
        }
        String missing = c.getDottedClassName();
        if (missing.length() == 1) {
            System.out.println(c);
        }
        if (skipReportingMissingClass(missing)) {
            return;
        }
        RepositoryLookupFailureCallback lookupFailureCallback = getCurrentLookupFailureCallback();
        if (lookupFailureCallback != null) {
            lookupFailureCallback.reportMissingClass(c);
        }
    }

    /**
     * Report an error
     */
    static public void logError(String msg, Exception e) {
        AnalysisContext currentAnalysisContext2 = currentAnalysisContext();
        if (currentAnalysisContext2 == null) {
            if (e instanceof NoSuchBugPattern) {
                return;
            }
            /*
            if (false && SystemProperties.ASSERTIONS_ENABLED) {
                AssertionError e2 = new AssertionError("Exception logged with no analysis context");
                e2.initCause(e);
                throw e2;
            }
             */
            e.printStackTrace(System.err);
            return;
        }
        if (e instanceof MissingClassException) {
            reportMissingClass(((MissingClassException) e).getClassNotFoundException());
            return;
        }
        if (e instanceof edu.umd.cs.findbugs.classfile.MissingClassException) {
            reportMissingClass(((edu.umd.cs.findbugs.classfile.MissingClassException) e).toClassNotFoundException());
            return;
        }

        RepositoryLookupFailureCallback lookupFailureCallback = currentAnalysisContext2.getLookupFailureCallback();
        if (lookupFailureCallback != null) {
            lookupFailureCallback.logError(msg, e);
        }
    }

    /**
     * Report an error
     */
    static public void logError(String msg) {
        AnalysisContext currentAnalysisContext2 = currentAnalysisContext();
        if (currentAnalysisContext2 == null) {
            return;
        }
        currentAnalysisContext2.logAnError(msg);
    }

    public void logAnError(String msg) {
        RepositoryLookupFailureCallback lookupFailureCallback = getLookupFailureCallback();
        if (lookupFailureCallback != null) {
            lookupFailureCallback.logError(msg);
        }
    }

    public void analysisSkippedDueToInvokeDynamic(XMethod m) {
        if (!m.usesInvokeDynamic()) {
            throw new IllegalArgumentException();
        }
        if (skippedDueToInvokeDynamic.add(m.getMethodDescriptor())) {
            logAnError(m + " skipped due to invoke_dynamic");
        }

    }

    public boolean setMissingClassWarningsSuppressed(boolean value) {
        boolean oldValue = missingClassWarningsSuppressed;
        missingClassWarningsSuppressed = value;
        return oldValue;
    }

    // /**
    // * Add an application class to the repository.
    // *
    // * @param appClass the application class
    // */
    // public abstract void addApplicationClassToRepository(JavaClass appClass);

    /**
     * Return whether or not the given class is an application class.
     *
     * @param cls
     *            the class to lookup
     * @return true if the class is an application class, false if not an
     *         application class or if the class cannot be located
     */
    public boolean isApplicationClass(JavaClass cls) {
        // return getSubtypes().isApplicationClass(cls);
        return getSubtypes2().isApplicationClass(DescriptorFactory.createClassDescriptor(cls));
    }

    /**
     * Return whether or not the given class is an application class.
     *
     * @param className
     *            name of a class
     * @return true if the class is an application class, false if not an
     *         application class or if the class cannot be located
     */
    public boolean isApplicationClass(@DottedClassName String className) {
        // try {
        // JavaClass javaClass = lookupClass(className);
        // return isApplicationClass(javaClass);
        // } catch (ClassNotFoundException e) {
        // AnalysisContext.reportMissingClass(e);
        // return false;
        // }
        ClassDescriptor classDesc = DescriptorFactory.createClassDescriptorFromDottedClassName(className);
        return getSubtypes2().isApplicationClass(classDesc);
    }

    public boolean isApplicationClass(ClassDescriptor desc) {
        return getSubtypes2().isApplicationClass(desc);
    }

    public int getClassSize(ClassDescriptor desc) {
        IAnalysisCache analysisCache = Global.getAnalysisCache();

        try {
            /* ClassContext classContext =*/ analysisCache.getClassAnalysis(ClassContext.class, desc);
            ClassData classData = analysisCache.getClassAnalysis(ClassData.class, desc);
            return classData.getData().length;

        } catch (RuntimeException e) {
            AnalysisContext.logError("Error getting class data for " + desc, e);
            return 10000;
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Could not get class context for "  + desc, e);
            return 10000;
        }
    }

    public boolean isTooBig(ClassDescriptor desc) {
        IAnalysisCache analysisCache = Global.getAnalysisCache();

        try {
            ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, desc);
            ClassData classData = analysisCache.getClassAnalysis(ClassData.class, desc);
            if (classData.getData().length > 1000000) {
                return true;
            }
            try {
                JavaClass javaClass = classContext.getJavaClass();
                if (javaClass.getMethods().length > 1000) {
                    return true;
                }
            } catch (RuntimeException e) {
                AnalysisContext.logError("Error parsing class " + desc
                        + " from " + classData.getCodeBaseEntry().getCodeBase(), e);
                return true;
            }

        } catch (RuntimeException e) {
            AnalysisContext.logError("Error getting class data for " + desc, e);
            return true;
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Could not get class context for "  + desc, e);
            return true;
        }
        return false;
    }

    /**
     * Lookup a class.
     * <em>Use this method instead of Repository.lookupClass().</em>
     *
     * @param classDescriptor
     *            descriptor specifying the class to look up
     * @return the class
     * @throws ClassNotFoundException
     *             if the class can't be found
     */
    public JavaClass lookupClass(@Nonnull ClassDescriptor classDescriptor) throws ClassNotFoundException {
        return lookupClass(classDescriptor.toDottedClassName());
    }

    /**
     * This is equivalent to Repository.lookupClass() or this.lookupClass(),
     * except it uses the original Repository instead of the current one.
     *
     * This can be important because URLClassPathRepository objects are closed
     * after an analysis, so JavaClass objects obtained from them are no good on
     * subsequent runs.
     *
     * @param className
     *            the name of the class
     * @return the JavaClass representing the class
     * @throws ClassNotFoundException
     */
    public static JavaClass lookupSystemClass(@Nonnull String className) throws ClassNotFoundException {
        // TODO: eventually we should move to our own thread-safe repository
        // implementation
        requireNonNull (className, "className is null");
        if (originalRepository == null) {
            throw new IllegalStateException("originalRepository is null");
        }

        JavaClass clazz = originalRepository.findClass(className);
        return (clazz == null ? originalRepository.loadClass(className) : clazz);
    }

    /**
     * Lookup a class's source file
     *
     * @param dottedClassName
     *            the name of the class
     * @return the source file for the class, or
     *         {@link SourceLineAnnotation#UNKNOWN_SOURCE_FILE} if unable to
     *         determine
     */
    public final String lookupSourceFile(@Nonnull @DottedClassName String dottedClassName) {
        requireNonNull(dottedClassName, "className is null");
        try {
            XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class,
                    DescriptorFactory.createClassDescriptorFromDottedClassName(dottedClassName));
            String name = xClass.getSource();
            if (name == null) {
                return SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
            }
            return name;
        } catch (CheckedAnalysisException e) {
            return SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
        }
    }

    /**
     * If possible, load interprocedural property databases.
     */
    public final void loadInterproceduralDatabases() {
        loadPropertyDatabase(getFieldStoreTypeDatabase(), FieldStoreTypeDatabase.DEFAULT_FILENAME, "field store type database");
        loadPropertyDatabase(getUnconditionalDerefParamDatabase(), UNCONDITIONAL_DEREF_DB_FILENAME,
                "unconditional param deref database");
        loadPropertyDatabase(getReturnValueNullnessPropertyDatabase(), NONNULL_RETURN_DB_FILENAME, "nonnull return db database");
    }

    /**
     * If possible, load default (built-in) interprocedural property databases.
     * These are the databases for things like Java core APIs that unconditional
     * dereference parameters.
     */
    public final void loadDefaultInterproceduralDatabases() {
        if (IGNORE_BUILTIN_MODELS) {
            return;
        }
        loadPropertyDatabaseFromResource(getUnconditionalDerefParamDatabase(), UNCONDITIONAL_DEREF_DB_RESOURCE,
                "unconditional param deref database");
        loadPropertyDatabaseFromResource(getReturnValueNullnessPropertyDatabase(), NONNULL_RETURN_DB_RESOURCE,
                "nonnull return db database");
    }

    /**
     * Set a boolean property.
     *
     * @param prop
     *            the property to set
     * @param value
     *            the value of the property
     */
    public final void setBoolProperty(@AnalysisFeature int prop, boolean value) {
        boolPropertySet.set(prop, value);
    }

    /**
     * Get a boolean property.
     *
     * @param prop
     *            the property
     * @return value of the property; defaults to false if the property has not
     *         had a value assigned explicitly
     */
    public final boolean getBoolProperty(@AnalysisFeature int prop) {
        return boolPropertySet.get(prop);
    }

    /**
     * Set the interprocedural database input directory.
     *
     * @param databaseInputDir
     *            the interprocedural database input directory
     */
    public final void setDatabaseInputDir(String databaseInputDir) {
        if (DEBUG) {
            System.out.println("Setting database input directory: " + databaseInputDir);
        }
        this.databaseInputDir = databaseInputDir;
    }

    /**
     * Get the interprocedural database input directory.
     *
     * @return the interprocedural database input directory
     */
    public final String getDatabaseInputDir() {
        return databaseInputDir;
    }

    /**
     * Set the interprocedural database output directory.
     *
     * @param databaseOutputDir
     *            the interprocedural database output directory
     */
    public final void setDatabaseOutputDir(String databaseOutputDir) {
        if (DEBUG) {
            System.out.println("Setting database output directory: " + databaseOutputDir);
        }
        this.databaseOutputDir = databaseOutputDir;
    }

    /**
     * Get the interprocedural database output directory.
     *
     * @return the interprocedural database output directory
     */
    public final String getDatabaseOutputDir() {
        return databaseOutputDir;
    }

    /**
     * Load an interprocedural property database.
     *
     * @param <DatabaseType>
     *            actual type of the database
     * @param <KeyType>
     *            type of key (e.g., method or field)
     * @param <Property>
     *            type of properties stored in the database
     * @param database
     *            the empty database object
     * @param fileName
     *            file to load database from
     * @param description
     *            description of the database (for diagnostics)
     * @return the database object, or null if the database couldn't be loaded
     */
    public <DatabaseType extends PropertyDatabase<KeyType, Property>, KeyType extends FieldOrMethodDescriptor, Property> DatabaseType loadPropertyDatabase(
            DatabaseType database, String fileName, String description) {
        try {
            File dbFile = new File(getDatabaseInputDir(), fileName);
            if (DEBUG) {
                System.out.println("Loading " + description + " from " + dbFile.getPath() + "...");
            }

            database.readFromFile(dbFile.getPath());
            return database;
        } catch (IOException e) {
            getLookupFailureCallback().logError("Error loading " + description, e);
        } catch (PropertyDatabaseFormatException e) {
            getLookupFailureCallback().logError("Invalid " + description, e);
        }
        return null;
    }

    /**
     * Load an interprocedural property database.
     *
     * @param <DatabaseType>
     *            actual type of the database
     * @param <KeyType>
     *            type of key (e.g., method or field)
     * @param <Property>
     *            type of properties stored in the database
     * @param database
     *            the empty database object
     * @param resourceName
     *            name of resource to load the database from
     * @param description
     *            description of the database (for diagnostics)
     * @return the database object, or null if the database couldn't be loaded
     */
    public <DatabaseType extends PropertyDatabase<KeyType, Property>, KeyType extends FieldOrMethodDescriptor, Property> DatabaseType loadPropertyDatabaseFromResource(
            DatabaseType database, String resourceName, String description) {
        try {
            if (DEBUG) {
                System.out.println("Loading default " + description + " from " + resourceName + " @ "
                        + database.getClass().getResource(resourceName) + " ... ");
            }
            try(InputStream in = database.getClass().getResourceAsStream(resourceName)){
                if (in == null) {
                    AnalysisContext.logError("Unable to load " + description + " from resource " + resourceName);
                } else {
                    database.read(in);
                }
            }
            return database;
        } catch (IOException e) {
            getLookupFailureCallback().logError("Error loading " + description, e);
        } catch (PropertyDatabaseFormatException e) {
            getLookupFailureCallback().logError("Invalid " + description, e);
        }
        return null;
    }

    /**
     * Write an interprocedural property database.
     *
     * @param <DatabaseType>
     *            actual type of the database
     * @param <KeyType>
     *            type of key (e.g., method or field)
     * @param <Property>
     *            type of properties stored in the database
     * @param database
     *            the database
     * @param fileName
     *            name of database file
     * @param description
     *            description of the database
     */
    public <DatabaseType extends PropertyDatabase<KeyType, Property>, KeyType extends FieldOrMethodDescriptor, Property> void storePropertyDatabase(
            DatabaseType database, String fileName, String description) {

        try {
            File dbFile = new File(getDatabaseOutputDir(), fileName);
            if (DEBUG) {
                System.out.println("Writing " + description + " to " + dbFile.getPath() + "...");
            }
            database.writeToFile(dbFile.getPath());
        } catch (IOException e) {
            getLookupFailureCallback().logError("Error writing " + description, e);
        }
    }

    /**
     * Set the current analysis context for this thread.
     *
     * @param analysisContext
     *            the current analysis context for this thread
     */
    public static void setCurrentAnalysisContext(AnalysisContext analysisContext) {
        currentAnalysisContext.set(analysisContext);
        if (Global.getAnalysisCache() != null) {
            currentXFactory.set(new XFactory());
        }
    }

    public static void removeCurrentAnalysisContext() {
        AnalysisContext context = currentAnalysisContext();
        if (context != null) {
            context.clear();
        }
        currentAnalysisContext.remove();
    }

    /**
     * Get Collection of all XClass objects seen so far.
     *
     * @return Collection of all XClass objects seen so far
     */
    public Collection<XClass> getXClassCollection() {
        return getSubtypes2().getXClassCollection();
    }

    public SuppressionMatcher getSuppressionMatcher() {
        return suppressionMatcher;
    }

    /**
     * Add an entry to the Repository's classpath.
     *
     * @param url
     *            the classpath entry URL
     * @throws IOException
     */
    public void addClasspathEntry(String url) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Clear the ClassContext cache. This should be done between analysis
     * passes.
     */
    public void clearClassContextCache() {
        throw new UnsupportedOperationException();
    }

    /**
     * Clear the BCEL Repository in preparation for analysis.
     */
    public void clearRepository() {
        // Set the backing store for the BCEL Repository to
        // be the AnalysisCache.
        Repository.setRepository(new AnalysisCacheToRepositoryAdapter());
    }

    public AnnotationRetentionDatabase getAnnotationRetentionDatabase() {
        return getDatabase(AnnotationRetentionDatabase.class);
    }

    public CheckReturnAnnotationDatabase getCheckReturnAnnotationDatabase() {
        return getDatabase(CheckReturnAnnotationDatabase.class);
    }

    /**
     * Get the ClassContext for a class.
     *
     * @param javaClass
     *            the class
     * @return the ClassContext for that class
     */
    public ClassContext getClassContext(JavaClass javaClass) {
        // This is a bit silly since we're doing an unnecessary
        // ClassDescriptor->JavaClass lookup.
        // However, we can be assured that it will succeed.

        ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(
                ClassName.toSlashedClassName(javaClass.getClassName()));

        try {
            return Global.getAnalysisCache().getClassAnalysis(ClassContext.class, classDescriptor);
        } catch (CheckedAnalysisException e) {
            IllegalStateException ise = new IllegalStateException("Could not get ClassContext for JavaClass");
            ise.initCause(e);
            throw ise;
        }
    }

    /**
     * Get stats about hit rate for ClassContext cache.
     *
     * @return stats about hit rate for ClassContext cache
     */
    public String getClassContextStats() {
        return "<unknown ClassContext stats>";
    }

    /**
     * Get the property database recording the types of values stored into
     * fields.
     *
     * @return the database, or null if there is no database available
     */
    public FieldStoreTypeDatabase getFieldStoreTypeDatabase() {
        return getDatabase(FieldStoreTypeDatabase.class);
    }


    public JCIPAnnotationDatabase getJCIPAnnotationDatabase() {
        return getDatabase(JCIPAnnotationDatabase.class);
    }

    /**
     * Get the lookup failure callback.
     */
    public RepositoryLookupFailureCallback getLookupFailureCallback() {
        return lookupFailureCallback;
    }

    /**
     * Get the SourceFinder, for finding source files.
     */
    public SourceFinder getSourceFinder() {
        return project.getSourceFinder();
    }

    /**
     * Get the SourceInfoMap.
     */
    public SourceInfoMap getSourceInfoMap() {
        return getDatabase(SourceInfoMap.class);
    }

    /**
     * Get the property database recording which methods unconditionally
     * dereference parameters.
     *
     * @return the database, or null if there is no database available
     */
    public ParameterNullnessPropertyDatabase getUnconditionalDerefParamDatabase() {
        return getDatabase(ParameterNullnessPropertyDatabase.class);
    }

    /**
     * Instantiate the CheckReturnAnnotationDatabase. Do this after the
     * repository has been set up.
     */

    public void initDatabases() {
        // Databases are created on-demand - don't need to explicitly create
        // them
    }

    /**
     * Lookup a class.
     * <em>Use this method instead of Repository.lookupClass().</em>
     *
     * @param className
     *            the name of the class
     * @return the JavaClass representing the class
     * @throws ClassNotFoundException
     *             (but not really)
     */
    public JavaClass lookupClass(@Nonnull @DottedClassName String className) throws ClassNotFoundException {
        try {
            if (className.length() == 0) {
                throw new IllegalArgumentException("Class name is empty");
            }
            if (!ClassName.isValidClassName(className)) {
                throw new ClassNotFoundException("Invalid class name: " + className);
            }
            return Global.getAnalysisCache().getClassAnalysis(JavaClass.class,
                    DescriptorFactory.instance().getClassDescriptor(ClassName.toSlashedClassName(className)));
        } catch (CheckedAnalysisException e) {
            throw new ClassNotFoundException("Class not found: " + className, e);
        }
    }

    public InnerClassAccessMap getInnerClassAccessMap() {
        return getDatabase(InnerClassAccessMap.class);
    }

    public void setAppClassList(List<ClassDescriptor> appClassCollection) {
        // FIXME: we really should drive the progress callback here
        HashSet<ClassDescriptor> appSet = new HashSet<ClassDescriptor>(appClassCollection);

        Collection<ClassDescriptor> allClassDescriptors = new ArrayList<ClassDescriptor>(DescriptorFactory.instance()
                .getAllClassDescriptors());
        for (ClassDescriptor appClass : allClassDescriptors) {
            try {
                XClass xclass = currentXFactory().getXClass(appClass);

                if (xclass == null) {
                    continue;
                }

                // Add the application class to the database
                if (appSet.contains(appClass)) {
                    getSubtypes2().addApplicationClass(xclass);
                } else if (xclass instanceof ClassInfo) {
                    getSubtypes2().addClass(xclass);
                }

            } catch (Exception e) {
                AnalysisContext.logError("Unable to get XClass for " + appClass, e);
            }
        }

        if (true && Subtypes2.DEBUG) {
            System.out.println(getSubtypes2().getGraph().getNumVertices() + " vertices in inheritance graph");
        }
    }

    /**
     * After a pass has been completed, allow the analysis context to update
     * information.
     *
     * @param pass
     *            -- the first pass is pass 0
     */
    public void updateDatabases(int pass) {
        if (pass == 0) {
            getCheckReturnAnnotationDatabase().loadAuxiliaryAnnotations();
            getNullnessAnnotationDatabase().loadAuxiliaryAnnotations();
        }

    }

    /**
     * Get the property database recording which methods always return nonnull
     * values
     *
     * @return the database, or null if there is no database available
     */
    public ReturnValueNullnessPropertyDatabase getReturnValueNullnessPropertyDatabase() {
        return getDatabase(ReturnValueNullnessPropertyDatabase.class);
    }

    /**
     * Get the Subtypes2 inheritance hierarchy database.
     */
    public Subtypes2 getSubtypes2() {
        return Global.getAnalysisCache().getDatabase(Subtypes2.class);
    }


    public DirectlyRelevantTypeQualifiersDatabase getDirectlyRelevantTypeQualifiersDatabase() {
        return Global.getAnalysisCache().getDatabase(DirectlyRelevantTypeQualifiersDatabase.class);
    }


    @CheckForNull
    public XMethod getBridgeTo(MethodInfo m) {
        return bridgeTo.get(m);
    }


    @CheckForNull
    public XMethod getBridgeFrom(MethodInfo m) {
        return bridgeFrom.get(m);
    }


    public void setBridgeMethod(MethodInfo from, MethodInfo to) {
        bridgeTo.put(from, to);
        bridgeFrom.put(to, from);
    }

    public TypeQualifierNullnessAnnotationDatabase getNullnessAnnotationDatabase() {
        if (tqNullnessDatabase == null) {
            tqNullnessDatabase = new TypeQualifierNullnessAnnotationDatabase();
        }
        return tqNullnessDatabase;

    }

    protected <E> E getDatabase(Class<E> cls) {
        return Global.getAnalysisCache().getDatabase(cls);
    }

    static class DelegatingRepositoryLookupFailureCallback implements RepositoryLookupFailureCallback {

        @Override
        public void logError(String message) {
            Global.getAnalysisCache().getErrorLogger().logError(message);
        }

        @Override
        public void logError(String message, Throwable e) {
            Global.getAnalysisCache().getErrorLogger().logError(message, e);
        }

        @Override
        public void reportMissingClass(ClassNotFoundException ex) {
            Global.getAnalysisCache().getErrorLogger().reportMissingClass(ex);
        }

        @Override
        public void reportMissingClass(ClassDescriptor classDescriptor) {
            Global.getAnalysisCache().getErrorLogger().reportMissingClass(classDescriptor);
        }

        @Override
        public void reportSkippedAnalysis(MethodDescriptor method) {
            Global.getAnalysisCache().getErrorLogger().reportSkippedAnalysis(method);
        }

    }

}

