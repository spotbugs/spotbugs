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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Quickly validates additional detector packages available for Findbugs.
 *
 * @author Andrei Loskutov
 */
public class DetectorValidator {

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
    @SuppressWarnings("boxing")
    public IStatus validate(String path) {
        File file = new File(path);
        if (!file.getName().endsWith(".jar")) {
            String message = "File " + path + " is not a .jar file";
            return FindbugsPlugin.createErrorStatus(message, new IllegalArgumentException(message));
        }
        if (!file.isFile() || !file.canRead()) {
            String message = "File " + path + " is not a file or is not readable";
            return FindbugsPlugin.createErrorStatus(message, new IllegalArgumentException(message));
        }
        if (file.length() == 0) {
            String message = "File " + path + " is empty";
            return FindbugsPlugin.createErrorStatus(message, new IllegalArgumentException(message));
        }

        boolean seenBugRank = false;
        boolean seenFBxml = false;
        boolean seenFBmessages = false;
        boolean seenClassFile = false;
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = null;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                String name = entry.getName();
                if (!seenClassFile) {
                    seenClassFile |= name.endsWith(".class");
                }
                if (!seenBugRank) {
                    seenBugRank |= name.equals("bugrank.txt");
                }
                if (!seenFBxml) {
                    seenFBxml |= name.equals("findbugs.xml");
                }
                if (!seenFBmessages) {
                    seenFBmessages |= name.equals("messages.xml");
                }
                if (seenFBxml && seenFBmessages) {
                    return Status.OK_STATUS;
                }
                zip.closeEntry();
            }

        } catch (FileNotFoundException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to read jar file " + file);
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to read jar file " + file);
        } finally {
            if (zip != null) {
                try {
                    zip.closeEntry();
                    zip.close();
                } catch (IOException e) {
                    FindbugsPlugin.getDefault().logException(e, "Failed to close jar file " + file);
                }
            }
        }
        String msg = String.format("path: %s, classFiles? %s bugrunk? %s findbugs.xml? %s messages.xml? %s%n", path, seenClassFile, seenBugRank, seenFBxml, seenFBmessages);
        String message = "Invalid detector archive! " + msg;
        if(FindbugsPlugin.getDefault().isDebugging()) {
            System.out.println(message);
        }
        return FindbugsPlugin.createStatus(IStatus.ERROR, message, new IllegalArgumentException(message));
    }
}
