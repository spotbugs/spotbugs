package de.tobject.findbugs.properties;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class PathElementLabelProvider extends LabelProvider implements IColorProvider {

    public Color getForeground(Object element) {
        if(!(element instanceof IPathElement)) {
            return null;
        }
        IPathElement pathElement = (IPathElement) element;
        IStatus status = pathElement.getStatus();
        if(status == null || status.isOK()) {
            // use default
            return null;
        }
        return Display.getDefault().getSystemColor(SWT.COLOR_RED);
    }

    public Color getBackground(Object element) {
        return null;
    }

    @Nonnull
    public String getToolTip(Object element) {
        if(!(element instanceof IPathElement)) {
            return "";
        }
        IPathElement pathElement = (IPathElement) element;
        IStatus status = pathElement.getStatus();
        if(status == null || status.isOK()) {
            return pathElement.toString();
        }
        return status.getMessage();
    }


}
