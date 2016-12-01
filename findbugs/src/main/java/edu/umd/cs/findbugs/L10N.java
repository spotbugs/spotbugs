/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2003,2004 University of Maryland
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

/*
 * FindBugsFrame.java
 *
 * Created on March 30, 2003, 12:05 PM
 */

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import edu.umd.cs.findbugs.charsets.UserTextFile;

public class L10N {
    private static final boolean DEBUG = SystemProperties.getBoolean("i18n.debug");

    private static final boolean GENERATE_MISSING_KEYS = SystemProperties.getBoolean("i18n.generateMissingKeys");

    private static ResourceBundle bundle;

    private static ResourceBundle bundle_en;

    private static PrintWriter extraProperties;
    static {
        try {
            if (GENERATE_MISSING_KEYS) {
                try {
                    extraProperties = UserTextFile.printWriter("/tmp/extra.properties");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            bundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.gui.bundle.findbugs");
            bundle_en = ResourceBundle.getBundle("edu.umd.cs.findbugs.gui.bundle.findbugs", Locale.ENGLISH);

        } catch (Exception mre) {
        }
    }

    private L10N() {
    }

    private static String lookup(ResourceBundle b, String key) {
        if (b == null || key == null) {
            throw new MissingResourceException(null, null, null);
        }

        return b.getString(key);
    }

    public static String getLocalString(String key, String defaultString) {
        if (key == null) {
            return "TRANSLATE(" + defaultString + ")";
        }
        try {
            return lookup(bundle, key);
        } catch (MissingResourceException mre) {
            try {
                String en = lookup(bundle_en, key);
                if (DEBUG) {
                    return "TRANSLATE(" + en + ")";
                } else {
                    return en;
                }
            } catch (MissingResourceException mre2) {
                if (extraProperties != null) {
                    extraProperties.println(key + "=" + defaultString);
                    extraProperties.flush();
                }
                // String en = "Default("+defaultString+")";
                String en = defaultString;
                if (DEBUG) {
                    return "TRANSLATE(" + en + ")";
                } else {
                    return en;
                }
            }
        }
    }

}
