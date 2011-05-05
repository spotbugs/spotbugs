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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugAnnotationWithSourceLines;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class CopyBuggySource {

    /**
     *
     */
    private static final String USAGE = "Usage: <cmd> " + "  <bugs.xml> <destinationSrcDir>";

    public static void main(String[] args) throws IOException, DocumentException {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance();
        if (args.length != 2) {
            System.out.println(USAGE);
            return;
        }

        BugCollection origCollection;
        origCollection = new SortedBugCollection();
        origCollection.readXML(args[0]);
        File src = new File(args[1]);
        byte buf[] = new byte[4096];
        if (!src.isDirectory())
            throw new IllegalArgumentException(args[1] + " is not a source directory");
        Project project = origCollection.getProject();
        SourceFinder sourceFinder = new SourceFinder(project);
        HashSet<String> copied = new HashSet<String>();
        HashSet<String> couldNotFind = new HashSet<String>();
        HashSet<String> couldNotCreate = new HashSet<String>();

        int copyCount = 0;
        for (BugInstance bug : origCollection.getCollection()) {
            for (Iterator<BugAnnotation> i = bug.annotationIterator(); i.hasNext();) {
                BugAnnotation ann = i.next();
                SourceLineAnnotation sourceAnnotation;
                if (ann instanceof BugAnnotationWithSourceLines)
                    sourceAnnotation = ((BugAnnotationWithSourceLines) ann).getSourceLines();
                else if (ann instanceof SourceLineAnnotation)
                    sourceAnnotation = (SourceLineAnnotation) ann;
                else
                    continue;
                if (sourceAnnotation == null)
                    continue;
                if (sourceAnnotation.isUnknown())
                    continue;
               
                String fullName = SourceFinder.getPlatformName(sourceAnnotation);
                
                SourceFile sourceFile;
                try {
                    sourceFile = sourceFinder.findSourceFile(sourceAnnotation);
                } catch (FileNotFoundException e) {
                    
                    if (couldNotFind.add(fullName))
                        System.out.println("Did not find " + fullName);
                    continue;
                }
                
                if (copied.add(fullName)) {
                    long lastModified = sourceFile.getLastModified();
                    File dstFile = new File(src, fullName);
                    if (dstFile.exists()) {
                        System.out.println(dstFile + " already exists");
                        continue;
                    }
                    File parent = dstFile.getParentFile();
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = sourceFile.getInputStream();
                        if (!parent.mkdirs() && !parent.isDirectory()) {
                            String path = parent.getPath();
                            if (couldNotCreate.add(path))
                                System.out.println("Can't create directory for " + path);
                            in.close();
                            continue;
                        }
                        out = new FileOutputStream(dstFile);
                        while (true) {
                            int sz = in.read(buf);
                            if (sz < 0)
                                break;
                            out.write(buf, 0, sz);
                        }
                        System.out.println("Copied " + dstFile);
                        copyCount++;
                    } catch (IOException e) {
                        if (couldNotFind.add(dstFile.getPath())) {
                            System.out.println("Problem copying " + dstFile);
                            e.printStackTrace(System.out);
                        }
                    } finally {
                        close(in);
                        close(out);
                        dstFile.setLastModified(lastModified);
                    }

                }
            }
        }

        System.out.printf("All done. %d files not found, %d files copied%n", couldNotFind.size(), copyCount);
    }

    public static void close(InputStream in) {
        try {
            if (in != null)
                in.close();
        } catch (IOException e) {
        }

    }

    public static void close(OutputStream out) {
        try {
            if (out != null)
                out.close();
        } catch (IOException e) {
        }

    }
}
