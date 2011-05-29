/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class FilterFilesTab extends Composite {

    private final FindbugsPropertyPage propertyPage;

    private final FilterProvider filterIncl;

    private final FilterProvider filterExcl;

    private final FilterProvider filterExclBugs;

    static final class SelectionValidator {
        private final UserPreferences prefs;

        private final Map<String, Boolean> exclFiles;

        public SelectionValidator(FilterKind kind, FindbugsPropertyPage propertyPage) {
            prefs = propertyPage.getCurrentUserPreferences();
            exclFiles = kind.excludedPaths(prefs);
        }

        public IStatus validate(String path) {
            if (exclFiles.containsKey(path)) {
                return FindbugsPlugin.createErrorStatus("Filter selected in a conflicting list", null);
            }
            return Status.OK_STATUS;
        }
    }

    public static class FilterProvider extends PathsProvider {

        private final FilterKind kind;

        protected FilterProvider(TableViewer viewer, FilterKind kind, FindbugsPropertyPage propertyPage) {
            super(viewer, propertyPage);
            this.kind = kind;
            setFilters(propertyPage.getCurrentUserPreferences());
        }

        List<IPathElement> getFilterFiles(UserPreferences prefs) {
            IProject project = propertyPage.getProject();
            final List<IPathElement> newPaths = new ArrayList<IPathElement>();
            Map<String, Boolean> filterPaths = kind.selectedPaths(prefs);
            if (filterPaths != null) {
                for (Entry<String, Boolean> entry : filterPaths.entrySet()) {
                    IPath filterPath = FindBugsWorker.getFilterPath(entry.getKey(), project);
                    PathElement element = new PathElement(filterPath, Status.OK_STATUS);
                    element.setEnabled(entry.getValue().booleanValue());
                    newPaths.add(element);
                }
            }
            return newPaths;
        }

        @Override
        protected void applyToPreferences() {
            super.applyToPreferences();
            kind.setPaths(propertyPage.getCurrentUserPreferences(), pathsToStrings());
        }

        void setFilters(UserPreferences prefs) {
            setFilters(getFilterFiles(prefs));
        }

        @Override
        protected IStatus validate() {
            SelectionValidator validator = new SelectionValidator(kind, propertyPage);
            IStatus bad = null;
            IProject project = propertyPage.getProject();
            for (IPathElement path : paths) {
                String filterPath = FindBugsWorker.toFilterPath(path.getPath(), project).toOSString();
                IStatus status = validator.validate(filterPath);
                path.setStatus(status);
                if (!status.isOK()) {
                    bad = status;
                }
            }
            return bad;
        }

        @Override
        protected void configureDialog(FileDialog dialog) {
            dialog.setFilterExtensions(new String[] { "*.xml" });
            dialog.setText(FindbugsPlugin.getDefault().getMessage(kind.propertyName) + ": select xml file(s) containing filters");
        }
    }

    public FilterFilesTab(TabFolder parent, FindbugsPropertyPage page, int style) {
        super(parent, style);
        this.propertyPage = page;
        setLayout(new GridLayout(2, true));

        Link label = new Link(this, SWT.NONE);
        label.setText("Filter files may be used to include or exclude bug detection for particular classes and methods.\n"
                + "<a href=\"http://findbugs.sourceforge.net/manual/filter.html\">Details...</a>\n");

        label.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                Program.launch(e.text);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                // noop
            }
        });
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));

        TabItem tabDetector = new TabItem(parent, SWT.NONE);
        tabDetector.setText(getMessage("property.filterFilesTab"));
        tabDetector.setControl(this);
        tabDetector.setToolTipText("Configure external bug reporting filters");

        ManagePathsWidget incl = new ManagePathsWidget(this);
        CheckboxTableViewer viewer = incl.createViewer(getMessage(FilterKind.INCLUDE.propertyName), null, true);
        filterIncl = createFilterProvider(viewer, FilterKind.INCLUDE, page);
        incl.createButtonsArea(filterIncl);

        ManagePathsWidget excl = new ManagePathsWidget(this);
        viewer = excl.createViewer(getMessage(FilterKind.EXCLUDE.propertyName), null, true);
        filterExcl = createFilterProvider(viewer, FilterKind.EXCLUDE, page);
        excl.createButtonsArea(filterExcl);

        ManagePathsWidget excl2 = new ManagePathsWidget(this);
        viewer = excl2.createViewer(getMessage(FilterKind.EXCLUDE_BUGS.propertyName),
                "You can include past FindBugs result XML files here to exclude those bugs from analysis. "
                        + "<a href=\"http://findbugs.sourceforge.net/manual/filter.html\">Details...</a>", true);
        filterExclBugs = createFilterProvider(viewer, FilterKind.EXCLUDE_BUGS, page);
        excl2.createButtonsArea(filterExclBugs);

        refreshTables();
    }

    public void refreshTables() {
        propertyPage.setErrorMessage(null);
        filterIncl.refresh();
        filterExcl.refresh();
        filterExclBugs.refresh();
    }

    /**
     * Helper method to shorten message access
     *
     * @param key
     *            a message key
     * @return requested message
     */
    protected static String getMessage(String key) {
        return FindbugsPlugin.getDefault().getMessage(key);
    }

    protected FilterProvider createFilterProvider(TableViewer viewer, FilterKind kind, FindbugsPropertyPage page) {
        FilterProvider filterProvider = new FilterProvider(viewer, kind, propertyPage);
        filterProvider.addListener(new Listener() {
            public void handleEvent(Event event) {
                refreshTables();
            }
        });
        return filterProvider;
    }

    public static enum FilterKind {
        INCLUDE("property.includefilter") {
            @Override
            Map<String, Boolean> selectedPaths(UserPreferences u) {
                return u.getIncludeFilterFiles();
            }

            @Override
            Map<String, Boolean> excludedPaths(UserPreferences u) {
                Map<String, Boolean> excl = new TreeMap<String, Boolean>();
                excl.putAll(u.getExcludeFilterFiles());
                excl.putAll(u.getExcludeBugsFiles());
                return excl;
            }

            @Override
            void setPaths(UserPreferences u, Map<String, Boolean> files) {
                u.setIncludeFilterFiles(files);
            }
        },
        EXCLUDE("property.excludefilter") {
            @Override
            Map<String, Boolean> selectedPaths(UserPreferences u) {
                return u.getExcludeFilterFiles();
            }

            @Override
            Map<String, Boolean> excludedPaths(UserPreferences u) {
                Map<String, Boolean> excl = new TreeMap<String, Boolean>();
                excl.putAll(u.getIncludeFilterFiles());
                excl.putAll(u.getExcludeBugsFiles());
                return excl;
            }

            @Override
            void setPaths(UserPreferences u, Map<String, Boolean> files) {
                u.setExcludeFilterFiles(files);
            }
        },
        EXCLUDE_BUGS("property.excludebugs") {
            @Override
            Map<String, Boolean> selectedPaths(UserPreferences u) {
                return u.getExcludeBugsFiles();
            }

            @Override
            Map<String, Boolean> excludedPaths(UserPreferences u) {
                Map<String, Boolean> excl = new TreeMap<String, Boolean>();
                excl.putAll(u.getIncludeFilterFiles());
                excl.putAll(u.getExcludeFilterFiles());
                return excl;
            }

            @Override
            void setPaths(UserPreferences u, Map<String, Boolean> files) {
                u.setExcludeBugsFiles(files);
            }
        };
        final String propertyName;

        FilterKind(String propertyName) {
            this.propertyName = propertyName;
        }

        abstract Map<String, Boolean> selectedPaths(UserPreferences u);

        abstract Map<String, Boolean> excludedPaths(UserPreferences u);

        abstract void setPaths(UserPreferences u, Map<String, Boolean> files);
    }

    @Override
    public void setEnabled(boolean enabled) {
        filterExcl.setControlEnabled(enabled);
        filterIncl.setControlEnabled(enabled);
        filterExclBugs.setControlEnabled(enabled);
        super.setEnabled(enabled);
    }

    void refreshUI(UserPreferences prefs) {
        filterExcl.setFilters(prefs);
        filterExclBugs.setFilters(prefs);
        filterIncl.setFilters(prefs);
        refreshTables();
    }

    protected PathsProvider getFilterIncl() {
        return filterIncl;
    }

    protected PathsProvider getFilterExcl() {
        return filterExcl;
    }

    protected PathsProvider getFilterExclBugs() {
        return filterExclBugs;
    }
}
