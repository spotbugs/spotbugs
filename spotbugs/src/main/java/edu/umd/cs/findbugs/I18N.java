/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;


/**
 * Singleton responsible for returning localized strings for information
 * returned to the user.
 *
 * @author David Hovemeyer
 */
public class I18N {
    private static final boolean DEBUG = SystemProperties.getBoolean("i18n.debug");

    public static final Locale defaultLocale = Locale.getDefault();

    private final ResourceBundle annotationDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsAnnotationDescriptions",
            defaultLocale);

    /**
     * used if local one can't be found
     */
    private final ResourceBundle englishAnnotationDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsAnnotationDescriptions",
            Locale.ENGLISH);

    I18N() {
        super();
    }

    private static I18N theInstance = new I18N();

    /**
     * Get the single object instance.
     */
    public static I18N instance() {
        return theInstance;
    }

    /**
     * Get a message string. This is a format pattern for describing an entire
     * bug instance in a single line.
     *
     * @param key
     *            which message to retrieve
     *
     *
     */
    @Deprecated
    public @Nonnull String getMessage(String key) {
        BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(key);
        if (bugPattern == null) {
            return L10N.getLocalString("err.missing_pattern", "Error: missing bug pattern for key") + " " + key;
        }
        return bugPattern.getAbbrev() + ": " + bugPattern.getLongDescription();
    }

    /**
     * Get a short message string. This is a concrete string (not a format
     * pattern) which briefly describes the type of bug, without mentioning
     * particular a particular class/method/field.
     *
     * @param key
     *            which short message to retrieve
     */
    public @Nonnull String getShortMessage(String key) {
        BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(key);
        if (bugPattern == null) {
            return L10N.getLocalString("err.missing_pattern", "Error: missing bug pattern for key") + " " + key;
        }
        return bugPattern.getAbbrev() + ": " + bugPattern.getShortDescription();
    }

    public @Nonnull String getShortMessageWithoutCode(String key) {
        BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(key);
        if (bugPattern == null) {
            return L10N.getLocalString("err.missing_pattern", "Error: missing bug pattern for key") + " " + key;
        }
        return bugPattern.getShortDescription();
    }

    /**
     * Get an HTML document describing the bug pattern for given key in detail.
     *
     * @param key
     *            which HTML details for retrieve
     */
    public @Nonnull String getDetailHTML(String key) {
        BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(key);
        if (bugPattern == null) {
            return L10N.getLocalString("err.missing_pattern", "Error: missing bug pattern for key") + " " + key;
        }
        return bugPattern.getDetailHTML();
    }

    /**
     * Get an annotation description string. This is a format pattern which will
     * describe a BugAnnotation in the context of a particular bug instance. Its
     * single format argument is the BugAnnotation.
     *
     * @param key
     *            the annotation description to retrieve
     */
    public String getAnnotationDescription(String key) {
        try {
            return annotationDescriptionBundle.getString(key);
        } catch (MissingResourceException mre) {
            if (DEBUG) {
                return "TRANSLATE(" + key + ") (param={0}}";
            } else {
                try {
                    return englishAnnotationDescriptionBundle.getString(key);
                } catch (MissingResourceException mre2) {
                    return key + " {0}";
                }
            }
        }
    }

    /**
     * Get a description for given "bug type". FIXME: this is referred to
     * elsewhere as the "bug code" or "bug abbrev". Should make the terminology
     * consistent everywhere. In this case, the bug type refers to the short
     * prefix code prepended to the long and short bug messages.
     *
     * @param shortBugType
     *            the short bug type code
     * @return the description of that short bug type code means
     */
    public @Nonnull String getBugTypeDescription(String shortBugType) {
        BugCode bugCode = DetectorFactoryCollection.instance().lookupBugCode(shortBugType);
        if (bugCode == null) {
            return L10N.getLocalString("err.missing_code", "Error: missing bug code for key") + " " + shortBugType;
        }
        return bugCode.getDescription();
    }

    /**
     * Get the description of a bug category. Returns the category if no
     * description can be found.
     *
     * @param category
     *            the category
     * @return the description of the category
     */
    public String getBugCategoryDescription(String category) {
        BugCategory bc = DetectorFactoryCollection.instance().getBugCategory(category);
        return (bc != null ? bc.getShortDescription() : category);
    }
}
