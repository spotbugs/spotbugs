/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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
package edu.umd.cs.findbugs.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugAnnotationWithSourceLines;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 *
 * @author William Pugh
 */

public class CopyBuggySource {
    enum SrcKind {
        DIR, ZIP, Z0P_GZ;
        static SrcKind get(File f) {
            if (f.exists() && f.isDirectory() && f.canWrite()) {
                return DIR;
            }
            if (!f.exists()) {
                if (f.getName().endsWith(".zip")) {
                    return ZIP;
                }
                if (f.getName().endsWith(".z0p.gz")) {
                    return Z0P_GZ;
                }
            }
            throw new IllegalArgumentException("Invalid src destination: " + f);
        }

    }

    /**
     *
     */
    private static final String USAGE = "Usage: <cmd> " + "  <bugs.xml> <destinationSrc>";


    public static void main(String[] args) throws Exception {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance();
        if (args.length != 2) {
            System.out.println(USAGE);
            return;
        }
        new CopyBuggySource(args).execute();
    }

    SortedBugCollection origCollection;
    File src;
    SrcKind kind;
    ZipOutputStream zOut;
    byte buf[] = new byte[4096];
    Project project;
    SourceFinder sourceFinder;
    HashSet<String> copied = new HashSet<String>();
    HashSet<String> couldNotFind = new HashSet<String>();
    HashSet<String> couldNotCreate = new HashSet<String>();
    int copyCount = 0;
    File dstFile;

    public CopyBuggySource(String[] args) throws Exception {
        origCollection = new SortedBugCollection();
        origCollection.readXML(args[0]);
        project = origCollection.getProject();

        sourceFinder = new SourceFinder(project);
        src = new File(args[1]);
        kind = SrcKind.get(src);

        switch (kind) {
        case DIR:
            break;
        case ZIP:
            zOut = new ZipOutputStream(new FileOutputStream(src));
            break;
        case Z0P_GZ:
            zOut = new ZipOutputStream(new DeflaterOutputStream(new FileOutputStream(src)));
            zOut.setLevel(0);
            break;
        }

    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void copySourceFile(String fullName, SourceFile sourceFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            long lastModified = sourceFile.getLastModified();
            in = sourceFile.getInputStream();
            out = getOutputStream(fullName, 0);
            if (out == null) {
                return;
            }
            while (true) {
                int sz = in.read(buf);
                if (sz < 0) {
                    break;
                }
                out.write(buf, 0, sz);
            }
            if (dstFile != null) {
                dstFile.setLastModified(lastModified);
            }
            System.out.println("Copied " + fullName);
            copyCount++;
        } catch (IOException e) {
            if (couldNotFind.add(fullName)) {
                System.out.println("Problem copying " + fullName);
                e.printStackTrace(System.out);
            }
        } finally {
            close(in);
            close(out);
        }
    }

    private void copySourceForAnnotation(BugAnnotation ann) {
        SourceLineAnnotation sourceAnnotation;
        if (ann instanceof BugAnnotationWithSourceLines) {
            sourceAnnotation = ((BugAnnotationWithSourceLines) ann).getSourceLines();
        } else if (ann instanceof SourceLineAnnotation) {
            sourceAnnotation = (SourceLineAnnotation) ann;
        } else {
            return;
        }
        if (sourceAnnotation == null) {
            return;
        }
        if (sourceAnnotation.isUnknown()) {
            return;
        }

        String fullName = SourceFinder.getPlatformName(sourceAnnotation);

        SourceFile sourceFile;
        try {
            sourceFile = sourceFinder.findSourceFile(sourceAnnotation);
        } catch (IOException e) {

            if (couldNotFind.add(fullName)) {
                System.out.println("Did not find " + fullName);
            }
            return;
        }

        if (copied.add(fullName)) {
            copySourceFile(fullName, sourceFile);
        }
    }

    public void execute() throws IOException {

        for (BugInstance bug : origCollection.getCollection()) {
            for (Iterator<BugAnnotation> i = bug.annotationIterator(); i.hasNext();) {
                copySourceForAnnotation(i.next());
            }
        }
        if (zOut != null) {
            zOut.close();
        }

        System.out.printf("All done. %d files not found, %d files copied%n", couldNotFind.size(), copyCount);
    }

    private @CheckForNull
    OutputStream getOutputStream(String fullName, long lastModifiedTime) throws IOException {
        if (kind == SrcKind.DIR) {
            dstFile = new File(src, fullName);

            if (dstFile.exists()) {
                System.out.println(dstFile + " already exists");
                return null;
            }

            File parent = dstFile.getParentFile();
            OutputStream out = null;

            if (!parent.mkdirs() && !parent.isDirectory()) {
                String path = parent.getPath();
                if (couldNotCreate.add(path)) {
                    System.out.println("Can't create directory for " + path);
                }
                return null;
            }

            return new FileOutputStream(dstFile);
        } else {
            ZipEntry e = new ZipEntry(fullName);
            e.setTime(lastModifiedTime);
            zOut.putNextEntry(e);
            return zOut;
        }

    }

    public static void close(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
        }

    }

    public static void close(OutputStream out) {
        try {
            if (out instanceof ZipOutputStream) {
                ((ZipOutputStream) out).closeEntry();
            } else if (out != null) {
                out.close();
            }
        } catch (IOException e) {
        }

    }

}
