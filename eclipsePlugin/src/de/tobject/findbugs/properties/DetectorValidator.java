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

import java.io.File;
import java.net.URI;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginLoader;
import edu.umd.cs.findbugs.PluginLoader.Summary;

/**
 * Quickly validates additional detector packages available for Findbugs.
 *
 * @author Andrei Loskutov
 */
public class DetectorValidator {

    public static class ValidationStatus extends Status {
        public static final String UNKNOWN_VALUE = "?";
        private final Summary sum;

        public ValidationStatus(int severity, String message, Summary sum, Throwable t) {
            super(severity, FindbugsPlugin.PLUGIN_ID, message, t);
            this.sum = sum == null ? new Summary(UNKNOWN_VALUE, UNKNOWN_VALUE, UNKNOWN_VALUE, UNKNOWN_VALUE) : sum;
        }

        /**
         * @return the sum
         */
        @Nonnull
        public Summary getSummary() {
            return sum;
        }
    }

    public DetectorValidator() {
        super();
    }

    /**
     *
     * @param path
     *            non null, full abstract path in the local file system
     * @return {@link Status#OK_STATUS} in case that given path might be a valid
     *         FindBugs detector package (jar file containing bugrank.txt,
     *         findbugs.xml, messages.xml and at least one class file). Returns
     *         error status in case anything goes wrong or file at given path is
     *         not considered as a valid plugin.
     */
    @Nonnull
    public ValidationStatus validate(String path) {
        File file = new File(path);
        Summary sum = null;
        try {
            sum = PluginLoader.validate(file);
        } catch (IllegalArgumentException e) {
            if(FindbugsPlugin.getDefault().isDebugging()) {
                e.printStackTrace();
            }
            return new ValidationStatus(IStatus.ERROR,
                    "Invalid FindBugs plugin archive: " + e.getMessage(), sum, e);
        }
        Plugin loadedPlugin = Plugin.getByPluginId(sum.id);
        URI uri = file.toURI();
        if(loadedPlugin != null && !uri.equals(loadedPlugin.getPluginLoader().getURI())
                && loadedPlugin.isGloballyEnabled()) {
            return new ValidationStatus(IStatus.ERROR, "Duplicated FindBugs plugin: " + sum.id + ", already loaded from: "
                    + loadedPlugin.getPluginLoader().getURI(), sum, null);
        }
        return new ValidationStatus(IStatus.OK, Status.OK_STATUS.getMessage(), sum, null);
    }
}
