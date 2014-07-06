/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.io.IOException;
import java.util.Date;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.cloud.Cloud;

/**
 * @author pugh
 */
public class BackdateHistoryUsingSource {

    private static final String USAGE = "Usage: <cmd> " + "  <bugs.xml> [<out.xml>]";

    public static void main(String[] args) throws IOException, DocumentException {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance();
        if (args.length < 1 || args.length > 2) {
            System.out.println(USAGE);
            return;
        }

        BugCollection origCollection;
        origCollection = new SortedBugCollection();
        origCollection.readXML(args[0]);
        SourceFinder sourceFinder = new SourceFinder(origCollection.getProject());

        for (BugInstance b : origCollection) {
            SourceLineAnnotation s = b.getPrimarySourceLineAnnotation();
            if (!s.isSourceFileKnown()) {
                continue;
            }
            if (!sourceFinder.hasSourceFile(s)) {
                continue;
            }
            SourceFile sourceFile = sourceFinder.findSourceFile(s);
            long when = sourceFile.getLastModified();
            if (when > 0) {
                Date firstSeen = new Date(when);
                b.getXmlProps().setFirstSeen(firstSeen);
                System.out.println("Set first seen to " + firstSeen);
            }
        }
        Cloud cloud = origCollection.getCloud();
        cloud.bugsPopulated();
        if (cloud.getSigninState() != Cloud.SigninState.SIGNED_IN
                && cloud.getSigninState() != Cloud.SigninState.NO_SIGNIN_REQUIRED) {
            cloud.signIn();
            if (cloud.getSigninState() != Cloud.SigninState.SIGNED_IN
                    && cloud.getSigninState() != Cloud.SigninState.NO_SIGNIN_REQUIRED) {
                throw new IllegalStateException("Unable to sign in; state : " + cloud.getSigninState());
            }
        }

        cloud.waitUntilIssueDataDownloaded();

        if (args.length > 1) {
            origCollection.writeXML(args[1]);
        }
        cloud.waitUntilNewIssuesUploaded();
        cloud.shutdown();

    }
}
