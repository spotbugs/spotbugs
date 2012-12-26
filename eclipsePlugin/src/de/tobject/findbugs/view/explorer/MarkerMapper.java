/*
 * Contributions to FindBugs
 * Copyright (C) 2012, Andrey Loskutov
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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

/**
 *
 * @param <Identifier>
 *            the specific identifier of given marker inside the given group
 *            type
 */
abstract class MarkerMapper<Identifier> {

    /**
     * there is no mapping possible for current mapping type
     */
    public static final MarkerMapper<Void> NO_MAPPING = new MarkerMapper<Void>() {

        @Override
        void setType(GroupType type) {
            throw new IllegalStateException("Setting the type not allowed");
        }

        @Override
        Void getIdentifier(IMarker marker) {
            return null;
        }

        @Override
        GroupType getType() {
            return GroupType.Undefined;
        }

        @Override
        String getShortDescription(Void id) {
            return "Undefined";
        }

        @Override
        String getDebugDescription(IMarker marker) {
            return getShortDescription(null);
        }

    };

    private GroupType type;

    MarkerMapper() {
        super();
    }

    void setType(GroupType type) {
        if (this.type != null) {
            throw new IllegalStateException("Re-setting the type not allowed");
        }
        if (type == null) {
            throw new IllegalStateException("Null type not allowed");
        }
        this.type = type;
    }

    /**
     * @param marker
     *            non null marker
     * @return the specific identifier for given marker for the current mapping
     *         type, or null if the mapping cannot be created
     */
    abstract Identifier getIdentifier(IMarker marker);

    abstract String getShortDescription(Identifier id);

    /**
     * @param marker non null
     * @return the raw data from current marker for debug purposes
     */
    abstract String getDebugDescription(IMarker marker) throws CoreException;

    GroupType getType() {
        return type;
    }
}
