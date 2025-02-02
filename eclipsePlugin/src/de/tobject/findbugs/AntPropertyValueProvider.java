package de.tobject.findbugs;

import org.eclipse.ant.core.IAntPropertyValueProvider;

public class AntPropertyValueProvider implements IAntPropertyValueProvider {

    @Override
    public String getAntPropertyValue(String antPropertyName) {
        if ("findbugs.home".equals(antPropertyName)) {
            return FindbugsPlugin.getFindBugsEnginePluginLocation();
        }
        throw new IllegalArgumentException("No property " + antPropertyName);
    }

}
