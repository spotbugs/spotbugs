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
package de.tobject.findbugs.preferences;

import static de.tobject.findbugs.preferences.FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH;
import static de.tobject.findbugs.preferences.FindBugsConstants.DISABLED_CATEGORIES;
import static de.tobject.findbugs.preferences.FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD;
import static de.tobject.findbugs.preferences.FindBugsConstants.EXPORT_SORT_ORDER;
import static de.tobject.findbugs.preferences.FindBugsConstants.KEY_CACHE_CLASS_DATA;
import static de.tobject.findbugs.preferences.FindBugsConstants.KEY_RUN_ANALYSIS_AS_EXTRA_JOB;
import static de.tobject.findbugs.preferences.FindBugsConstants.ORDER_BY_NAME;
import static de.tobject.findbugs.preferences.FindBugsConstants.RANK_OFCONCERN_MARKER_SEVERITY;
import static de.tobject.findbugs.preferences.FindBugsConstants.RANK_SCARIEST_MARKER_SEVERITY;
import static de.tobject.findbugs.preferences.FindBugsConstants.RANK_SCARY_MARKER_SEVERITY;
import static de.tobject.findbugs.preferences.FindBugsConstants.RANK_TROUBLING_MARKER_SEVERITY;
import static de.tobject.findbugs.preferences.FindBugsConstants.RUN_ANALYSIS_AUTOMATICALLY;
import static de.tobject.findbugs.preferences.FindBugsConstants.RUN_ANALYSIS_ON_FULL_BUILD;
import static de.tobject.findbugs.preferences.FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS;
import static de.tobject.findbugs.preferences.FindBugsConstants.decodeIds;

import java.util.Set;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerSeverity;
import edu.umd.cs.findbugs.config.UserPreferences;

public class FindBugsPreferenceInitializer extends AbstractPreferenceInitializer {

    public FindBugsPreferenceInitializer() {
        super();
    }

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        store.setDefault(EXPORT_SORT_ORDER, ORDER_BY_NAME);
        store.setDefault(DONT_REMIND_ABOUT_FULL_BUILD, false);

        store.setDefault(KEY_CACHE_CLASS_DATA, false);
        store.setDefault(KEY_RUN_ANALYSIS_AS_EXTRA_JOB, true);

        store.setDefault(DISABLED_CATEGORIES, "EXPERIMENTAL,I18N,MALICIOUS_CODE,SECURITY");
        store.setDefault(RUN_ANALYSIS_AUTOMATICALLY, false);
        store.setDefault(RUN_ANALYSIS_ON_FULL_BUILD, false);
        store.setDefault(ASK_ABOUT_PERSPECTIVE_SWITCH, true);
        store.setDefault(SWITCH_PERSPECTIVE_AFTER_ANALYSIS, false);
        store.setDefault(RANK_OFCONCERN_MARKER_SEVERITY, MarkerSeverity.Warning.name());
        store.setDefault(RANK_TROUBLING_MARKER_SEVERITY, MarkerSeverity.Warning.name());
        store.setDefault(RANK_SCARY_MARKER_SEVERITY, MarkerSeverity.Warning.name());
        store.setDefault(RANK_SCARIEST_MARKER_SEVERITY, MarkerSeverity.Warning.name());
        // disabled to be able to distinguish between default and current value
        // store.setDefault(PROJECT_PROPS_DISABLED, true);
    }

    public static UserPreferences createDefaultUserPreferences() {
        UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
        IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        String categoriesStr = store.getString(DISABLED_CATEGORIES);
        Set<String> ids = decodeIds(categoriesStr);
        for (String categoryId : ids) {
            prefs.getFilterSettings().removeCategory(categoryId);
        }
        prefs.setRunAtFullBuild(false);

        // Do not need, as per default the factory default is used if key is
        // missing
        // TODO later we can use custom workspace settings to disable detectors
        // here
        // Iterator<DetectorFactory> iterator =
        // DetectorFactoryCollection.instance().factoryIterator();
        // while (iterator.hasNext()) {
        // DetectorFactory factory = iterator.next();
        // prefs.enableDetector(factory, factory.isDefaultEnabled());
        // }
        return prefs;
    }

    public static void restoreDefaults(IPreferenceStore store) {
        store.setToDefault(EXPORT_SORT_ORDER);
        store.setToDefault(DONT_REMIND_ABOUT_FULL_BUILD);
        store.setToDefault(DISABLED_CATEGORIES);
        store.setToDefault(RUN_ANALYSIS_AUTOMATICALLY);
        store.setToDefault(RUN_ANALYSIS_ON_FULL_BUILD);
        store.setToDefault(ASK_ABOUT_PERSPECTIVE_SWITCH);
        store.setToDefault(SWITCH_PERSPECTIVE_AFTER_ANALYSIS);
        store.setToDefault(RANK_OFCONCERN_MARKER_SEVERITY);
        store.setToDefault(RANK_TROUBLING_MARKER_SEVERITY);
        store.setToDefault(RANK_SCARY_MARKER_SEVERITY);
        store.setToDefault(RANK_SCARIEST_MARKER_SEVERITY);

        store.setToDefault(KEY_CACHE_CLASS_DATA);
        store.setToDefault(KEY_RUN_ANALYSIS_AS_EXTRA_JOB);
    }

}
