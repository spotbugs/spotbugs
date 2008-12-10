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
package de.tobject.findbugs.view.explorer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;

public class PropertyPageAdapterFactory implements IAdapterFactory {

	public static class PropertySource implements IPropertySource {

		private final Object object;
		private final IPropertyDescriptor[] propertyDescriptors;

		public PropertySource(Object object) {
			this.object = object;
			List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
			List<Method> getters = getGetters(object);
			for (Method method : getters) {
				props.add(new PropertyDescriptor(method, getReadableName(method)));
			}
			propertyDescriptors = props.toArray(new PropertyDescriptor[0]);
		}

		public IPropertyDescriptor[] getPropertyDescriptors() {
			return propertyDescriptors;
		}

		public Object getPropertyValue(Object propId) {
			if (propId instanceof Method) {
				Method method = (Method) propId;
				try {
					return method.invoke(object, (Object[])null);
				} catch (Exception e) {
					FindbugsPlugin.getDefault().logException(e,
						"getPropertyValue: method access failed");
				}
			}
			return null;
		}

		public Object getEditableValue() {
			return null;
		}
		public boolean isPropertySet(Object id) {
			return false;
		}
		public void resetPropertyValue(Object id) {
			//
		}
		public void setPropertyValue(Object id, Object value) {
			//
		}
	}

	static List<Method> getGetters(Object obj){
		List<Method> methodList = new ArrayList<Method>();
		Method[] methods = obj.getClass().getMethods();
		for (Method method : methods) {
			if(method.getParameterTypes().length == 0){
				String name = method.getName();
				if(name.startsWith("get") || name.startsWith("is")) {
					methodList.add(method);
				}
			}
		}
		return methodList;
	}

	public static String getReadableName(Method method) {
		String name = method.getName();
		return name.startsWith("get") ? name.substring(3) : name.startsWith("is") ? name
				.substring(2) : name;
	}

	static enum PropId {
		Type, Resource, Bug, Id, CreationTime
	}

	public static class MarkerPropertySource implements IPropertySource {

		private final IMarker marker;
		private final IPropertyDescriptor[] propertyDescriptors;

		public MarkerPropertySource(IMarker marker) {
			this.marker = marker;
			List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
			try {
				Map<?, ?> attributes = marker.getAttributes();
				Set<?> keySet = new TreeSet<Object>(attributes.keySet());
				for (Object object : keySet) {
					props.add(new PropertyDescriptor(object, "" + object));
				}
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e,
						"MarkerPropertySource: marker access failed");
			}
			props.add(new PropertyDescriptor(PropId.Bug, "Bug"));
			props.add(new PropertyDescriptor(PropId.Resource, "Resource"));
			props.add(new PropertyDescriptor(PropId.Id, "Marker id"));
			props.add(new PropertyDescriptor(PropId.Type, "Marker type"));
			props.add(new PropertyDescriptor(PropId.CreationTime, "Creation time"));
			propertyDescriptors = props.toArray(new PropertyDescriptor[0]);
		}

		public IPropertyDescriptor[] getPropertyDescriptors() {
			return propertyDescriptors;
		}

		public Object getPropertyValue(Object propId) {
			try {
				if (propId instanceof PropId) {
					PropId id = (PropId) propId;
					switch (id) {
					case Bug:
						return MarkerUtil.findBugInstanceForMarker(marker);
					case Resource:
						return marker.getResource();
					case Id:
						return Long.valueOf(marker.getId());
					case CreationTime:
						return new Date(marker.getCreationTime());
					case Type:
						return marker.getType();
					}
				} else if (propId instanceof String) {
					return marker.getAttribute((String) propId);
				}
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e,
						"getPropertyValue: marker access failed");
			}
			return null;
		}

		public Object getEditableValue() {
			return null;
		}
		public boolean isPropertySet(Object id) {
			return false;
		}
		public void resetPropertyValue(Object id) {
			//
		}
		public void setPropertyValue(Object id, Object value) {
			//
		}
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IPropertySheetPage.class && (adaptableObject instanceof JavaEditor)) {
			IWorkbenchPart part = (IWorkbenchPart) adaptableObject;
			IViewReference[] references = part.getSite().getPage().getViewReferences();
			for (IViewReference viewReference : references) {
				if ("de.tobject.findbugs.view.bugtreeview".equals(viewReference.getId())) {
					IWorkbenchPart workbenchPart = viewReference.getPart(false);
					if(workbenchPart != null){
						return new JavaEditorTabbedPropertySheetPage(workbenchPart);
					}
				}
			}
			// return nothing to get rid of errors generated through
			// TabbedPropertySheetAdapterFactory which is adapting to CommonNavigator
			// class
			return null;
		}
		if (adapterType == IPropertySource.class) {
			if (adaptableObject instanceof BugPattern) {
				BugPattern bug = (BugPattern) adaptableObject;
				return new PropertySource(bug);
			}
			if (adaptableObject instanceof BugInstance) {
				BugInstance bug = (BugInstance) adaptableObject;
				return new PropertySource(bug);
			}
			IMarker marker = null;
			if (adaptableObject instanceof IMarker) {
				marker = (IMarker) adaptableObject;
			} else if (adaptableObject instanceof IAdaptable) {
				marker = (IMarker) ((IAdaptable) adaptableObject)
						.getAdapter(IMarker.class);
			}
			if (marker == null || !MarkerUtil.isFindBugsMarker(marker)) {
				return null;
			}
			return new MarkerPropertySource(marker);
		}
		if (adapterType == BugInstance.class) {
			if (adaptableObject instanceof BugInstance) {
				BugInstance bug = (BugInstance) adaptableObject;
				return new PropertySource(bug);
			}
		}
		if (adapterType == BugPattern.class) {
			if (adaptableObject instanceof BugPattern) {
				BugPattern bug = (BugPattern) adaptableObject;
				return new PropertySource(bug);
			}
		}
		return null;
	}


	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return new Class[] { IPropertySheetPage.class, IPropertySource.class };
	}

	private static class JavaEditorTabbedPropertySheetPage extends TabbedPropertySheetPage {

		private final IWorkbenchPart workbenchPart;

		static ITabbedPropertySheetPageContributor contributor = new ITabbedPropertySheetPageContributor() {
			public String getContributorId() {
				return "de.tobject.findbugs.view.bugtreeview";
			}
		};

		public JavaEditorTabbedPropertySheetPage(IWorkbenchPart workbenchPart) {
			super(contributor);
			this.workbenchPart = workbenchPart;
		}

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			selection = workbenchPart.getSite().getSelectionProvider().getSelection();
			super.selectionChanged(part, selection);
		}
	}

}
