/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2008 University of Maryland
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

package edu.umd.cs.findbugs.classfile.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.JavaVersion;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.engine.ClassParser;
import edu.umd.cs.findbugs.classfile.engine.ClassParserInterface;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.util.Archive;
import edu.umd.cs.findbugs.util.ClassPathUtil;

/**
 * Implementation of IClassPathBuilder.
 *
 * @author David Hovemeyer
 */
public class ClassPathBuilder implements IClassPathBuilder {
    private static final boolean VERBOSE = SystemProperties.getBoolean("findbugs2.builder.verbose");

    private static final boolean DEBUG = VERBOSE || SystemProperties.getBoolean("findbugs2.builder.debug");

    private static final boolean NO_PARSE_CLASS_NAMES = SystemProperties.getBoolean("findbugs2.builder.noparseclassnames");

    /**
     * Worklist item. Represents one codebase to be processed during the
     * classpath construction algorithm.
     */
    static class WorkListItem {
        private final ICodeBaseLocator codeBaseLocator;

        private final boolean isAppCodeBase;

        private final ICodeBase.Discovered howDiscovered;

        @Override
        public String toString() {
            return "WorkListItem(" + codeBaseLocator + ", " + isAppCodeBase + ", " + howDiscovered + ")";
        }

        public WorkListItem(ICodeBaseLocator codeBaseLocator, boolean isApplication, ICodeBase.Discovered howDiscovered) {
            this.codeBaseLocator = codeBaseLocator;
            this.isAppCodeBase = isApplication;
            this.howDiscovered = howDiscovered;
        }

        public ICodeBaseLocator getCodeBaseLocator() {
            return codeBaseLocator;
        }

        public boolean isAppCodeBase() {
            return isAppCodeBase;
        }

        /**
         * @return Returns the howDiscovered.
         */
        public ICodeBase.Discovered getHowDiscovered() {
            return howDiscovered;
        }
    }

    /**
     * A codebase discovered during classpath building.
     */
    static class DiscoveredCodeBase {
        ICodeBase codeBase;

        LinkedList<ICodeBaseEntry> resourceList;

        public DiscoveredCodeBase(ICodeBase codeBase) {
            this.codeBase = codeBase;
            this.resourceList = new LinkedList<>();
        }

        public ICodeBase getCodeBase() {
            return codeBase;
        }

        public LinkedList<ICodeBaseEntry> getResourceList() {
            return resourceList;
        }

        public void addCodeBaseEntry(ICodeBaseEntry entry) {
            resourceList.add(entry);
        }

        public ICodeBaseIterator iterator() throws InterruptedException {
            if (codeBase instanceof IScannableCodeBase) {
                return ((IScannableCodeBase) codeBase).iterator();
            } else {
                return new ICodeBaseIterator() {
                    @Override
                    public boolean hasNext() throws InterruptedException {
                        return false;
                    }

                    @Override
                    public ICodeBaseEntry next() throws InterruptedException {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }
    }

    // Fields
    private final IClassFactory classFactory;

    private final IErrorLogger errorLogger;

    private final LinkedList<WorkListItem> projectWorkList;

    private final LinkedList<DiscoveredCodeBase> discoveredCodeBaseList;

    private final Map<String, DiscoveredCodeBase> discoveredCodeBaseMap;

    private final LinkedList<ClassDescriptor> appClassList;

    private boolean scanNestedArchives;

    /**
     * Constructor.
     *
     * @param classFactory
     *            the class factory
     * @param errorLogger
     *            the error logger
     */
    ClassPathBuilder(IClassFactory classFactory, IErrorLogger errorLogger) {
        this.classFactory = classFactory;
        this.errorLogger = errorLogger;
        this.projectWorkList = new LinkedList<>();
        this.discoveredCodeBaseList = new LinkedList<>();
        this.discoveredCodeBaseMap = new HashMap<>();
        this.appClassList = new LinkedList<>();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassPathBuilder#addCodeBase(edu.umd.cs
     * .findbugs.classfile.ICodeBaseLocator, boolean)
     */
    @Override
    public void addCodeBase(ICodeBaseLocator locator, boolean isApplication) {
        addToWorkList(projectWorkList, new WorkListItem(locator, isApplication, ICodeBase.Discovered.SPECIFIED));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassPathBuilder#scanNestedArchives(boolean
     * )
     */
    @Override
    public void scanNestedArchives(boolean scanNestedArchives) {
        this.scanNestedArchives = scanNestedArchives;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IClassPathBuilder#build(edu.umd.cs.findbugs
     * .classfile.IClassPath,
     * edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress)
     */
    @Override
    public void build(IClassPath classPath, IClassPathBuilderProgress progress) throws CheckedAnalysisException, IOException,
            InterruptedException {
        // Discover all directly and indirectly referenced codebases
        processWorkList(classPath, projectWorkList, progress);

        // If not already located, try to locate any additional codebases
        // containing classes required for analysis.
        if (!discoveredCodeBaseList.isEmpty()) {
            locateCodebasesRequiredForAnalysis(classPath, progress);
        }

        // Add all discovered codebases to the classpath
        for (DiscoveredCodeBase discoveredCodeBase : discoveredCodeBaseList) {
            classPath.addCodeBase(discoveredCodeBase.getCodeBase());
        }

        Set<ClassDescriptor> appClassSet = new HashSet<>();

        // Build collection of all application classes.
        // Also, add resource name -> codebase entry mappings for application
        // classes.
        for (DiscoveredCodeBase discoveredCodeBase : discoveredCodeBaseList) {
            if (!discoveredCodeBase.getCodeBase().isApplicationCodeBase()) {
                continue;
            }

            codeBaseEntryLoop: for (ICodeBaseIterator i = discoveredCodeBase.iterator(); i.hasNext();) {
                ICodeBaseEntry entry = i.next();
                if (!DescriptorFactory.isClassResource(entry.getResourceName())) {
                    continue;
                }

                ClassDescriptor classDescriptor = entry.getClassDescriptor();
                if (classDescriptor == null) {
                    throw new IllegalStateException();
                }

                if (appClassSet.contains(classDescriptor)) {
                    // An earlier entry takes precedence over this class
                    continue codeBaseEntryLoop;
                }
                appClassSet.add(classDescriptor);
                appClassList.add(classDescriptor);

                classPath.mapResourceNameToCodeBaseEntry(entry.getResourceName(), entry);
            }
        }

        if (DEBUG) {
            System.out.println("Classpath:");
            dumpCodeBaseList(classPath.appCodeBaseIterator(), "Application codebases");
            dumpCodeBaseList(classPath.auxCodeBaseIterator(), "Auxiliary codebases");
        }

        // Make sure we always know if we can't find system classes
        ICodeBaseEntry resource = classPath.lookupResource("java/lang/Object.class");
        if (resource == null) {
            throw new ResourceNotFoundException("java/lang/Object.class");
        }
    }

    /**
     * Make an effort to find the codebases containing any files required for
     * analysis.
     */
    private void locateCodebasesRequiredForAnalysis(IClassPath classPath, IClassPathBuilderProgress progress)
            throws InterruptedException, IOException, ResourceNotFoundException {
        boolean foundJavaLangObject = false;
        boolean foundFindBugsAnnotations = false;
        boolean foundJSR305Annotations = false;

        for (DiscoveredCodeBase discoveredCodeBase : discoveredCodeBaseList) {
            if (!foundJavaLangObject) {
                foundJavaLangObject = probeCodeBaseForResource(discoveredCodeBase, "java/lang/Object.class");
            }
            if (!foundFindBugsAnnotations) {
                foundFindBugsAnnotations = probeCodeBaseForResource(discoveredCodeBase,
                        "edu/umd/cs/findbugs/annotations/Nonnull.class");
            }
            if (!foundJSR305Annotations) {
                foundJSR305Annotations = probeCodeBaseForResource(discoveredCodeBase, "javax/annotation/meta/TypeQualifier.class");
                if (DEBUG) {
                    System.out.println("foundJSR305Annotations: " + foundJSR305Annotations);
                }
            }
        }

        if (!foundJavaLangObject) {
            processWorkList(classPath, buildSystemCodebaseList(), progress);
        }

        // If we're running findbugs-full.jar, IT contains the contents
        // of jsr305.jar and annotations.jar. So, add it to the classpath.
        if (runningFindBugsFullJar()) {
            processWorkList(classPath, buildFindBugsFullJarCodebaseList(), progress);
            return;
        }

        // Not running findbugs-full.jar: try to find jsr305.jar and
        // annotations.jar.

        if (!foundFindBugsAnnotations) {
            processWorkList(classPath, buildFindBugsAnnotationCodebaseList(), progress);
        }
        if (!foundJSR305Annotations) {
            processWorkList(classPath, buildJSR305AnnotationsCodebaseList(), progress);
        }
    }

    private boolean runningFindBugsFullJar() {
        String findbugsFullJar = ClassPathUtil.findCodeBaseInClassPath("findbugs-full.jar",
                SystemProperties.getProperty("java.class.path"));
        return findbugsFullJar != null;
    }

    private LinkedList<WorkListItem> buildFindBugsFullJarCodebaseList() {
        String findbugsFullJar = ClassPathUtil.findCodeBaseInClassPath("findbugs-full.jar",
                SystemProperties.getProperty("java.class.path"));

        LinkedList<WorkListItem> workList = new LinkedList<>();
        if (findbugsFullJar != null) {
            //
            // Found findbugs-full.jar: add it to the aux classpath.
            // (This is a bit weird, since we only want to resolve a subset
            // of its classes.)
            //
            ICodeBaseLocator loc = new FilesystemCodeBaseLocator(findbugsFullJar);
            workList.addLast(new WorkListItem(loc, false, ICodeBase.Discovered.IN_SYSTEM_CLASSPATH));
        }
        return workList;
    }

    /**
     * Probe a codebase to see if a given source exists in that code base.
     *
     * @param resourceName
     *            name of a resource
     * @return true if the resource exists in the codebase, false if not
     */
    private boolean probeCodeBaseForResource(DiscoveredCodeBase discoveredCodeBase, String resourceName) {
        ICodeBaseEntry resource = discoveredCodeBase.getCodeBase().lookupResource(resourceName);
        return resource != null;
    }

    private void dumpCodeBaseList(Iterator<? extends ICodeBase> i, String desc) throws InterruptedException {
        System.out.println("  " + desc + ":");
        while (i.hasNext()) {
            ICodeBase codeBase = i.next();
            System.out.println("    " + codeBase.getCodeBaseLocator().toString());
            if (codeBase.containsSourceFiles()) {
                System.out.println("      * contains source files");
            }
        }
    }

    private LinkedList<WorkListItem> buildSystemCodebaseList() {
        // This method is based on the
        // org.apache.bcel.util.ClassPath.getClassPath()
        // method.

        LinkedList<WorkListItem> workList = new LinkedList<>();

        String bootClassPath = SystemProperties.getProperty("sun.boot.class.path");
        // Seed worklist with system codebases.
        // addWorkListItemsForClasspath(workList,
        // SystemProperties.getProperty("java.class.path"));
        addWorkListItemsForClasspath(workList, bootClassPath);
        String extPath = SystemProperties.getProperty("java.ext.dirs");
        if (extPath != null) {
            StringTokenizer st = new StringTokenizer(extPath, File.pathSeparator);
            while (st.hasMoreTokens()) {
                String extDir = st.nextToken();
                addWorkListItemsForExtDir(workList, extDir);
            }
        }

        if (isJava9orLater()) {
            Path jrtFsJar = Paths.get(System.getProperty("java.home", ""), "lib/jrt-fs.jar");
            if (Files.isRegularFile(jrtFsJar)) {
                addWorkListItemsForClasspath(workList, jrtFsJar.toString());
            }
        }
        return workList;
    }

    private static boolean isJava9orLater() {
        JavaVersion javaVersion = JavaVersion.getRuntimeVersion();
        return javaVersion.getMajor() >= 9;
    }

    /**
     * Create a worklist that will add the FindBugs lib/annotations.jar to the
     * classpath.
     */
    private LinkedList<WorkListItem> buildFindBugsAnnotationCodebaseList() {
        return createFindBugsLibWorkList("annotations.jar");
    }

    /**
     * Create a worklist that will add the FindBugs lib/jsr305.jar to the
     * classpath.
     */
    private LinkedList<WorkListItem> buildJSR305AnnotationsCodebaseList() {
        return createFindBugsLibWorkList("jsr305.jar");
    }

    private LinkedList<WorkListItem> createFindBugsLibWorkList(String jarFileName) {
        LinkedList<WorkListItem> workList = new LinkedList<>();

        boolean found = false;

        String findbugsHome = FindBugs.getHome();
        if (findbugsHome != null) {
            //
            // If the findbugs.home property is set,
            // we should be able to find the jar file in
            // the lib subdirectory.
            //
            File base = new File(findbugsHome);
            File loc1 = new File(new File(base, "lib"), jarFileName);
            File loc2 = new File(base, jarFileName);
            File loc = null;
            if (loc1.exists()) {
                loc = loc1;
            } else if (loc2.exists()) {
                loc = loc2;
            }
            if (loc != null) {
                found = true;
                ICodeBaseLocator codeBaseLocator = classFactory.createFilesystemCodeBaseLocator(loc.getPath());
                workList.add(new WorkListItem(codeBaseLocator, false, ICodeBase.Discovered.IN_SYSTEM_CLASSPATH));
            }
        }

        if (!found) {
            if (DEBUG) {
                System.out.println("Looking for " + jarFileName + " on classpath...");
            }
            //
            // See if the required jar file is available on the class path.
            //
            String javaClassPath = SystemProperties.getProperty("java.class.path");
            StringTokenizer t = new StringTokenizer(javaClassPath, File.pathSeparator);
            while (t.hasMoreTokens()) {
                String entry = t.nextToken();
                if (DEBUG) {
                    System.out.print("  Checking " + entry + "...");
                }

                if (matchesJarFile(entry, jarFileName)) {
                    found = true;
                } else if (matchesJarFile(entry, "spotbugs.jar")) {
                    // See if the searched-for jar file can be found
                    // alongside spotbugs.jar.
                    File findbugsJar = new File(entry);
                    File loc = new File(findbugsJar.getParent() + File.separator + jarFileName);
                    if (DEBUG) {
                        System.out.print(" [spotbugs.jar, checking " + loc.getPath() + "] ");
                    }
                    if (loc.exists()) {
                        entry = loc.getPath();
                        found = true;
                    }
                }

                if (DEBUG) {
                    System.out.println(found ? "FOUND" : "no");
                }
                if (found) {
                    ICodeBaseLocator codeBaseLocator = classFactory.createFilesystemCodeBaseLocator(entry);
                    workList.add(new WorkListItem(codeBaseLocator, false, ICodeBase.Discovered.IN_SYSTEM_CLASSPATH));
                    break;
                }

            }
        }

        return workList;
    }

    private boolean matchesJarFile(String entry, String jarFileName) {
        return entry.equals(jarFileName) || entry.endsWith(File.separator + jarFileName) || entry.endsWith("/" + jarFileName);
    }

    /**
     * Add worklist items from given system classpath.
     *
     * @param workList
     *            the worklist
     * @param path
     *            a system classpath
     */
    private void addWorkListItemsForClasspath(LinkedList<WorkListItem> workList, String path) {
        if (path == null) {
            return;
        }

        StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String entry = st.nextToken();
            if (DEBUG) {
                System.out.println("System classpath entry: " + entry);
            }
            addToWorkList(workList, new WorkListItem(classFactory.createFilesystemCodeBaseLocator(entry), false,
                    ICodeBase.Discovered.IN_SYSTEM_CLASSPATH));
        }
    }

    /**
     * Add worklist items from given extensions directory.
     *
     * @param workList
     *            the worklist
     * @param extDir
     *            an extensions directory
     */
    private void addWorkListItemsForExtDir(LinkedList<WorkListItem> workList, String extDir) {
        File dir = new File(extDir);
        File[] fileList = dir.listFiles((FileFilter) pathname -> {
            String path = pathname.getPath();
            boolean isArchive = Archive.isArchiveFileName(path);
            return isArchive;
        });
        if (fileList == null) {
            return;
        }

        for (File archive : fileList) {
            addToWorkList(workList, new WorkListItem(classFactory.createFilesystemCodeBaseLocator(archive.getPath()), false,
                    ICodeBase.Discovered.IN_SYSTEM_CLASSPATH));
        }
    }

    /**
     * Process classpath worklist items. We will attempt to find all nested
     * archives and Class-Path entries specified in Jar manifests. This should
     * give us as good an idea as possible of all of the classes available (and
     * which are part of the application).
     *
     * @param workList
     *            the worklist to process
     * @param progress
     *            IClassPathBuilderProgress callback
     * @throws InterruptedException
     * @throws IOException
     * @throws ResourceNotFoundException
     */
    private void processWorkList(IClassPath classPath, LinkedList<WorkListItem> workList, IClassPathBuilderProgress progress)
            throws InterruptedException, IOException, ResourceNotFoundException {
        // Build the classpath, scanning codebases for nested archives
        // and referenced codebases.
        while (!workList.isEmpty()) {
            WorkListItem item = workList.removeFirst();
            if (item.getHowDiscovered() == ICodeBase.Discovered.SPECIFIED) {
                progress.startArchive(item.toString());
            }
            if (DEBUG) {
                System.out.println("Working: " + item.getCodeBaseLocator());
            }

            DiscoveredCodeBase discoveredCodeBase;

            // See if we have encountered this codebase before
            discoveredCodeBase = discoveredCodeBaseMap.get(item.getCodeBaseLocator().toString());
            if (discoveredCodeBase != null) {
                // If the codebase is not an app codebase and
                // the worklist item says that it is an app codebase,
                // change it. Otherwise, we have nothing to do.
                if (!discoveredCodeBase.getCodeBase().isApplicationCodeBase() && item.isAppCodeBase()) {
                    discoveredCodeBase.getCodeBase().setApplicationCodeBase(true);
                }

                continue;
            }

            // Detect .java files, which are probably human error
            if (item.getCodeBaseLocator() instanceof FilesystemCodeBaseLocator) {
                FilesystemCodeBaseLocator l = (FilesystemCodeBaseLocator) item.getCodeBaseLocator();
                if (l.getPathName().endsWith(".java")) {
                    if (DEBUG) {
                        System.err.println("Ignoring .java file \"" + l.getPathName() + "\" specified in classpath or auxclasspath");
                    }
                    continue;
                }
            }

            // If we are working on an application codebase,
            // then failing to open/scan it is a fatal error.
            // We issue warnings about problems with aux codebases,
            // but continue anyway.

            try {
                // Open the codebase and add it to the classpath
                discoveredCodeBase = new DiscoveredCodeBase(item.getCodeBaseLocator().openCodeBase());
                discoveredCodeBase.getCodeBase().setApplicationCodeBase(item.isAppCodeBase());
                discoveredCodeBase.getCodeBase().setHowDiscovered(item.getHowDiscovered());

                // Note that this codebase has been visited
                discoveredCodeBaseMap.put(item.getCodeBaseLocator().toString(), discoveredCodeBase);
                discoveredCodeBaseList.addLast(discoveredCodeBase);

                // If it is a scannable codebase, check it for nested archives.
                // In addition, if it is an application codebase then
                // make a list of application classes.
                if (discoveredCodeBase.getCodeBase() instanceof IScannableCodeBase
                        && (discoveredCodeBase.codeBase.isApplicationCodeBase()
                                || item.getHowDiscovered() == ICodeBase.Discovered.SPECIFIED)) {
                    scanCodebase(classPath, workList, discoveredCodeBase);
                }

                // Check for a Jar manifest for additional aux classpath
                // entries.
                scanJarManifestForClassPathEntries(workList, discoveredCodeBase.getCodeBase());
            } catch (IOException e) {
                if (item.isAppCodeBase() || item.getHowDiscovered() == ICodeBase.Discovered.SPECIFIED) {
                    if (e instanceof FileNotFoundException) {
                        if (item.isAppCodeBase()) {
                            errorLogger.logError("File from project not found: " + item.getCodeBaseLocator(), e);
                        } else {
                            errorLogger.logError("File from auxiliary classpath not found: " + item.getCodeBaseLocator(), e);
                        }
                    } else {
                        errorLogger.logError("Cannot open codebase " + item.getCodeBaseLocator(), e);
                    }
                }
            } catch (ResourceNotFoundException e) {
                if (item.getHowDiscovered() == ICodeBase.Discovered.SPECIFIED) {
                    errorLogger.logError("Cannot open codebase " + item.getCodeBaseLocator(), e);
                }
            }

            if (item.getHowDiscovered() == ICodeBase.Discovered.SPECIFIED) {
                progress.finishArchive();
            }
        }
    }

    /**
     * Scan given codebase in order to
     * <ul>
     * <li>check the codebase for nested archives (adding any found to the
     * worklist)
     * <li>build a list of class resources found in the codebase
     * </ul>
     *
     * @param workList
     *            the worklist
     * @param discoveredCodeBase
     *            the codebase to scan
     * @throws InterruptedException
     */
    private void scanCodebase(IClassPath classPath, LinkedList<WorkListItem> workList, DiscoveredCodeBase discoveredCodeBase)
            throws InterruptedException {
        if (DEBUG) {
            System.out.println("Scanning " + discoveredCodeBase.getCodeBase().getCodeBaseLocator());
        }

        IScannableCodeBase codeBase = (IScannableCodeBase) discoveredCodeBase.getCodeBase();

        ICodeBaseIterator i = codeBase.iterator();
        while (i.hasNext()) {
            ICodeBaseEntry entry = i.next();
            if (VERBOSE) {
                System.out.println("Entry: " + entry.getResourceName());
            }

            if (!NO_PARSE_CLASS_NAMES && codeBase.isApplicationCodeBase()
                    && DescriptorFactory.isClassResource(entry.getResourceName()) && !(entry instanceof SingleFileCodeBaseEntry)) {
                parseClassName(entry);
            }

            // Note the resource exists in this codebase
            discoveredCodeBase.addCodeBaseEntry(entry);

            // If resource is a nested archive, add it to the worklist
            if (scanNestedArchives && (codeBase.isApplicationCodeBase() || codeBase instanceof DirectoryCodeBase)
                    && Archive.isLibraryFileName(entry.getResourceName())) {
                if (VERBOSE) {
                    System.out.println("Entry is an library!");
                }
                ICodeBaseLocator nestedArchiveLocator = classFactory.createNestedArchiveCodeBaseLocator(codeBase,
                        entry.getResourceName());
                addToWorkList(workList,
                        new WorkListItem(nestedArchiveLocator, codeBase.isApplicationCodeBase(), ICodeBase.Discovered.NESTED));
            }
        }
    }

    /**
     * Attempt to parse data of given resource in order to divine the real name
     * of the class contained in the resource.
     *
     * @param entry
     *            the resource
     */
    private void parseClassName(ICodeBaseEntry entry) {
        DataInputStream in = null;
        try {
            InputStream resourceIn = entry.openResource();
            if (resourceIn == null) {
                throw new NullPointerException("Got null resource");
            }
            in = new DataInputStream(resourceIn);
            ClassParserInterface parser = new ClassParser(in, null, entry);
            ClassNameAndSuperclassInfo.Builder builder = new ClassNameAndSuperclassInfo.Builder();
            parser.parse(builder);

            String trueResourceName = builder.build().getClassDescriptor().toResourceName();
            if (!trueResourceName.equals(entry.getResourceName())) {
                entry.overrideResourceName(trueResourceName);
            }
        } catch (IOException e) {
            errorLogger.logError("Invalid class resource " + entry.getResourceName() + " in " + entry, e);
        } catch (InvalidClassFileFormatException e) {
            errorLogger.logError("Invalid class resource " + entry.getResourceName() + " in " + entry, e);
        } finally {
            IO.close(in);
        }
    }

    /**
     * Check a codebase for a Jar manifest to examine for Class-Path entries.
     *
     * @param workList
     *            the worklist
     * @param codeBase
     *            the codebase for examine for a Jar manifest
     * @throws IOException
     */
    private void scanJarManifestForClassPathEntries(LinkedList<WorkListItem> workList, ICodeBase codeBase) throws IOException {
        // See if this codebase has a jar manifest
        ICodeBaseEntry manifestEntry = codeBase.lookupResource("META-INF/MANIFEST.MF");
        if (manifestEntry == null) {
            // Do nothing - no Jar manifest found
            return;
        }

        // Try to read the manifest
        InputStream in = null;
        try {
            in = manifestEntry.openResource();
            Manifest manifest = new Manifest(in);

            Attributes mainAttrs = manifest.getMainAttributes();
            String classPath = mainAttrs.getValue("Class-Path");
            if (classPath != null) {
                String[] pathList = classPath.split("\\s+");

                for (String path : pathList) {
                    // Create a codebase locator for the classpath entry
                    // relative to the codebase in which we discovered the Jar
                    // manifest
                    ICodeBaseLocator relativeCodeBaseLocator = codeBase.getCodeBaseLocator().createRelativeCodeBaseLocator(path);

                    // Codebases found in Class-Path entries are always
                    // added to the aux classpath, not the application.
                    addToWorkList(workList, new WorkListItem(relativeCodeBaseLocator, false, ICodeBase.Discovered.IN_JAR_MANIFEST));
                }
            }
        } finally {
            if (in != null) {
                IO.close(in);
            }
        }
    }

    /**
     * Add a worklist item to the worklist. This method maintains the invariant
     * that all of the worklist items representing application codebases appear
     * <em>before</em> all of the worklist items representing auxiliary
     * codebases.
     *
     * @param workList
     *            the worklist
     * @param itemToAdd
     *            the worklist item to add
     */
    private void addToWorkList(LinkedList<WorkListItem> workList, WorkListItem itemToAdd) {
        if (DEBUG) {
            new RuntimeException("Adding work list item " + itemToAdd).printStackTrace(System.out);
        }
        if (!itemToAdd.isAppCodeBase()) {
            // Auxiliary codebases are always added at the end
            workList.addLast(itemToAdd);
            return;
        }

        // Adding an application codebase: position a ListIterator
        // just before first auxiliary codebase (or at the end of the list
        // if there are no auxiliary codebases)
        ListIterator<WorkListItem> i = workList.listIterator();
        while (i.hasNext()) {
            WorkListItem listItem = i.next();
            if (!listItem.isAppCodeBase()) {
                i.previous();
                break;
            }
        }

        // Add the codebase to the worklist
        i.add(itemToAdd);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#getAppClassList()
     */
    @Override
    public List<ClassDescriptor> getAppClassList() {
        return appClassList;
    }
}
