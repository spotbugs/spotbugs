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
package de.tobject.findbugs.view.properties;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.Util;
import de.tobject.findbugs.view.AbstractFindbugsView;
import de.tobject.findbugs.view.BugExplorerView;
import de.tobject.findbugs.view.explorer.BugGroup;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.Plugin;

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
                    Object object2 = method.invoke(object, (Object[]) null);
                    if (object2 != null) {
                        if (object2.getClass().isArray()) {
                            return new ArrayPropertySource((Object[]) object2);
                        }
                        if (Collection.class.isAssignableFrom(object2.getClass())) {
                            Collection<?> coll = (Collection<?>) object2;
                            return new ArrayPropertySource(coll.toArray());
                        }
                    }
                    return object2;
                } catch (Exception e) {
                    FindbugsPlugin.getDefault().logException(e, "getPropertyValue: method access failed");
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

    public static class ArrayPropertySource implements IPropertySource {

        private final Object[] array;

        private final IPropertyDescriptor[] propertyDescriptors;

        public ArrayPropertySource(Object[] object) {
            this.array = object;
            List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
            for (Object obj : array) {
                props.add(new PropertyDescriptor(obj, getDisplayName(obj)));
            }
            propertyDescriptors = props.toArray(new PropertyDescriptor[0]);
        }

        private String getDisplayName(Object obj) {
            if (obj instanceof IMarker) {
                return "Marker " + ((IMarker) obj).getId();
            }
            return "" + obj;
        }

        public IPropertyDescriptor[] getPropertyDescriptors() {
            return propertyDescriptors;
        }

        public Object getPropertyValue(Object propId) {
            return propId;
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

    static List<Method> getGetters(Object obj) {
        List<Method> methodList = new ArrayList<Method>();
        Method[] methods = obj.getClass().getMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0) {
                String name = method.getName();
                if ((name.startsWith("get") || name.startsWith("is") || name.startsWith("has"))
                        && (!"getClass".equals(name) && !"hashCode".equals(name))) {
                    methodList.add(method);
                }
            }
        }
        return methodList;
    }

    public static String getReadableName(Method method) {
        String name = method.getName();
        return (name.startsWith("get") || name.startsWith("has")) ? name.substring(3) : name.startsWith("is") ? name.substring(2)
                : name;
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
                FindbugsPlugin.getDefault().logException(e, "MarkerPropertySource: marker access failed");
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
                FindbugsPlugin.getDefault().logException(e, "getPropertyValue: marker access failed");
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

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IPropertySheetPage.class) {
            if (adaptableObject instanceof BugExplorerView || adaptableObject instanceof JavaEditor
                    || adaptableObject instanceof AbstractFindbugsView) {
                return new BugPropertySheetPage();
            }
        }
        if (adapterType == IPropertySource.class) {
            if (adaptableObject instanceof BugPattern || adaptableObject instanceof BugInstance
                    || adaptableObject instanceof DetectorFactory || adaptableObject instanceof Plugin
                    || adaptableObject instanceof BugInstance.XmlProps || adaptableObject instanceof BugGroup
                    || adaptableObject instanceof BugAnnotation) {
                return new PropertySource(adaptableObject);
            }
            IMarker marker = Util.getAdapter(IMarker.class, adaptableObject);
            if (!MarkerUtil.isFindBugsMarker(marker)) {
                return null;
            }
            return new MarkerPropertySource(marker);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Class[] getAdapterList() {
        return new Class[] { IPropertySheetPage.class, IPropertySource.class };
    }

    private static class BugPropertySheetPage extends TabbedPropertySheetPage {

        private boolean isDisposed;

        static ITabbedPropertySheetPageContributor contributor = new ITabbedPropertySheetPageContributor() {
            public String getContributorId() {
                return FindbugsPlugin.TREE_VIEW_ID;
            }
        };

        public BugPropertySheetPage() {
            super(contributor);
        }

        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            if(isDisposed) {
                return;
            }
            // adapt text selection in java editor to FB marker
            if (part instanceof ITextEditor && selection instanceof ITextSelection) {
                IMarker marker = MarkerUtil.getMarkerFromEditor((ITextSelection) selection, (ITextEditor) part);
                if (marker != null) {
                    selection = new StructuredSelection(marker);
                } else {
                    selection = new StructuredSelection();
                }
            }
            super.selectionChanged(part, selection);
        }

        @Override
        public void dispose() {
            isDisposed = true;
            super.dispose();
        }
    }

}
