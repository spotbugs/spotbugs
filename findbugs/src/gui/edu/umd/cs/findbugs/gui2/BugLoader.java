/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import static java.util.Objects.requireNonNull;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import edu.umd.cs.findbugs.workflow.Update;

/**
 * Everything having to do with loading bugs should end up here.
 *
 * @author Dan
 *
 */
public class BugLoader {

    private static UserPreferences preferencesSingleton = UserPreferences.createDefaultUserPreferences();

    /**
     * Get UserPreferences singleton. This should only be used if there is a
     * single set of user preferences to be used for all projects.
     *
     * @return the UserPreferences
     */
    static UserPreferences getUserPreferences() {
        return preferencesSingleton;
    }

    /**
     * Performs an analysis and returns the BugSet created
     *
     * @param p
     *            The Project to run the analysis on
     * @param progressCallback
     *            the progressCallBack is supposed to be supplied by analyzing
     *            dialog, FindBugs supplies progress information while it runs
     *            the analysis
     * @return the bugs found
     * @throws InterruptedException
     * @throws IOException
     */
    public static BugCollection doAnalysis(@Nonnull Project p, FindBugsProgress progressCallback) throws IOException,
    InterruptedException {
        StringWriter stringWriter = new StringWriter();
        BugCollectionBugReporter pcb = new BugCollectionBugReporter(p, new PrintWriter(stringWriter, true));
        pcb.setPriorityThreshold(Priorities.LOW_PRIORITY);
        IFindBugsEngine fb = createEngine(p, pcb);
        fb.setUserPreferences(getUserPreferences());
        fb.setProgressCallback(progressCallback);
        fb.setProjectName(p.getProjectName());

        fb.execute();
        String warnings = stringWriter.toString();
        if (warnings.length() > 0) {
            JTextArea tp = new JTextArea(warnings);
            tp.setEditable(false);
            JScrollPane pane = new JScrollPane(tp);
            pane.setPreferredSize(new Dimension(600, 400));
            JOptionPane.showMessageDialog(MainFrame.getInstance(),
                    pane, "Analysis errors",
                    JOptionPane.WARNING_MESSAGE);
        }

        return pcb.getBugCollection();
    }

    /**
     * Create the IFindBugsEngine that will be used to analyze the application.
     *
     * @param p
     *            the Project
     * @param pcb
     *            the PrintCallBack
     * @return the IFindBugsEngine
     */
    private static IFindBugsEngine createEngine(@Nonnull Project p, BugReporter pcb) {
        FindBugs2 engine = new FindBugs2();
        engine.setBugReporter(pcb);
        engine.setProject(p);

        engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());

        //
        // Honor -effort option if one was given on the command line.
        //
        engine.setAnalysisFeatureSettings(Driver.getAnalysisSettingList());

        return engine;
    }

    public static @CheckForNull
    SortedBugCollection loadBugs(MainFrame mainFrame, Project project, File source) {
        if (!source.isFile() || !source.canRead()) {
            JOptionPane.showMessageDialog(mainFrame, "Unable to read " + source);
            return null;
        }
        SortedBugCollection col = new SortedBugCollection(project);
        try {
            col.readXML(source);
            initiateCommunication(col);
            if (col.hasDeadBugs()) {
                addDeadBugMatcher(col);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Could not read " + source + "; " + e.getMessage());
        }
        MainFrame.getInstance().setProjectAndBugCollectionInSwingThread(project, col);
        return col;
    }

    private static void initiateCommunication(SortedBugCollection col) {
        Cloud cloud = col.getCloud();
        cloud.initiateCommunication();
    }

    public static @CheckForNull
    SortedBugCollection loadBugs(MainFrame mainFrame, Project project, URL url) {

        SortedBugCollection col = new SortedBugCollection(project);
        try {
            if (MainFrame.GUI2_DEBUG) {
                System.out.println("loading from: " + url);
                JOptionPane.showMessageDialog(mainFrame, "loading from: " + url);

            }
            col.readXML(url);
            if (MainFrame.GUI2_DEBUG) {
                System.out.println("finished reading: " + url);
                JOptionPane.showMessageDialog(mainFrame, "loaded: " + url);
            }
            initiateCommunication(col);
            addDeadBugMatcher(col);

        } catch (Throwable e) {
            String msg = SystemProperties.getOSDependentProperty("findbugs.unableToLoadViaURL");
            if (msg == null) {
                msg = e.getMessage();
            } else {
                try {
                    msg = String.format(msg, url);
                } catch (Exception e2) {
                    msg = e.getMessage();
                }
            }
            JOptionPane.showMessageDialog(mainFrame, "Could not read " + url + "\n" + msg);
            if (SystemProperties.getBoolean("findbugs.failIfUnableToLoadViaURL")) {
                System.exit(1);
            }
        }
        MainFrame.getInstance().setProjectAndBugCollectionInSwingThread(project, col);
        return col;
    }

    static void addDeadBugMatcher(BugCollection bugCollection) {
        if (bugCollection == null  || !bugCollection.hasDeadBugs()) {
            return;
        }

        Filter suppressionMatcher = bugCollection.getProject().getSuppressionFilter();
        suppressionMatcher.softAdd(LastVersionMatcher.DEAD_BUG_MATCHER);
    }

    public static @CheckForNull
    Project loadProject(MainFrame mainFrame, File f) {
        try {
            Project project = Project.readXML(f);
            project.setGuiCallback(mainFrame.getGuiCallback());
            return project;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame, "Could not read " + f + "; " + e.getMessage());
        } catch (SAXException e) {
            JOptionPane.showMessageDialog(mainFrame, "Could not read  project from " + f + "; " + e.getMessage());
        }
        return null;
    }

    private BugLoader() {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO: This really needs to be rewritten such that they don't have to
     * choose ALL xmls in one fel swoop. I'm thinking something more like new
     * project wizard's functionality. -Dan
     *
     * Merges bug collection histories from xmls selected by the user. Right now
     * all xmls must be in the same folder and he must select all of them at
     * once Makes use of FindBugs's mergeCollection method in the Update class
     * of the workflow package
     *
     * @return the merged collecction of bugs
     */
    public static BugCollection combineBugHistories() {
        try {
            FBFileChooser chooser = new FBFileChooser();
            chooser.setFileFilter(new FindBugsAnalysisFileFilter());
            // chooser.setCurrentDirectory(GUISaveState.getInstance().getStarterDirectoryForLoadBugs());
            // This is done by FBFileChooser.
            chooser.setMultiSelectionEnabled(true);
            chooser.setDialogTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.choose_xmls_ttl", "Choose All XML's To Combine"));
            if (chooser.showOpenDialog(MainFrame.getInstance()) == JFileChooser.CANCEL_OPTION) {
                return null;
            }

            SortedBugCollection conglomeration = new SortedBugCollection();
            conglomeration.readXML(chooser.getSelectedFiles()[0]);
            Update update = new Update();
            for (int x = 1; x < chooser.getSelectedFiles().length; x++) {
                File f = chooser.getSelectedFiles()[x];
                SortedBugCollection col = new SortedBugCollection();
                col.readXML(f);
                conglomeration = (SortedBugCollection) update.mergeCollections(conglomeration, col, false, false);// False
                // means
                // dont
                // show
                // dead
                // bugs
            }

            return conglomeration;
        } catch (IOException e) {
            Debug.println(e);
            return null;
        } catch (DocumentException e) {
            Debug.println(e);
            return null;
        }

    }

    /**
     * Does what it says it does, hit apple r (control r on pc) and the analysis
     * is redone using the current project
     *
     * @param p
     * @return the bugs from the reanalysis, or null if cancelled
     */
    public static @CheckForNull
    BugCollection doAnalysis(@Nonnull Project p) {
        requireNonNull(p, "null project");

        RedoAnalysisCallback ac = new RedoAnalysisCallback();

        AnalyzingDialog.show(p, ac, true);

        if (ac.finished) {
            return ac.getBugCollection();
        } else {
            return null;
        }

    }

    /**
     * Does what it says it does, hit apple r (control r on pc) and the analysis
     * is redone using the current project
     *
     * @param p
     * @return the bugs from the reanalysis, or null if canceled
     */
    public static @CheckForNull
    BugCollection redoAnalysisKeepComments(@Nonnull Project p) {
        requireNonNull(p, "null project");

        BugCollection current = MainFrame.getInstance().getBugCollection();

        Update update = new Update();

        RedoAnalysisCallback ac = new RedoAnalysisCallback();

        AnalyzingDialog.show(p, ac, true);

        if (!ac.finished) {
            return null;
        }
        if (current == null) {
            current =  ac.getBugCollection();
        } else {
            current =  update.mergeCollections(current, ac.getBugCollection(), true, false);
            if (current.hasDeadBugs()) {
                addDeadBugMatcher(current);
            }
        }
        return current;


    }

    /** just used to know how the new analysis went */
    private static class RedoAnalysisCallback implements AnalysisCallback {

        BugCollection getBugCollection() {
            return justAnalyzed;
        }

        BugCollection justAnalyzed;

        volatile boolean finished;

        @Override
        public void analysisFinished(BugCollection b) {
            justAnalyzed = b;
            finished = true;
        }

        @Override
        public void analysisInterrupted() {
            finished = false;
        }
    }
}
