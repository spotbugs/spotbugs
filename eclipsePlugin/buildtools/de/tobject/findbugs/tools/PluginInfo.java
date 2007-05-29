package de.tobject.findbugs.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import edu.umd.cs.findbugs.Version;

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
		if (args.length < 1 || args.length > 2) {
			System.err.println("Usage: " + PluginInfo.class.getName() + " <plugin.xml file>");
			System.exit(1);
		}
		
		SAXReader reader = new SAXReader();
		Document document = reader.read(new File(args[0]));
		String date = Version.ECLIPSE_DATE;
        String version = getValue(document, "/plugin/@version");
        String expectedVersion = Version.RELEASE_BASE+".qualifier";
        if (!version.equals(expectedVersion))
            throw new IllegalStateException("plugin gives Eclipse version as " + version + ", FindBugs gives version as " + expectedVersion);
		emitProperty(document, "plugin.id", "/plugin/@id");
		String modifiedVersion = getValue(document, "/plugin/@version").replaceFirst("qualifier", date);
		setValue(document, "/plugin/@version", modifiedVersion );
		emitProperty(document, "plugin.version", "/plugin/@version");
		if (args.length == 2)
			serializetoXML(document, new FileOutputStream(args[1]));
	}

	public static  void serializetoXML(Document doc, OutputStream out) throws Exception {
		   OutputFormat outformat = OutputFormat.createPrettyPrint();
		   XMLWriter writer = new XMLWriter(out, outformat);
		   writer.write(doc);
		   writer.close();
		   out.close();
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
	private static void setValue(Document document, String xpath, String value) {
		Node node = document.selectSingleNode(xpath);
		if (node == null)
			throw new RuntimeException("No node found for xpath " + xpath);
		node.setText(value);
	}
}
