/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003, Peter Friese
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
 
package de.tobject.findbugs.properties;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.util.ProjectUtilities;

public class FindbugsPropertyPage extends PropertyPage {

	private boolean initialEnabled;
	private Button chkEnableFindBugs;
	private IProject project;
	private static final String PATH_TITLE = "Path:";
	private static final String OWNER_TITLE = "&Owner:";
	private static final String OWNER_PROPERTY = "OWNER";
	private static final String DEFAULT_OWNER = "John Doe";

	private static final int TEXT_FIELD_WIDTH = 50;

	private Text ownerText;

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public FindbugsPropertyPage() {
		super();
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(PATH_TITLE);

		// Path text field
		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		pathValueText.setText(((IResource) getElement()).getFullPath().toString());
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		// Label for owner field
		Label ownerLabel = new Label(composite, SWT.NONE);
		ownerLabel.setText(OWNER_TITLE);

		// Owner text field
		ownerText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		ownerText.setLayoutData(gd);

		// Populate owner text field
		try {
			String owner =
				((IResource) getElement()).getPersistentProperty(
					new QualifiedName("", OWNER_PROPERTY));
			ownerText.setText((owner != null) ? owner : DEFAULT_OWNER);
		} catch (CoreException e) {
			ownerText.setText(DEFAULT_OWNER);
		}
	}
	

	/**
	 * @param parent The parent composite.
	 */
	private void addEnableFindBugsCheckbox(Composite parent) {
		Composite composite = createDefaultComposite(parent);
		
		Button button = new Button(composite, SWT.CHECK);
		button.setText("Enable FindBugs");
		GridData data = new GridData();
		button.setLayoutData(data);
		
		this.chkEnableFindBugs = button;
		this.initialEnabled = isEnabled();
		this.chkEnableFindBugs.setSelection(this.initialEnabled);
	}

	

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		noDefaultAndApplyButton();
		
		// getElement returns the element this page has been opened for,
		// in our case this is a Java Project (IJavaProject).
		IAdaptable resource = getElement();
		IJavaProject javaProject = (IJavaProject)resource.getAdapter(IJavaProject.class);
		if (javaProject != null) {
			// get the IProject underlying the IJavaProject
			this.project = javaProject.getProject();
		}
		

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);
		
		addEnableFindBugsCheckbox(composite);
		
		return composite;		
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	/**
	 * Will be called when the user presses the OK button.
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean selection = this.chkEnableFindBugs.getSelection();
		boolean result = true;
		if (!this.initialEnabled && selection == true) {
			result = addNature();
		} else if (this.initialEnabled && selection == false) {
			result = removeNature();
		}
		return result;
	}
	
	
	/**
	 * Using the natures name, check whether the current
	 * project has the given nature.
	 * 
	 * @return boolean <code>true</code>, if the nature is
	 *   assigned to the project, <code>false</code> otherwise.
	 */
	private boolean isEnabled() {
		boolean result = false;

		try {
			if (this.project.hasNature(FindbugsPlugin.NATURE_ID))
				result = true;
		} 
		catch (CoreException e) {
			System.err.println("Exception: " + e);
		}
		return result;
	}	
	
	/**
	 * Add the nature to the current project. The real work is 
	 * done by the inner class NatureWorker
	 * @return boolean <code>true</code> if the nature could
	 *   be added successfully, <code>false</code> otherwise.
	 */
	private boolean addNature() {
		boolean result = true;
		try {
			NatureWorker worker = new NatureWorker(true);
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(getShell());
			monitor.run(true, true, worker);
		} 
		catch (InvocationTargetException e) {
			System.err.println("Exception: " + e);
		} 
		catch (InterruptedException e) {
			System.err.println("Exception: " + e);
		}
		return result;
	}
	
	/**
	 * Remove the nature from the project.
	 * @return boolean <code>true</code> if the nature could
	 *   be added successfully, <code>false</code> otherwise.
	 */
	private boolean removeNature() {
		boolean result = true;
		try {
			// remove any markers added by our builder
			this.project.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
			
			NatureWorker worker = new NatureWorker(false);
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(getShell());
			monitor.run(true, true, worker);
		} 
		catch (InvocationTargetException e) {
			System.err.println("Exception: " + e);
		} 
		catch (InterruptedException e) {
			System.err.println("Exception: " + e);
		} catch (CoreException e) {
			System.err.println("Exception: " + e);
		}
		return result;
				
	}
	
	private class NatureWorker implements IRunnableWithProgress {
		
		private boolean add = true;
		
		public NatureWorker(boolean add) {
			this.add = add;
		}
		
		/**
		 * @see IRunnableWithProgress#run(IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
			try {
				if (add) {
					ProjectUtilities.addFindBugsNature(project, monitor);
				}
				else {
					ProjectUtilities.removeFindBugsNature(project, monitor);
				}
			}
			catch (CoreException e) {
				e.printStackTrace();
				System.err.println("Exception: " + e);
			}
		}
	}

}