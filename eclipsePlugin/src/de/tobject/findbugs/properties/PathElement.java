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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

public class PathElement implements IPathElement {

    private final IPath path;

    private IStatus status;

    private boolean enabled;

    public PathElement(IPath path, IStatus status) {
        this.status = status;
        String osString = path.toOSString();
        boolean userEnabled;
        if(!osString.contains("|")) {
            this.path = path;
            // old style: no enablement at all => always on
            userEnabled = true;
        } else {
            String[] parts = osString.split("\\|");
            this.path = new Path(parts[0]);
            userEnabled = Boolean.parseBoolean(parts[1]);
        }
        enabled = userEnabled && this.path.toFile().exists();
    }

    public void setStatus(IStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return path.toString() + (status.isOK() ? "" : " (" + status.getMessage() + ")");
    }

    public String getPath() {
        return path.toOSString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PathElement) {
            return path.equals(((PathElement) obj).path) && enabled == ((PathElement) obj).enabled;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
