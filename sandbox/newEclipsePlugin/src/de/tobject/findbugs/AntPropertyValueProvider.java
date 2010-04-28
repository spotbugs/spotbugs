package de.tobject.findbugs;

import org.eclipse.ant.core.IAntPropertyValueProvider;

public class AntPropertyValueProvider implements IAntPropertyValueProvider {

	public String getAntPropertyValue(String antPropertyName) {
		if (antPropertyName.equals("findbugs.home")) {
			String home = FindbugsPlugin.getFindBugsEnginePluginLocation();
			return home;
		}
		throw new IllegalArgumentException("No property " + antPropertyName);
	}

}
