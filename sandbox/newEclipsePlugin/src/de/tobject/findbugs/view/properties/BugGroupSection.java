/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
package de.tobject.findbugs.view.properties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.tobject.findbugs.view.explorer.BugGroup;

/**
 * @author Andrei
 */
public class BugGroupSection extends AbstractPropertySection {

	private Composite rootComposite;
	private Text text;
	private final PropPageTitleProvider provider;

	public BugGroupSection() {
		super();
		provider = new PropPageTitleProvider();
	}

	@Override
	public void createControls(Composite parent,
			final TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
		Color background = tabbedPropertySheetPage.getWidgetFactory().getColors()
				.getBackground();

		rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginLeft = 5;
		layout.marginTop = 5;
		rootComposite.setLayout(layout);
		rootComposite.setSize(SWT.DEFAULT, SWT.DEFAULT);

		rootComposite.setBackground(background);

		text = new Text(rootComposite, SWT.READ_ONLY | SWT.WRAP);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		text.setLayoutData(data);
		text.setBackground(background);
		text.setFont(JFaceResources.getTextFont());
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		refreshText(selection);
	}

	@Override
	public void refresh() {
		super.refresh();
	}

	@Override
	public void dispose() {
		if(rootComposite != null) {
			rootComposite.dispose();
		}
		super.dispose();
	}

	@Override
	public boolean shouldUseExtraSpace() {
		return true;
	}

	private void refreshText(ISelection selection) {
		text.setText("");
		if(selection.isEmpty() || !(selection instanceof IStructuredSelection)){
			return;
		}

		IStructuredSelection selection2 = (IStructuredSelection) selection;
		if(selection2.size() > 1){
			String description = getSummary(selection2);
			text.setText(description);
		} else {
			Object element = selection2.getFirstElement();
			if(!(element instanceof BugGroup)){
				return;
			}
			BugGroup bugGroup = (BugGroup) element;
			String title = provider.getTitle(bugGroup);
			char[] separator = new char[title.length() + 1];
			Arrays.fill(separator, '=');
			separator[0] = '\n';
			text.setText(title + String.valueOf(separator) + "\nBugs count: "
					+ bugGroup.getMarkersCount());
		}
	}

	private String getSummary(IStructuredSelection selection) {
		Iterator<?> iter = selection.iterator();
		Set<BugGroup> groups = new HashSet<BugGroup>();
		Set<IMarker> markers = new HashSet<IMarker>();
		while (iter.hasNext()) {
			Object object = iter.next();
			if(!(object instanceof BugGroup)){
				continue;
			}
			BugGroup group = (BugGroup) object;
			if(!groups.contains(group)){
				markers.addAll(group.getAllMarkers());
				groups.add(group);
			}
		}
		Set<String> names = new TreeSet<String>();
		for (BugGroup bugGroup : groups) {
			String description = bugGroup.getShortDescription() + " (" + bugGroup.getMarkersCount() + ")";
			names.add(description);
		}
		StringBuilder sb = new StringBuilder();
		int maxLength = 0;
		for (String name : names) {
			sb.append(name).append("\n");
			if(name.length() > maxLength){
				maxLength = name.length();
			}
		}
		char[] separator = new char[maxLength];
		Arrays.fill(separator, '=');
		sb.append(separator);
		String description = sb.toString() + "\nOverall bugs count: " + markers.size();
		return description;
	}

}
