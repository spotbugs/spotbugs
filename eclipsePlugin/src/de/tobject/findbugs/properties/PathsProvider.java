/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import de.tobject.findbugs.builder.FindBugsWorker;

abstract class PathsProvider extends SelectionAdapter implements IStructuredContentProvider,
    ICheckStateProvider {
    private static IPath lastUsedPath;

    protected final List<IPathElement> paths;

    private final Control control;

    private final TableViewer viewer;

    protected final FindbugsPropertyPage propertyPage;

    private final ListenerList listeners;

    protected PathsProvider(TableViewer viewer, FindbugsPropertyPage propertyPage) {
        this.propertyPage = propertyPage;
        this.paths = new ArrayList<IPathElement>();
        this.viewer = viewer;
        if(viewer instanceof CheckboxTableViewer) {
            CheckboxTableViewer tv = (CheckboxTableViewer) viewer;
            tv.setCheckStateProvider(this);
            tv.addCheckStateListener(new ICheckStateListener() {
                public void checkStateChanged(CheckStateChangedEvent event) {
                    boolean checked = event.getChecked();
                    IPathElement element = (IPathElement) event.getElement();
                    element.setEnabled(checked);
                    handleContendChanged();
                }
            });
        }
        this.control = viewer.getTable();
        listeners = new ListenerList();
        viewer.setContentProvider(this);
    }

    static void setLastUsedPath(IPath lastUsed) {
        // TODO write to preferences
        lastUsedPath = lastUsed;
    }

    static IPath getLastUsedPath() {
        // TODO read from preferences
        return lastUsedPath;
    }

    void setFilters(List<IPathElement> filterFiles) {
        paths.clear();
        paths.addAll(filterFiles);
        if(viewer instanceof CheckboxTableViewer) {
            CheckboxTableViewer tv = (CheckboxTableViewer) viewer;
            List<IPathElement> checked = new ArrayList<IPathElement>();
            for (IPathElement pe : paths) {
                if(pe.isEnabled()) {
                    checked.add(pe);
                }
            }
            tv.setCheckedElements(checked.toArray(new IPathElement[checked.size()]));
        }
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        Widget widget = e.widget;
        String buttonId = widget.getData() + "";
        if ("add".equals(buttonId)) {
            addFiles(e.display.getActiveShell());
        } else {
            Iterator<?> selectionIter = ((IStructuredSelection) viewer.getSelection()).iterator();
            while (selectionIter.hasNext()) {
                remove((IPathElement) selectionIter.next());
            }
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void addFiles(Shell parentShell) {
        FileDialog dialog = createFileDialog(parentShell);

        // The validator checks to see if the user's selection
        // is valid given the type of the object selected (e.g.
        // it can't be a folder) and the objects that have
        // already been selected
        String pathStr = openFileDialog(dialog);
        if (pathStr == null) {
            return;
        }
        addSelectedPaths(dialog);
        handleContendChanged();
    }

    private FileDialog createFileDialog(Shell parentShell) {
        FileDialog dialog = new FileDialog(parentShell, SWT.OPEN | SWT.MULTI);
        configureDialog(dialog);

        IPath lastUsed = getLastUsedPath();
        String filterPath = null;
        if (lastUsed != null && lastUsed.toFile().isDirectory()) {
            filterPath = lastUsed.toOSString();
            dialog.setFilterPath(filterPath);
        }
        return dialog;
    }

    abstract protected void configureDialog(FileDialog dialog);

    protected String openFileDialog(FileDialog dialog) {
        return dialog.open();
    }

    protected String[] getFileNames(FileDialog dialog) {
        return dialog.getFileNames();
    }

    protected String getFilePath(FileDialog dialog) {
        return dialog.getFilterPath();
    }

    private void addSelectedPaths(FileDialog dialog) {
        String[] names = getFileNames(dialog);
        String filterPath = getFilePath(dialog);
        Path baseDir = new Path(filterPath);
        setLastUsedPath(baseDir);
        for (String fileName : names) {
            IPath path = baseDir.append(fileName);
            PathElement pathElt = new PathElement(path, Status.OK_STATUS);
            if (!paths.contains(pathElt)) {
                paths.add(pathElt);
            }
        }
    }

    public void dispose() {
        //
    }

    public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
        //
    }

    public Object[] getElements(Object inputElement) {
        return paths.toArray();
    }

    public boolean isChecked(Object element) {
        if(element instanceof IPathElement) {
            IPathElement elt = (IPathElement) element;
            return elt.isEnabled();
        }
        return false;
    }

    public boolean isGrayed(Object element) {
        return false;
    }

    boolean contains(Object o) {
        return paths.contains(o);
    }

    void setControlEnabled(boolean enabled) {
        control.setEnabled(enabled);
    }

    void refresh() {
        IStatus status = validate();
        if (status != null) {
            propertyPage.setErrorMessage(status.getMessage());
        }
        viewer.setSelection(null);
        viewer.setInput(new Object());
        viewer.refresh(true);
    }

    abstract protected IStatus validate();

    public void remove(IPathElement holder) {
        paths.remove(holder);
        handleContendChanged();
    }

    private void handleContendChanged() {
        applyToPreferences();
        for (Object object : listeners.getListeners()) {
            ((Listener) object).handleEvent(null);
        }
    }

    protected void applyToPreferences() {
        IStatus status = validate();
        if (status != null) {
            propertyPage.setErrorMessage(status.getMessage());
        }
    }

    protected Map<String, Boolean> pathsToStrings() {
        IProject project = propertyPage.getProject();
        Map<String, Boolean> result = new TreeMap<String, Boolean>();
        for (IPathElement path : paths) {
            if(path.isSystem()) {
                if (!path.isEnabled()) {
                    // only need to remember *disabled* plugins
                    result.put(path.getId(), Boolean.valueOf(false));
                }
                continue;
            }
            IPath filterPath = FindBugsWorker.toFilterPath(path.getPath(), project);
            result.put(filterPath.toPortableString(), Boolean.valueOf(path.isEnabled()));
        }
        return result;
    }

}
