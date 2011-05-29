/*
 * Contributions to FindBugs
 * Copyright (C) 2011, Andrei Loskutov
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

import edu.umd.cs.findbugs.Plugin;

public class PluginElement {

    private final Plugin plugin;
    private boolean enabled;

    public PluginElement(Plugin plugin, boolean enabled) {
        this.plugin = plugin;
        this.setEnabled(enabled);
    }

    @Override
    public String toString() {
        return plugin.toString() + (isEnabled() ? "" : " (disabled)");
    }

    public String getPath() {
        return plugin.getPluginId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PluginElement) {
            return plugin.equals(((PluginElement) obj).plugin) && enabled == ((PluginElement) obj).enabled;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return plugin.hashCode();
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
