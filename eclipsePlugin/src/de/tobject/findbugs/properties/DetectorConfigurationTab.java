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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author Andrei Loskutov
 */
public class DetectorConfigurationTab extends Composite {

    private enum COLUMN {
        BUG_CODES, BUG_CATEGORIES, DETECTOR_NAME, DETECTOR_SPEED, PLUGIN, UNKNOWN
    }

    private static final class BugPatternTableSorter extends ViewerSorter implements Comparator<DetectorFactory> {
        private COLUMN sortColumnId;

        private COLUMN lastSortColumnId;

        boolean revertOrder;

        private final DetectorConfigurationTab tab;

        BugPatternTableSorter(DetectorConfigurationTab tab) {
            this.tab = tab;
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            return compare((DetectorFactory) e1, (DetectorFactory) e2);
        }

        public int compare(DetectorFactory factory1, DetectorFactory factory2) {
            int result = 0;
            String s1, s2;
            switch (getSortColumnId()) {
            case BUG_CODES:
                s1 = tab.getBugsAbbreviation(factory1);
                s2 = tab.getBugsAbbreviation(factory2);
                break;
            case DETECTOR_SPEED:
                s1 = factory1.getSpeed();
                s2 = factory2.getSpeed();
                break;
            case PLUGIN:
                s1 = factory1.getPlugin().getPluginId();
                s2 = factory2.getPlugin().getPluginId();
                break;
            case BUG_CATEGORIES:
                s1 = tab.getBugsCategories(factory1);
                s2 = tab.getBugsCategories(factory2);
                break;
            case DETECTOR_NAME:
            default:
                s1 = "" + factory1.getShortName(); //$NON-NLS-1$
                s2 = factory2.getShortName();
                break;
            }
            if (s1 == null) {
                s1 = "";
            }
            if (s2 == null) {
                s2 = "";
            }
            result = s1.compareTo(s2);

            // second sort if elements are equals - on only 2 criterias
            if (result == 0) {
                switch (getSortColumnId()) {
                case DETECTOR_NAME:
                    s1 = tab.getBugsAbbreviation(factory1);
                    s2 = tab.getBugsAbbreviation(factory2);
                    break;
                case BUG_CODES:
                default:
                    s1 = "" + factory1.getShortName(); //$NON-NLS-1$
                    s2 = factory2.getShortName();
                    break;
                }
                result = s1.compareTo(s2);
            } else if (revertOrder) {
                // same column selected twice - revert first order
                result = -Util.sign(result);
            }
            return result;
        }

        @Override
        public boolean isSorterProperty(Object element, String property) {
            return property.equals(COLUMN.DETECTOR_NAME.name()) || property.equals(COLUMN.BUG_CODES.name())
                    || property.equals(COLUMN.DETECTOR_SPEED.name()) || property.equals(COLUMN.PLUGIN.name());
        }

        /**
         * @param columnId
         *            The sortColumnId to set.
         */
        public void setSortColumnIndex(COLUMN columnId) {
            this.lastSortColumnId = this.sortColumnId;
            this.sortColumnId = columnId;
            revertOrder = !revertOrder && lastSortColumnId == columnId;
        }

        /**
         * @return Returns the sortColumnId.
         */
        public COLUMN getSortColumnId() {
            return sortColumnId;
        }
    }

    private static final class DetectorFactoriesContentProvider implements IStructuredContentProvider {
        public void dispose() {
            // ignored
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // ignored
        }

        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof List) {
                List<?> list = (List<?>) inputElement;
                return list.toArray();
            }
            return null;
        }
    }

    private static final class DetectorFactoryLabelProvider implements ITableLabelProvider, IColorProvider {
        private final DetectorConfigurationTab tab;

        DetectorFactoryLabelProvider(DetectorConfigurationTab tab) {
            this.tab = tab;
        }

        public void addListener(ILabelProviderListener listener) {
            // ignored
        }

        public void dispose() {
            // ignored
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // ignored
        }

        public Image getColumnImage(Object element, int columnIndex) {
            // TODO ignored - but if we have images for different detectors ...
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {

            if (!(element instanceof DetectorFactory)) {
                return null;
            }
            DetectorFactory factory = (DetectorFactory) element;
            COLUMN col = tab.getColumn(columnIndex);

            switch (col) {
            case BUG_CODES:
                return tab.getBugsAbbreviation(factory);
            case DETECTOR_SPEED:
                return factory.getSpeed();
            case PLUGIN:
                String provider = factory.getPlugin().getProvider();
                if(provider == null) {
                    provider = "<unknown>";
                }
                if (provider.endsWith(" project")) {
                    return provider.substring(0, provider.length() - " project".length());
                }
                return provider;
            case BUG_CATEGORIES:
                return tab.getBugsCategories(factory);
            case DETECTOR_NAME:
                return factory.getShortName();
            default:
                return null;
            }
        }

        public Color getBackground(Object element) {
            if (!(element instanceof DetectorFactory)) {
                return null;
            }
            if (!isFactoryVisible((DetectorFactory) element)) {
                return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
            }
            return null;
        }

        /**
         * Return whether or not given DetectorFactory reports bug patterns in
         * one of the currently-enabled set of bug categories.
         *
         * @param factory
         *            the DetectorFactory
         * @return true if the factory reports bug patterns in one of the
         *         currently-enabled bug categories, false if not
         */
        private boolean isFactoryVisible(DetectorFactory factory) {
            Map<DetectorFactory, Boolean> enabledDetectors = tab.propertyPage.getVisibleDetectors();
            Boolean enabled = enabledDetectors.get(factory);

            if (enabled != null) {
                return enabled.booleanValue();
            }
            ProjectFilterSettings filterSettings = tab.getCurrentProps().getFilterSettings();
            for (BugPattern pattern : factory.getReportedBugPatterns()) {
                if (filterSettings.containsCategory(pattern.getCategory())) {
                    enabledDetectors.put(factory, Boolean.TRUE);
                    return true;
                }
            }
            enabledDetectors.put(factory, Boolean.FALSE);
            return false;
        }

        public Color getForeground(Object element) {
            return null;
        }
    }

    private Map<DetectorFactory, String> factoriesToBugAbbrev;

    private final FindbugsPropertyPage propertyPage;

    protected CheckboxTableViewer availableFactoriesTableViewer;

    private final Map<Integer, COLUMN> columnsMap;

    private final Button hiddenVisible;

    public DetectorConfigurationTab(TabFolder tabFolder, final FindbugsPropertyPage page, int style) {
        super(tabFolder, style);
        columnsMap = new HashMap<Integer, COLUMN>();
        this.propertyPage = page;
        setLayout(new GridLayout());

        TabItem tabDetector = new TabItem(tabFolder, SWT.NONE);
        tabDetector.setText(getMessage("property.detectorsTab"));
        tabDetector.setControl(this);
        tabDetector.setToolTipText("Enable / disable available detectors");

        Label info = new Label(this, SWT.WRAP);
        info.setText("Disabled detectors will not participate in FindBugs analysis. \n"
                + "'Grayed out' detectors will run, however they will not report" + " any results to the UI.");

        hiddenVisible = new Button(this, SWT.CHECK);
        hiddenVisible.setText("Show hidden detectors");
        hiddenVisible.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                populateAvailableRulesTable(propertyPage.getProject());
            }
        });

        final SashForm sash = new SashForm(this, SWT.VERTICAL);
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        layoutData.heightHint = 400;
        layoutData.widthHint = 550;

        sash.setLayoutData(layoutData);

        Table availableRulesTable = createDetectorsTableViewer(sash, page.getProject());
        GridData tableLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL);
        tableLayoutData.heightHint = 300;
        tableLayoutData.widthHint = 550;
        availableRulesTable.setLayoutData(tableLayoutData);

        Group group = new Group(sash, SWT.NONE);
        group.setLayout(new GridLayout());
        GridData data = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(data);
        group.setText("Detector details");

        final Text text = new Text(group, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
        GridData layoutData2 = new GridData(GridData.FILL_BOTH);
        text.setLayoutData(layoutData2);
        text.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        sash.setWeights(new int[] { 3, 1 });

        availableRulesTable.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                TableItem item = (TableItem) e.item;
                DetectorFactory factory = (DetectorFactory) item.getData();
                String description = getDetailedText(factory);
                text.setText(description);
            }
        });
    }

    private static String getDetailedText(DetectorFactory factory) {
        if (factory == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(factory.getFullName());
        sb.append("\n");
        sb.append(getDescriptionWithoutHtml(factory));
        sb.append("\n\nReported patterns:\n");
        Collection<BugPattern> patterns = factory.getReportedBugPatterns();
        for (Iterator<BugPattern> iter = patterns.iterator(); iter.hasNext();) {
            BugPattern pattern = iter.next();
            sb.append(pattern.getType()).append(" ").append(" (").append(pattern.getAbbrev()).append(", ")
                    .append(pattern.getCategory()).append("):").append("  ");
            sb.append(pattern.getShortDescription());
            if (iter.hasNext()) {
                sb.append("\n");
            }
        }
        if (patterns.isEmpty()) {
            sb.append("none");
        }
        sb.append(getPluginDescription(factory.getPlugin()));
        return sb.toString();
    }

    private static String getPluginDescription(Plugin plugin) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nPlugin: ").append(plugin.getPluginId());
        String version = plugin.getVersion();
        if (version.length() > 0) {
            sb.append("\nVersion: ").append(version);
        }

        sb.append("\nProvider: ").append(plugin.getProvider());
        String website = plugin.getWebsite();
        if (website != null && website.length() > 0) {
            sb.append(" (").append(website).append(")");
        }
        return sb.toString();
    }

    /**
     * Tries to trim all the html out of the
     * {@link DetectorFactory#getDetailHTML()} return value. See also private
     * {@link PluginLoader} .init() method.
     */
    private static String getDescriptionWithoutHtml(DetectorFactory factory) {
        String detailHTML = factory.getDetailHTML();
        // cut beginning and the end of the html document
        detailHTML = trimHtml(detailHTML, "<BODY>", "</BODY>");
        // replace any amount of white space with newline inbetween through one
        // space
        detailHTML = detailHTML.replaceAll("\\s*[\\n]+\\s*", " ");
        // remove all valid html tags
        detailHTML = detailHTML.replaceAll("<[a-zA-Z]+>", "");
        detailHTML = detailHTML.replaceAll("</[a-zA-Z]+>", "");
        // convert some of the entities which are used in current FB
        // messages.xml
        detailHTML = detailHTML.replaceAll("&nbsp;", "");
        detailHTML = detailHTML.replaceAll("&lt;", "<");
        detailHTML = detailHTML.replaceAll("&gt;", ">");
        detailHTML = detailHTML.replaceAll("&amp;", "&");
        return detailHTML.trim();
    }

    private static String trimHtml(String detailHTML, String startTag, String endTag) {
        if (detailHTML.indexOf(startTag) > 0) {
            detailHTML = detailHTML.substring(detailHTML.indexOf(startTag) + startTag.length());
        }
        if (detailHTML.indexOf(endTag) > 0) {
            detailHTML = detailHTML.substring(0, detailHTML.lastIndexOf(endTag));
        }
        return detailHTML;
    }

    /**
     * @param factory
     * @return
     */
    private String getBugsCategories(DetectorFactory factory) {
        Collection<BugPattern> patterns = factory.getReportedBugPatterns();
        String category = null;
        Set<String> categories = new TreeSet<String>();
        for (BugPattern bugPattern : patterns) {
            String category2 = bugPattern.getCategory();
            if (category == null) {
                category = category2;
            } else if (!category.equals(category2)) {
                categories.add(category);
                categories.add(category2);
            }
        }
        if (!categories.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String string : categories) {
                sb.append(I18N.instance().getBugCategoryDescription(string)).append("|");
            }
            category = sb.toString();
        } else {
            category = I18N.instance().getBugCategoryDescription(category);
        }
        return category;
    }

    void refreshUI(UserPreferences preferences) {
        // Enable only those detectors that are enabled by preferences
        TableItem[] itemList = availableFactoriesTableViewer.getTable().getItems();
        for (int i = 0; i < itemList.length; i++) {
            TableItem item = itemList[i];
            DetectorFactory factory = (DetectorFactory) item.getData();
            item.setChecked(preferences.isDetectorEnabled(factory));
        }
        refreshTable();
        syncUserPreferencesWithTable();
    }

    void refreshTable() {
        availableFactoriesTableViewer.refresh(true);
    }

    /**
     * Disables all unchecked detector factories and enables checked factory
     * detectors, leaving those not in the table unmodified.
     */
    protected void syncUserPreferencesWithTable() {
        TableItem[] itemList = availableFactoriesTableViewer.getTable().getItems();
        UserPreferences currentProps = getCurrentProps();
        for (int i = 0; i < itemList.length; i++) {
            DetectorFactory factory = (DetectorFactory) itemList[i].getData();
            // set enabled if defined in configuration
            currentProps.enableDetector(factory, itemList[i].getChecked());
        }
    }

    /**
     * @return
     */
    private UserPreferences getCurrentProps() {
        return propertyPage.getCurrentUserPreferences();
    }

    /**
     * @param sorter
     * @param column
     */
    private void addColumnSelectionListener(final BugPatternTableSorter sorter, final TableColumn column, final COLUMN columnId) {
        column.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sorter.setSortColumnIndex(columnId);
                Table factoriesTable = availableFactoriesTableViewer.getTable();
                factoriesTable.setSortDirection(sorter.revertOrder ? SWT.UP : SWT.DOWN);
                factoriesTable.setSortColumn(column);
                availableFactoriesTableViewer.refresh();
            }
        });
    }

    /**
     * Build rule table viewer
     */
    private Table createDetectorsTableViewer(Composite parent, IProject project) {
        final BugPatternTableSorter sorter = new BugPatternTableSorter(this);

        int tableStyle = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK;
        availableFactoriesTableViewer = CheckboxTableViewer.newCheckList(parent, tableStyle);
        availableFactoriesTableViewer.addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                syncUserPreferencesWithTable();
            }
        });

        int currentColumnIdx = 0;
        Table factoriesTable = availableFactoriesTableViewer.getTable();

        TableColumn factoryNameColumn = createColumn(currentColumnIdx, factoriesTable, getMessage("property.detectorName"), 230,
                COLUMN.DETECTOR_NAME);
        addColumnSelectionListener(sorter, factoryNameColumn, COLUMN.DETECTOR_NAME);

        currentColumnIdx++;
        TableColumn bugsAbbrevColumn = createColumn(currentColumnIdx, factoriesTable, getMessage("property.bugCodes"), 75,
                COLUMN.BUG_CODES);
        addColumnSelectionListener(sorter, bugsAbbrevColumn, COLUMN.BUG_CODES);

        currentColumnIdx++;
        TableColumn speedColumn = createColumn(currentColumnIdx, factoriesTable, getMessage("property.speed"), 70,
                COLUMN.DETECTOR_SPEED);
        addColumnSelectionListener(sorter, speedColumn, COLUMN.DETECTOR_SPEED);

        currentColumnIdx++;
        TableColumn pluginColumn = createColumn(currentColumnIdx, factoriesTable, getMessage("property.provider"), 100,
                COLUMN.PLUGIN);
        addColumnSelectionListener(sorter, pluginColumn, COLUMN.PLUGIN);

        currentColumnIdx++;
        TableColumn categoryColumn = createColumn(currentColumnIdx, factoriesTable, getMessage("property.category"), 75,
                COLUMN.BUG_CATEGORIES);
        addColumnSelectionListener(sorter, categoryColumn, COLUMN.BUG_CATEGORIES);

        factoriesTable.setLinesVisible(true);
        factoriesTable.setHeaderVisible(true);
        // initial sort indicator
        factoriesTable.setSortDirection(sorter.revertOrder ? SWT.UP : SWT.DOWN);
        factoriesTable.setSortColumn(factoryNameColumn);
        sorter.setSortColumnIndex(COLUMN.DETECTOR_NAME);

        availableFactoriesTableViewer.setContentProvider(new DetectorFactoriesContentProvider());
        availableFactoriesTableViewer.setLabelProvider(new DetectorFactoryLabelProvider(this));

        availableFactoriesTableViewer.setSorter(sorter);

        populateAvailableRulesTable(project);
        factoriesTable.setEnabled(true);

        return factoriesTable;
    }

    private COLUMN getColumn(int index) {
        COLUMN column = columnsMap.get(Integer.valueOf(index));
        if (column == null) {
            return COLUMN.UNKNOWN;
        }
        return column;
    }

    /**
     * @param currentColumnIdx
     * @param factoriesTable
     */
    private TableColumn createColumn(int currentColumnIdx, Table factoriesTable, String text, int width, COLUMN col) {
        TableColumn column = new TableColumn(factoriesTable, SWT.FILL);
        column.setResizable(true);
        column.setText(text);
        column.setWidth(width);
        columnsMap.put(Integer.valueOf(currentColumnIdx), col);
        return column;
    }

    /**
     * Helper method to shorten message access
     *
     * @param key
     *            a message key
     * @return requested message
     */
    protected String getMessage(String key) {
        return FindbugsPlugin.getDefault().getMessage(key);
    }

    /**
     * Populate the rule table
     */
    private void populateAvailableRulesTable(IProject project) {
        List<DetectorFactory> allAvailableList = new ArrayList<DetectorFactory>();
        factoriesToBugAbbrev = new HashMap<DetectorFactory, String>();
        Iterator<DetectorFactory> iterator = DetectorFactoryCollection.instance().factoryIterator();
        while (iterator.hasNext()) {
            DetectorFactory factory = iterator.next();

            // Only configure non-hidden factories
            if (factory.isHidden() && !isHiddenVisible()) {
                continue;
            }

            allAvailableList.add(factory);
            addBugsAbbreviation(factory);
        }

        availableFactoriesTableViewer.setInput(allAvailableList);
        TableItem[] itemList = availableFactoriesTableViewer.getTable().getItems();
        UserPreferences userPreferences = getCurrentProps();
        for (int i = 0; i < itemList.length; i++) {
            DetectorFactory rule = (DetectorFactory) itemList[i].getData();
            // set enabled if defined in configuration
            if (userPreferences.isDetectorEnabled(rule)) {
                itemList[i].setChecked(true);
            }
        }
    }

    boolean isHiddenVisible() {
        return hiddenVisible.getSelection();
    }

    /**
     * @param factory
     */
    protected void addBugsAbbreviation(DetectorFactory factory) {
        factoriesToBugAbbrev.put(factory, createBugsAbbreviation(factory));
    }

    protected String getBugsAbbreviation(DetectorFactory factory) {
        String abbr = factoriesToBugAbbrev.get(factory);
        if (abbr == null) {
            abbr = createBugsAbbreviation(factory);
        }
        return abbr;
    }

    @Nonnull
    private String createBugsAbbreviation(DetectorFactory factory) {
        StringBuffer sb = new StringBuffer();
        Collection<BugPattern> patterns = factory.getReportedBugPatterns();
        LinkedHashSet<String> abbrs = new LinkedHashSet<String>();
        for (Iterator<BugPattern> iter = patterns.iterator(); iter.hasNext();) {
            BugPattern pattern = iter.next();
            String abbr = pattern.getAbbrev();
            abbrs.add(abbr);
        }
        for (Iterator<String> iter = abbrs.iterator(); iter.hasNext();) {
            String element = iter.next();
            sb.append(element);
            if (iter.hasNext()) {
                sb.append("|"); //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    @Override
    public void setEnabled(boolean enabled) {
        availableFactoriesTableViewer.getTable().setEnabled(enabled);
        hiddenVisible.setEnabled(enabled);
        super.setEnabled(enabled);
    }

}
