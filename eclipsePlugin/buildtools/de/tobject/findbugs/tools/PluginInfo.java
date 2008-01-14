package de.tobject.findbugs.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import edu.umd.cs.findbugs.Version;

/**
 * Extract information (plugin id, version, etc.)
 * from the plugin's MANIFEST.MF file,
 * and print the information as java properties.
 * This allows our Ant build.xml file to figure out
 * what the deployable plugin zip file should be called.
 */
public class PluginInfo {

	static final SimpleDateFormat eclipseDateFormat = new SimpleDateFormat("yyyyMMdd");

	public static void main(String[] args) throws Exception {
		if (args.length < 1 || args.length > 2) {
			System.err.println("Usage: " + PluginInfo.class.getName() + " <MANIFEST.MF [MANIFEST.MF] file>");
			System.exit(1);
		}

		Manifest manifest = new Manifest(new FileInputStream(args[0]));

		String date = Version.ECLIPSE_DATE;
		String version = getValue(manifest, "Bundle-Version");
		String expectedVersion = Version.RELEASE_BASE+".qualifier";
		if (!version.equals(expectedVersion)) {
			throw new IllegalStateException("plugin gives Eclipse version as " + version + ", FindBugs gives version as " + expectedVersion);
		}
		emitProperty(manifest, "plugin.id", "Bundle-SymbolicName");
		String modifiedVersion = version.replaceFirst("qualifier", date);
		setValue(manifest, "Bundle-Version", modifiedVersion);
		emitProperty(manifest, "plugin.version", "Bundle-Version");
		if (args.length == 2) {
			File outputFile = new File(args[1]);
			outputFile.getParentFile().mkdirs();
			outputFile.delete();
			outputFile.createNewFile();
			serializetoXML(manifest, new FileOutputStream(args[1]));
		}
	}

	public static void serializetoXML(Manifest manifest, OutputStream out)
			throws Exception {
		manifest.write(out);
		out.close();
	}


	private static void emitProperty(Manifest manifest, String propName, String propNameToSearch) {
		System.out.println(propName + "=" + getValue(manifest, propNameToSearch));
	}

	private static String getValue(Manifest manifest, String propName) {
		String value = manifest.getMainAttributes().getValue(propName);
		if (value == null) {
			throw new RuntimeException("No value found for property " + propName);
		}
		// replace compound value with the first segment: edu.umd.cs.findbugs.plugin.eclipse; singleton:=true
		if(value.indexOf(';') > 0) {
			value = value.substring(0, value.indexOf(';'));
		}
		return  value;
	}

	private static void setValue(Manifest manifest, String propName, String value) {
		Attributes attributes = manifest.getMainAttributes();
		if (attributes == null) {
			throw new RuntimeException("No value found for property " + propName);
		}
		attributes.putValue(propName, value);
	}
}
