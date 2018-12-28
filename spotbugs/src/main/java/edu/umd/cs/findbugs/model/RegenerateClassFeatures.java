/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.model;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.SortedBugCollection;

import static java.util.logging.Level.*;

/**
 * Repopulate a BugCollection with class features from the classes in a
 * specified jar file.
 *
 * @author David Hovemeyer
 */
public class RegenerateClassFeatures {

    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private final BugCollection bugCollection;

    private final String jarFile;

    public RegenerateClassFeatures(BugCollection bugCollection, String jarFile) {
        this.bugCollection = bugCollection;
        this.jarFile = jarFile;
    }

    public RegenerateClassFeatures execute() throws IOException {
        bugCollection.clearClassFeatures();


        ArrayList<JavaClass> classList = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(jarFile)) {

            // Add all classes to repository (for hierarchy queries)
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                ClassParser parser = new ClassParser(zipFile.getInputStream(entry), entry.getName());
                JavaClass javaClass = parser.parse();

                Repository.addClass(javaClass);
                classList.add(javaClass);
            }
        }

        for (JavaClass javaClass : classList) {
            ClassFeatureSet classFeatureSet = new ClassFeatureSet().initialize(javaClass);
            bugCollection.setClassFeatureSet(classFeatureSet);
        }

        return this;
    }

    /**
     * @return Returns the bugCollection.
     */
    public BugCollection getBugCollection() {
        return bugCollection;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            LOG.log(SEVERE, "Usage: {0} <bug collection> <jar file>", RegenerateClassFeatures.class.getName());
            System.exit(1);
        }

        SortedBugCollection bugCollection = new SortedBugCollection();

        bugCollection.readXML(args[0]);

        new RegenerateClassFeatures(bugCollection, args[1]).execute();

        bugCollection.writeXML(System.out);
    }
}
