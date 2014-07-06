package edu.umd.cs.findbugs.gui2;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.HTMLBugReporter;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;

public class MainFrameLoadSaveHelper implements Serializable {
    private final MainFrame mainFrame;

    private FBFileChooser saveOpenFileChooser;

    private FBFileChooser filterOpenFileChooser;

    public MainFrameLoadSaveHelper(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void initialize() {
        saveOpenFileChooser = new FBFileChooser();
        saveOpenFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        saveOpenFileChooser.setAcceptAllFileFilterUsed(false);
        saveOpenFileChooser.addChoosableFileFilter(FindBugsAnalysisFileFilter.INSTANCE);
        saveOpenFileChooser.addChoosableFileFilter(FindBugsFBPFileFilter.INSTANCE);
        saveOpenFileChooser.addChoosableFileFilter(FindBugsFBAFileFilter.INSTANCE);
        saveOpenFileChooser.setFileFilter(FindBugsAnalysisFileFilter.INSTANCE);
        saveOpenFileChooser.addChoosableFileFilter(FindBugsHtmlFileFilter.INSTANCE);
        filterOpenFileChooser = new FBFileChooser();
        filterOpenFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        filterOpenFileChooser.setFileFilter(FindBugsFilterFileFilter.INSTANCE);
    }

    /**
     * This method is for when the user wants to open a file.
     */
    void importFilter() {
        filterOpenFileChooser.setDialogTitle(L10N.getLocalString("dlg.importFilter_ttl", "Import and merge filter..."));

        boolean retry = true;
        File f;
        while (retry) {
            retry = false;

            int value = filterOpenFileChooser.showOpenDialog(mainFrame);

            if (value != JFileChooser.APPROVE_OPTION) {
                return;
            }

            f = filterOpenFileChooser.getSelectedFile();

            if (!f.exists()) {
                JOptionPane.showMessageDialog(filterOpenFileChooser, "No such file", "Invalid File", JOptionPane.WARNING_MESSAGE);
                retry = true;
                continue;
            }
            Filter filter;
            try {
                filter = Filter.parseFilter(f.getPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(filterOpenFileChooser, "Could not load filter.");
                retry = true;
                continue;
            }
            mainFrame.setProjectChanged(true);
            Filter suppressionFilter = mainFrame.getProject().getSuppressionFilter();

            for (Matcher m : filter.getChildren()) {
                suppressionFilter.addChild(m);
            }

            PreferencesFrame.getInstance().updateFilterPanel();
        }

    }

    /**
     * This method is for when the user wants to open a file.
     */
    void open() {
        if (!mainFrame.canNavigateAway()) {
            return;
        }

        if (askToSave()) {
            return;
        }

        boolean loading = true;
        SaveType fileType;
        tryAgain: while (loading) {
            int value = saveOpenFileChooser.showOpenDialog(mainFrame);
            if (value != JFileChooser.APPROVE_OPTION) {
                return;
            }

            loading = false;
            fileType = convertFilterToType(saveOpenFileChooser.getFileFilter());
            final File f = saveOpenFileChooser.getSelectedFile();

            if (!fileType.isValid(f)) {
                JOptionPane.showMessageDialog(saveOpenFileChooser, "That file is not compatible with the choosen file type",
                        "Invalid File", JOptionPane.WARNING_MESSAGE);
                loading = true;
                continue;
            }

            switch (fileType) {
            case XML_ANALYSIS:
                if (!f.getName().endsWith(".xml")) {
                    JOptionPane.showMessageDialog(saveOpenFileChooser,
                            L10N.getLocalString("dlg.not_xml_data_lbl", "This is not a saved bug XML data file."));
                    loading = true;
                    continue tryAgain;
                }

                if (!mainFrame.openAnalysis(f, fileType)) {
                    // TODO: Deal if something happens when loading analysis
                    JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
                    loading = true;
                    continue tryAgain;
                }
                break;
            case FBP_FILE:
                if (!openFBPFile(f)) {
                    // TODO: Deal if something happens when loading analysis
                    JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
                    loading = true;
                    continue tryAgain;
                }
                break;
            case FBA_FILE:
                if (!openFBAFile(f)) {
                    // TODO: Deal if something happens when loading analysis
                    JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
                    loading = true;
                    continue tryAgain;
                }
                break;
            default:
                assert false;
            }
        }
    }

    /** Returns true if cancelled */
    private boolean askToSave() {
        if (mainFrame.isProjectChanged()) {
            int response = JOptionPane.showConfirmDialog(mainFrame, L10N.getLocalString("dlg.save_current_changes",
                    "The current project has been changed, Save current changes?"), L10N.getLocalString("dlg.save_changes",
                            "Save Changes?"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (response == JOptionPane.YES_OPTION) {
                if (mainFrame.getSaveFile() != null) {
                    save();
                } else {
                    saveAs();
                }
            } else if (response == JOptionPane.CANCEL_OPTION)
            {
                return true;
                // IF no, do nothing.
            }
        }
        return false;
    }

    boolean openFBAFile(File f) {
        return mainFrame.openAnalysis(f, SaveType.FBA_FILE);
    }

    boolean openFBPFile(File f) {
        if (!f.exists() || !f.canRead()) {
            return false;
        }

        prepareForFileLoad(f, SaveType.FBP_FILE);

        loadProjectFromFile(f);

        return true;
    }

    boolean exportFilter() {
        filterOpenFileChooser.setDialogTitle(L10N.getLocalString("dlg.exportFilter_ttl", "Export filter..."));

        boolean retry = true;
        boolean alreadyExists;
        File f;
        while (retry) {
            retry = false;

            int value = filterOpenFileChooser.showSaveDialog(mainFrame);

            if (value != JFileChooser.APPROVE_OPTION) {
                return false;
            }

            f = filterOpenFileChooser.getSelectedFile();

            alreadyExists = f.exists();
            if (alreadyExists) {
                int response = JOptionPane.showConfirmDialog(filterOpenFileChooser,
                        L10N.getLocalString("dlg.file_exists_lbl", "This file already exists.\nReplace it?"),
                        L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (response == JOptionPane.OK_OPTION) {
                    retry = false;
                }
                if (response == JOptionPane.CANCEL_OPTION) {
                    retry = true;
                    continue;
                }

            }

            Filter suppressionFilter = mainFrame.getProject().getSuppressionFilter();
            try {
                suppressionFilter.writeEnabledMatchersAsXML(new FileOutputStream(f));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainFrame,
                        L10N.getLocalString("dlg.saving_error_lbl", "An error occurred in saving."));
                return false;
            }
        }

        return true;
    }

    boolean saveAs() {
        if (!mainFrame.canNavigateAway()) {
            return false;
        }

        saveOpenFileChooser.setDialogTitle(L10N.getLocalString("dlg.saveas_ttl", "Save as..."));

        if (mainFrame.getCurrentProject() == null) {
            JOptionPane.showMessageDialog(mainFrame, L10N.getLocalString("dlg.no_proj_save_lbl", "There is no project to save"));
            return false;
        }

        boolean retry = true;
        SaveType fileType = SaveType.NOT_KNOWN;
        boolean alreadyExists;
        File f = null;
        while (retry) {
            retry = false;

            int value = saveOpenFileChooser.showSaveDialog(mainFrame);

            if (value != JFileChooser.APPROVE_OPTION) {
                return false;
            }

            fileType = convertFilterToType(saveOpenFileChooser.getFileFilter());
            if (fileType == SaveType.NOT_KNOWN) {
                Debug.println("Error! fileType == SaveType.NOT_KNOWN");
                // This should never happen b/c saveOpenFileChooser can only
                // display filters
                // given it.
                retry = true;
                continue;
            }

            f = saveOpenFileChooser.getSelectedFile();

            f = convertFile(f, fileType);

            if (!fileType.isValid(f)) {
                JOptionPane.showMessageDialog(saveOpenFileChooser, "That file is not compatible with the chosen file type",
                        "Invalid File", JOptionPane.WARNING_MESSAGE);
                retry = true;
                continue;
            }

            alreadyExists = fileAlreadyExists(f);
            if (alreadyExists) {
                int response = -1;

                switch (fileType) {
                case HTML_OUTPUT:
                    response = JOptionPane.showConfirmDialog(saveOpenFileChooser,
                            L10N.getLocalString("dlg.analysis_exists_lbl", "This html output already exists.\nReplace it?"),
                            L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    break;case XML_ANALYSIS:
                        response = JOptionPane.showConfirmDialog(saveOpenFileChooser,
                                L10N.getLocalString("dlg.analysis_exists_lbl", "This analysis already exists.\nReplace it?"),
                                L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        break;
                    case FBP_FILE:
                        response = JOptionPane.showConfirmDialog(saveOpenFileChooser,
                                L10N.getLocalString("FB Project File already exists",
                                        "This FB project file already exists.\nDo you want to replace it?"), L10N.getLocalString(
                                                "dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                        break;
                    case FBA_FILE:
                        response = JOptionPane.showConfirmDialog(saveOpenFileChooser, L10N.getLocalString(
                                "FB Analysis File already exists",
                                "This FB analysis file already exists.\nDo you want to replace it?"), L10N.getLocalString(
                                        "dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                        break;
                    default:
                        assert false;
                }

                if (response == JOptionPane.OK_OPTION) {
                    retry = false;
                }
                if (response == JOptionPane.CANCEL_OPTION) {
                    retry = true;
                    continue;
                }

            }

            SaveReturn successful = SaveReturn.SAVE_ERROR;

            switch (fileType) {

            case HTML_OUTPUT:
                successful = printHtml(f);
                break;
            case XML_ANALYSIS:
                successful = saveAnalysis(f);
                break;
            case FBA_FILE:
                successful = saveFBAFile(f);
                break;
            case FBP_FILE:
                successful = saveFBPFile(f);
                break;
            default:
                JOptionPane.showMessageDialog(mainFrame, "Unknown save file type");
                return false;
            }

            if (successful != SaveReturn.SAVE_SUCCESSFUL) {
                JOptionPane.showMessageDialog(mainFrame,
                        L10N.getLocalString("dlg.saving_error_lbl", "An error occurred in saving."));
                return false;
            }
        }
        assert f != null;

        // saveProjectMenuItem.setEnabled(false);
        mainFrame.getSaveMenuItem().setEnabled(false);
        mainFrame.setSaveType(fileType);
        mainFrame.setSaveFile(f);
        File xmlFile = f;

        mainFrame.addFileToRecent(xmlFile);

        return true;
    }

    SaveType convertFilterToType(FileFilter f) {
        if (f instanceof FindBugsFileFilter) {
            return ((FindBugsFileFilter) f).getSaveType();
        }

        return SaveType.NOT_KNOWN;
    }

    boolean fileAlreadyExists(File f) {

        return f.exists();
    }

    File convertFile(File f, SaveType fileType) {
        // Checks that it has the correct file extension, makes a new file if it
        // doesn't.
        if (!f.getName().endsWith(fileType.getFileExtension())) {
            f = new File(f.getAbsolutePath() + fileType.getFileExtension());
        }

        return f;
    }

    void save() {
        if (!mainFrame.canNavigateAway()) {
            return;
        }
        File sFile = mainFrame.getSaveFile();
        assert sFile != null;

        SaveReturn result = SaveReturn.SAVE_ERROR;

        switch (mainFrame.getSaveType()) {

        case XML_ANALYSIS:
            result = saveAnalysis(sFile);
            break;
        case FBA_FILE:
            result = saveFBAFile(sFile);
            break;
        case FBP_FILE:
            result = saveFBPFile(sFile);
            break;
        default:
            JOptionPane.showMessageDialog(mainFrame, "Unknown save file type");
            return;

        }

        if (result != SaveReturn.SAVE_SUCCESSFUL) {
            JOptionPane.showMessageDialog(mainFrame, L10N.getLocalString("dlg.saving_error_lbl", "An error occurred in saving."));
        }
    }

    SaveReturn saveFBAFile(File saveFile2) {
        return saveAnalysis(saveFile2);
    }

    SaveReturn saveFBPFile(File saveFile2) {
        if (!mainFrame.canNavigateAway()) {
            return SaveReturn.SAVE_ERROR;
        }
        try {
            mainFrame.getProject().writeXML(saveFile2, mainFrame.getBugCollection());
        } catch (IOException e) {
            AnalysisContext.logError("Couldn't save FBP file to " + saveFile2, e);
            return SaveReturn.SAVE_IO_EXCEPTION;
        }

        mainFrame.setProjectChanged(false);

        return SaveReturn.SAVE_SUCCESSFUL;
    }

    SaveReturn printHtml(final File f) {

        Future<Object> waiter = mainFrame.getBackgroundExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                HTMLBugReporter reporter = new HTMLBugReporter( mainFrame.getProject(), "default.xsl");
                reporter.setIsRelaxed(true);
                reporter.setOutputStream(UTF8.printStream(new FileOutputStream(f)));
                for(BugInstance bug : mainFrame.getBugCollection().getCollection()) {
                    try {
                        if (mainFrame.getViewFilter().show(bug)) {
                            reporter.reportBug(bug);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                reporter.finish();
                return null;
            }
        });
        try {
            waiter.get();
        } catch (InterruptedException e) {
            return SaveReturn.SAVE_ERROR;
        } catch (ExecutionException e) {
            return SaveReturn.SAVE_ERROR;
        }

        return SaveReturn.SAVE_SUCCESSFUL;
    }

    /**
     * Save current analysis as file passed in. Return SAVE_SUCCESSFUL if save
     * successful. Method doesn't do much. This method is more if need to do
     * other things in the future for saving analysis. And to keep saving naming
     * convention.
     */
    SaveReturn saveAnalysis(final File f) {

        Future<Object> waiter = mainFrame.getBackgroundExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                BugSaver.saveBugs(f, mainFrame.getBugCollection(), mainFrame.getProject());
                return null;
            }
        });
        try {
            waiter.get();
        } catch (InterruptedException e) {
            return SaveReturn.SAVE_ERROR;
        } catch (ExecutionException e) {
            return SaveReturn.SAVE_ERROR;
        }

        mainFrame.setProjectChanged(false);

        return SaveReturn.SAVE_SUCCESSFUL;
    }

    void prepareForFileLoad(File f, SaveType saveType) {
        closeProjectInternal();

        mainFrame.getReconfigMenuItem().setEnabled(true);
        mainFrame.setSaveType(saveType);
        mainFrame.setSaveFile(f);

        mainFrame.addFileToRecent(f);
    }

    void closeProject() {
        if (askToSave()) {
            return;
        }

        closeProjectInternal();
    }

    private void closeProjectInternal() {
        // This creates a new filters and suppressions so don't use the
        // previoues one.
        mainFrame.createProjectSettings();

        mainFrame.clearSourcePane();
        mainFrame.clearSummaryTab();
        mainFrame.getComments().refresh();
        mainFrame.setProjectChanged(false);
    }

    void loadAnalysis(final File file) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mainFrame.acquireDisplayWait();
                try {
                    Project project = new Project();
                    project.setGuiCallback(mainFrame.getGuiCallback());
                    project.setCurrentWorkingDirectory(file.getParentFile());
                    BugLoader.loadBugs(mainFrame, project, file);
                    project.getSourceFinder(); // force source finder to be
                    // initialized
                    mainFrame.updateBugTree();
                } finally {
                    mainFrame.releaseDisplayWait();
                }
            }
        };
        if (EventQueue.isDispatchThread()) {
            new Thread(runnable, "Analysis loading thread").start();
        } else {
            runnable.run();
        }
    }

    void loadAnalysis(final URL url) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mainFrame.acquireDisplayWait();
                try {
                    Project project = new Project();
                    project.setGuiCallback(mainFrame.getGuiCallback());
                    BugLoader.loadBugs(mainFrame, project, url);
                    project.getSourceFinder(); // force source finder to be
                    // initialized
                    mainFrame.updateBugTree();
                } finally {
                    mainFrame.releaseDisplayWait();
                }
            }
        };
        if (EventQueue.isDispatchThread()) {
            new Thread(runnable, "Analysis loading thread").start();
        } else {
            runnable.run();
        }
    }

    void loadProjectFromFile(final File f) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Project project = BugLoader.loadProject(mainFrame, f);
                final BugCollection bc = project == null ? null : BugLoader.doAnalysis(project);
                mainFrame.updateProjectAndBugCollection(bc);
                mainFrame.setProjectAndBugCollectionInSwingThread(project, bc);
            }
        };
        if (EventQueue.isDispatchThread()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }

    void mergeAnalysis() {
        if (!mainFrame.canNavigateAway()) {
            return;
        }

        mainFrame.acquireDisplayWait();
        try {
            BugCollection bc = BugLoader.combineBugHistories();
            mainFrame.setBugCollection(bc);
        } finally {
            mainFrame.releaseDisplayWait();
        }

    }

    enum SaveReturn {
        SAVE_SUCCESSFUL, SAVE_IO_EXCEPTION, SAVE_ERROR
    }
}
