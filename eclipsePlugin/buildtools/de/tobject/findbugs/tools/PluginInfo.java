package de.tobject.findbugs.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 * Extract information (plugin id, version, etc.)
 * from the plugin's plugin.xml file,
 * and print the information as java properties.
 * This allows our Ant build.xml file to figure out
 * what the deployable plugin zip file should be called.
 */
public class PluginInfo {
	static final SimpleDateFormat eclipseDateFormat = new SimpleDateFormat("yyyyMMdd");
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + PluginInfo.class.getName() + " <plugin.xml file>");
			System.exit(1);
		}
		
		SAXReader reader = new SAXReader();
		Document document = reader.read(new File(args[0]));
		String date = eclipseDateFormat.format(new Date());
		emitProperty(document, "plugin.id", "/plugin/@id");
		System.out.println("plugin.version" + "=" + getValue(document, "/plugin/@version").replace("qualifier", date));
	}

	private static void emitProperty(Document document, String propName, String xpath) {
		System.out.println(propName + "=" + getValue(document, xpath));
	}
	private static String getValue(Document document, String xpath) {
		Node node = document.selectSingleNode(xpath);
		if (node == null)
			throw new RuntimeException("No node found for xpath " + xpath);
		return  node.getText();
	}
}
