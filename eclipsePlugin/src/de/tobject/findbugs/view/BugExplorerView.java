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
package de.tobject.findbugs.view;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.view.explorer.BugContentProvider;

public class BugExplorerView extends CommonNavigator implements IMarkerSelectionHandler, ISelectionChangedListener {

    private MarkerSelectionListener selectionListener;

    private static final String TAG_MEMENTO = "memento";

    private IMemento viewMemento;

    protected boolean selectionInProgress;

    public BugExplorerView() {
        super();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        // Add selection listener to detect click in problems view or in tree
        // view
        ISelectionService theService = getSite().getWorkbenchWindow().getSelectionService();
        selectionListener = new MarkerSelectionListener(this) {
            @Override
            public void selectionChanged(IWorkbenchPart thePart, ISelection theSelection) {
                selectionInProgress = true;
                super.selectionChanged(thePart, theSelection);
                selectionInProgress = false;
            }
        };
        theService.addSelectionListener(selectionListener);
        getCommonViewer().addSelectionChangedListener(this);
    }

    public boolean isVisible() {
        return getSite().getPage().isPartVisible(this);
    }

    public void markerSelected(IWorkbenchPart part, IMarker marker) {
        if (selectionInProgress) {
            return;
        }
        BugContentProvider provider = BugContentProvider.getProvider(getNavigatorContentService());
        CommonViewer commonViewer = getCommonViewer();
        if (marker == null) {
            commonViewer.setSelection(new StructuredSelection(), false);
        } else if (provider.isFiltered(marker)) {
            Object parent = provider.getParent(marker);
            if (parent != null) {
                commonViewer.setSelection(new StructuredSelection(parent), true);
            }
        } else {
            commonViewer.setSelection(new StructuredSelection(marker), true);
        }
    }

    @Override
    public void updateTitle() {
        super.updateTitle();
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        viewMemento = memento;
        if (memento == null) {
            IDialogSettings dialogSettings = FindbugsPlugin.getDefault().getDialogSettings();
            String persistedMemento = dialogSettings.get(TAG_MEMENTO);
            if (persistedMemento == null) {
                // See bug 2504068. First time user opens a view, no settings
                // are defined
                // but we still need to enforce initialisation of content
                // provider
                // which can only happen if memento is not null
                memento = XMLMemento.createWriteRoot("bugExplorer");
            } else {
                try {
                    memento = XMLMemento.createReadRoot(new StringReader(persistedMemento));
                } catch (WorkbenchException e) {
                    // don't do anything. Simply don't restore the settings
                }
            }
        }
        super.init(site, memento);
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class clazz) {
        Object adapter = super.getAdapter(clazz);
        if (adapter == null && clazz == IMemento.class) {
            return viewMemento;
        }
        return adapter;
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
    }

    @Override
    public void dispose() {
        // XXX see https://bugs.eclipse.org/bugs/show_bug.cgi?id=223068
        XMLMemento memento = XMLMemento.createWriteRoot("bugExplorer"); //$NON-NLS-1$
        saveState(memento);
        StringWriter writer = new StringWriter();
        try {
            memento.save(writer);
            IDialogSettings dialogSettings = FindbugsPlugin.getDefault().getDialogSettings();
            dialogSettings.put(TAG_MEMENTO, writer.getBuffer().toString());
        } catch (IOException e) {
            // don't do anything. Simply don't store the settings
        }

        if (selectionListener != null) {
            getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
            selectionListener = null;
        }
        super.dispose();
    }

    public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        if (selection.isEmpty() || selection.size() == 1) {
            setContentDescription("");
        } else {
            setContentDescription(getFrameToolTipText(selection));
        }
    }

    @Override
    public void selectReveal(ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            super.selectReveal(selection);
            return;
        }
        selection = adaptSelection((IStructuredSelection) selection);
        super.selectReveal(selection);
    }

    private ISelection adaptSelection(IStructuredSelection selection) {
        BugContentProvider provider = BugContentProvider.getProvider(getNavigatorContentService());
        Set<Object> accepted = new HashSet<Object>();
        Iterator<?> iter = selection.iterator();
        while (iter.hasNext()) {
            Object object = iter.next();
            Set<Object> supported = provider.getShowInTargets(object);
            accepted.addAll(supported);
        }
        return new StructuredSelection(accepted.toArray());
    }

}
