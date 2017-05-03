/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.annotation.WillClose;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.util.Util;

/**
 * Class to open input streams on source files. It maintains a "source path",
 * which is like a classpath, but for finding source files instead of class
 * files.
 */
public class SourceFinder {
    private static final boolean DEBUG = SystemProperties.getBoolean("srcfinder.debug");

    private static final int CACHE_SIZE = 50;

    /*
     * ----------------------------------------------------------------------
     * Helper classes
     * ----------------------------------------------------------------------
     */

    /**
     * Cache of SourceFiles. We use this to avoid repeatedly having to read
     * frequently accessed source files.
     */
    private static class Cache extends LinkedHashMap<String, SourceFile> {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SourceFile> eldest) {
            return size() >= CACHE_SIZE;
        }
    }

    /**
     * A repository of source files.
     */
    private interface SourceRepository {
        public boolean contains(String fileName);

        public boolean isPlatformDependent();

        public SourceFileDataSource getDataSource(String fileName);
    }

    /**
     * A directory containing source files.
     */
    private static class DirectorySourceRepository implements SourceRepository {
        private final String baseDir;

        public DirectorySourceRepository(String baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public String toString() {
            return "DirectorySourceRepository:" + baseDir;
        }

        @Override
        public boolean contains(String fileName) {
            File file = new File(getFullFileName(fileName));
            boolean exists = file.exists();
            if (DEBUG) {
                System.out.println("Exists " + exists + " for " + file);
            }
            return exists;
        }

        @Override
        public boolean isPlatformDependent() {
            return true;
        }

        @Override
        public SourceFileDataSource getDataSource(String fileName) {
            return new FileSourceFileDataSource(getFullFileName(fileName));
        }

        private String getFullFileName(String fileName) {
            return baseDir + File.separator + fileName;
        }
    }

    private static class InMemorySourceRepository implements SourceRepository {

        Map<String, byte[]> contents = new HashMap<String, byte[]>();
        Map<String, Long> lastModified = new HashMap<String, Long>();

        InMemorySourceRepository(@WillClose ZipInputStream in) throws IOException {
            try {
                while (true) {

                    ZipEntry e = in.getNextEntry();
                    if (e == null) {
                        break;
                    }
                    if (!e.isDirectory()) {
                        String name = e.getName();
                        long size = e.getSize();

                        if (size > Integer.MAX_VALUE) {
                            throw new IOException(name + " is too big at " + size + " bytes");
                        }
                        ByteArrayOutputStream out;
                        if (size <= 0) {
                            out = new ByteArrayOutputStream();
                        } else {
                            out = new ByteArrayOutputStream((int) size);
                        }
                        GZIPOutputStream gOut = new GZIPOutputStream(out);
                        IO.copy(in, gOut);
                        gOut.close();
                        byte data[] = out.toByteArray();
                        contents.put(name, data);
                        lastModified.put(name, e.getTime());
                    }
                    in.closeEntry();
                }
            } finally {
                Util.closeSilently(in);
            }

        }

        @Override
        public boolean contains(String fileName) {
            return contents.containsKey(fileName);
        }

        @Override
        public SourceFileDataSource getDataSource(final String fileName) {
            return new SourceFileDataSource() {

                @Override
                public String getFullFileName() {
                    return fileName;
                }

                @Override
                public InputStream open() throws IOException {
                    return new GZIPInputStream(new ByteArrayInputStream(contents.get(fileName)));
                }

                @Override
                public long getLastModified() {
                    Long when = lastModified.get(fileName);
                    if (when == null || when < 0) {
                        return 0;
                    }
                    return when;
                }
            };
        }

        @Override
        public boolean isPlatformDependent() {
            return false;
        }
    }

    SourceRepository makeInMemorySourceRepository(final String url) {
        final BlockingSourceRepository r = new BlockingSourceRepository();
        Util.runInDameonThread(new Runnable() {

            @Override
            public void run() {
                InputStream in = null;
                try {

                    URLConnection connection = new URL(url).openConnection();
                    in = connection.getInputStream();
                    if (getProject().isGuiAvaliable()) {
                        in = getProject().getGuiCallback().getProgressMonitorInputStream(in, connection.getContentLength(),
                                "Downloading project source code...");
                    }

                    if (url.endsWith(".z0p.gz")) {
                        in = new GZIPInputStream(in);
                    }

                    r.setBase(new InMemorySourceRepository(new ZipInputStream(in)));

                } catch (IOException e) {
                    if (getProject().isGuiAvaliable()) {
                        getProject().getGuiCallback().setErrorMessage("Unable to load " + url + "; " + e.getMessage());
                    }
                    AnalysisContext.logError("Unable to load " + url, e);
                    Util.closeSilently(in);
                }
            }
        }, "Source loading thread");
        return r;
    }

    SourceRepository makeJarURLConnectionSourceRepository(final String url) throws MalformedURLException, IOException {
        final File file = File.createTempFile("jar_cache", null);
        file.deleteOnExit();
        final BlockingSourceRepository r = new BlockingSourceRepository();
        Util.runInDameonThread(new Runnable() {

            @Override
            public void run() {
                InputStream in = null;
                OutputStream out = null;
                try {
                    URLConnection connection = new URL(url).openConnection();
                    if (getProject().isGuiAvaliable()) {
                        int size = connection.getContentLength();
                        in = getProject().getGuiCallback().getProgressMonitorInputStream(connection.getInputStream(), size,
                                "Loading source via url");
                    } else {
                        in = connection.getInputStream();
                    }
                    out = new FileOutputStream(file);
                    IO.copy(in, out);
                    r.setBase(new ZipSourceRepository(new ZipFile(file)));
                } catch (IOException e) {
                    assert true;
                } finally {
                    Util.closeSilently(in);
                    Util.closeSilently(out);
                }
            }
        }, "Source loading thread");
        return r;
    }

    static class BlockingSourceRepository implements SourceRepository {
        SourceRepository base;

        final CountDownLatch ready = new CountDownLatch(1);

        public BlockingSourceRepository() {
        }

        public boolean isReady() {
            return ready.getCount() == 0;
        }

        public void setBase(SourceRepository base) {
            this.base = base;
            ready.countDown();
        }

        private void await() {
            try {
                ready.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Unexpected interrupt", e);
            }
        }

        @Override
        public boolean contains(String fileName) {
            await();
            return base.contains(fileName);
        }

        @Override
        public SourceFileDataSource getDataSource(String fileName) {
            await();
            return base.getDataSource(fileName);
        }

        @Override
        public boolean isPlatformDependent() {
            await();
            return base.isPlatformDependent();
        }

    }

    /**
     * A zip or jar archive containing source files.
     */
    static class ZipSourceRepository implements SourceRepository {
        ZipFile zipFile;

        public ZipSourceRepository(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        @Override
        public boolean contains(String fileName) {
            return zipFile.getEntry(fileName) != null;
        }

        @Override
        public boolean isPlatformDependent() {
            return false;
        }

        @Override
        public SourceFileDataSource getDataSource(String fileName) {
            return new ZipSourceFileDataSource(zipFile, fileName);
        }
    }

    /*
     * ----------------------------------------------------------------------
     * Fields
     * ----------------------------------------------------------------------
     */

    private List<SourceRepository> repositoryList;

    private Cache cache;

    private Project project;

    /*
     * ----------------------------------------------------------------------
     * Public methods
     * ----------------------------------------------------------------------
     */

    public SourceFinder(Project project) {
        setProject(project);
    }

    /**
     * @return Returns the project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Set the list of source directories.
     */
    void setSourceBaseList(Iterable<String> sourceBaseList) {
        for (String repos : sourceBaseList) {
            if (repos.endsWith(".zip") || repos.endsWith(".jar") || repos.endsWith(".z0p.gz")) {
                // Zip or jar archive
                try {
                    if (repos.startsWith("http:") || repos.startsWith("https:") || repos.startsWith("file:")) {
                        String url = SystemProperties.rewriteURLAccordingToProperties(repos);
                        repositoryList.add(makeInMemorySourceRepository(url));
                    } else {
                        repositoryList.add(new ZipSourceRepository(new ZipFile(repos)));
                    }
                } catch (IOException e) {
                    // Ignored - we won't use this archive
                    AnalysisContext.logError("Unable to load " + repos, e);
                }
            } else {
                File dir = new File(repos);
                if (dir.canRead() && dir.isDirectory()) {
                    repositoryList.add(new DirectorySourceRepository(repos));
                } else {
                    AnalysisContext.logError("Unable to load " + repos);

                }
            }
        }
    }

    /**
     * Open an input stream on a source file in given package.
     *
     * @param packageName
     *            the name of the package containing the class whose source file
     *            is given
     * @param fileName
     *            the unqualified name of the source file
     * @return an InputStream on the source file
     * @throws IOException
     *             if a matching source file cannot be found
     */
    public InputStream openSource(String packageName, String fileName) throws IOException {
        SourceFile sourceFile = findSourceFile(packageName, fileName);
        return sourceFile.getInputStream();
    }

    public InputStream openSource(SourceLineAnnotation source) throws IOException {
        SourceFile sourceFile = findSourceFile(source);
        return sourceFile.getInputStream();
    }

    public SourceFile findSourceFile(SourceLineAnnotation source) throws IOException {
        return findSourceFile(source.getPackageName(), getOrGuessSourceFile(source));
    }

    /**
     * Open a source file in given package.
     *
     * @param packageName
     *            the name of the package containing the class whose source file
     *            is given
     * @param fileName
     *            the unqualified name of the source file
     * @return the source file
     * @throws IOException
     *             if a matching source file cannot be found
     */
    public SourceFile findSourceFile(String packageName, String fileName) throws IOException {
        // On windows the fileName specification is different between a file in
        // a directory tree, and a
        // file in a zip file. In a directory tree the separator used is '\',
        // while in a zip it's '/'
        // Therefore for each repository figure out what kind it is and use the
        // appropriate separator.

        // In all practicality, this code could just use the hardcoded '/' char,
        // as windows can open
        // files with this separator, but to allow for the mythical 'other'
        // platform that uses an
        // alternate separator, make a distinction

        String platformName = getPlatformName(packageName, fileName);
        String canonicalName = getCanonicalName(packageName, fileName);

        // Is the file in the cache already? Always cache it with the canonical
        // name
        SourceFile sourceFile = cache.get(canonicalName);
        if (sourceFile != null) {
            return sourceFile;
        }

        // Find this source file, add its data to the cache
        if (DEBUG) {
            System.out.println("Trying " + fileName + " in package " + packageName + "...");
        }
        // Query each element of the source path to find the requested source
        // file
        for (SourceRepository repos : repositoryList) {
            if (repos instanceof BlockingSourceRepository && !((BlockingSourceRepository) repos).isReady()) {
                continue;
            }
            fileName = repos.isPlatformDependent() ? platformName : canonicalName;
            if (DEBUG) {
                System.out.println("Looking in " + repos + " for " + fileName);
            }
            if (repos.contains(fileName)) {
                // Found it
                sourceFile = new SourceFile(repos.getDataSource(fileName));
                cache.put(canonicalName, sourceFile); // always cache with
                // canonicalName
                return sourceFile;
            }
        }

        throw new FileNotFoundException("Can't find source file " + fileName);
    }

    public static String getPlatformName(String packageName, String fileName) {
        String platformName = packageName.replace('.', File.separatorChar) + (packageName.length() > 0 ? File.separator : "")
                + fileName;
        return platformName;
    }

    public static String getPlatformName(SourceLineAnnotation source) {
        return getPlatformName(source.getPackageName(), getOrGuessSourceFile(source));
    }

    public static String getCanonicalName(SourceLineAnnotation source) {
        return getCanonicalName(source.getPackageName(), getOrGuessSourceFile(source));
    }

    public static String getCanonicalName(String packageName, String fileName) {
        String canonicalName = packageName.replace('.', '/') + (packageName.length() > 0 ? "/" : "") + fileName;
        return canonicalName;
    }

    public static String getOrGuessSourceFile(SourceLineAnnotation source)  {
        if (source.isSourceFileKnown()) {
            return source.getSourceFile();
        }
        String baseClassName = source.getClassName();
        int i = baseClassName.lastIndexOf('.');
        baseClassName = baseClassName.substring(i + 1);
        int j = baseClassName.indexOf('$');
        if (j >= 0) {
            baseClassName = baseClassName.substring(0, j);
        }
        return baseClassName + ".java";
    }

    public boolean hasSourceFile(SourceLineAnnotation source) {
        return hasSourceFile(source.getPackageName(), getOrGuessSourceFile(source));
    }

    public boolean hasSourceFile(String packageName, String fileName) {
        // On windows the fileName specification is different between a file in
        // a directory tree, and a
        // file in a zip file. In a directory tree the separator used is '\',
        // while in a zip it's '/'
        // Therefore for each repository figure out what kind it is and use the
        // appropriate separator.

        // In all practicality, this code could just use the hardcoded '/' char,
        // as windows can open
        // files with this separator, but to allow for the mythical 'other'
        // platform that uses an
        // alternate separator, make a distinction

        // Create a fully qualified source filename using the package name for
        // both directories and zips
        String platformName = getPlatformName(packageName, fileName);
        String canonicalName = getCanonicalName(packageName, fileName);

        // Is the file in the cache already? Always cache it with the canonical
        // name
        SourceFile sourceFile = cache.get(canonicalName);
        if (sourceFile != null) {
            return true;
        }

        // Find this source file, add its data to the cache
        if (DEBUG) {
            System.out.println("Trying " + fileName + " in package " + packageName + "...");
        }
        // Query each element of the source path to find the requested source
        // file
        for (SourceRepository repos : repositoryList) {
            if (repos instanceof BlockingSourceRepository && !((BlockingSourceRepository) repos).isReady()) {
                continue;
            }
            fileName = repos.isPlatformDependent() ? platformName : canonicalName;
            if (DEBUG) {
                System.out.println("Looking in " + repos + " for " + fileName);
            }
            if (repos.contains(fileName)) {
                return true;
            }
        }

        return false;
    }

    private void setProject(Project project) {
        this.project = project;
        repositoryList = new LinkedList<SourceRepository>();
        cache = new Cache();
        setSourceBaseList(project.getResolvedSourcePaths());
    }
}

