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
package de.tobject.findbugs.reporter;

import org.eclipse.core.resources.IMarker;

/**
 * Used for mapping between bug priorities and {@link IMarker} constants
 * 
 * @author Andrei Loskutov
 */
public enum MarkerSeverity {

    Error(IMarker.SEVERITY_ERROR), Warning(IMarker.SEVERITY_WARNING), Info(IMarker.SEVERITY_INFO);

    public final Integer value;

    private MarkerSeverity(int severity) {
        this.value = Integer.valueOf(severity);
    }

    /**
     * @return never return null. In case given argument is unknown or null,
     *         returns {@link #Warning} severity.
     */
    public static MarkerSeverity get(String markerSeverity) {
        if (markerSeverity == null || markerSeverity.length() == 0) {
            return Warning;
        }
        try {
            return valueOf(markerSeverity);
        } catch (Exception e) {
            return Warning;
        }
    }
}
