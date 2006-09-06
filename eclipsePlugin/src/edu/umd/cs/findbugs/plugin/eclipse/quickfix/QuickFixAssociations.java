package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ui.IMarkerResolution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class QuickFixAssociations {
	private Map<String, Class<IMarkerResolution>[]> fixMap;
	
	public QuickFixAssociations() {
		fixMap = new HashMap<String, Class<IMarkerResolution>[]>();
	}
	
	public void load(File fixFile) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();		
			Document doc = builder.parse(fixFile);
			NodeList list = ((Element)doc.getFirstChild()).getElementsByTagName("BugFix");
			Element item;
			String type;
			Class<IMarkerResolution>[] classes;
			
			for (int ndx = 0; ndx < list.getLength(); ndx++) {
				item = (Element)list.item(ndx);
				type = item.getAttribute("type");

				try {
					classes = loadClasses(item);
					fixMap.put(type, classes);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Class<IMarkerResolution>[] loadClasses(Element item) throws ClassNotFoundException {
		NodeList list = item.getElementsByTagName("Fixer");
		Element classElement;
		Class<IMarkerResolution>[] classes = new Class[list.getLength()];
		String className;
		
		for (int ndx = 0; ndx < list.getLength(); ndx++) {
			classElement = (Element)list.item(ndx);
			className = classElement.getAttribute("classname");

			classes[ndx] = (Class<IMarkerResolution>) Class.forName(className);
		}
		return classes;
	}

	public IMarkerResolution[] createFixers(String type) {
		Class<IMarkerResolution>[] classes = fixMap.get(type);
		IMarkerResolution[] res;
		if (classes == null)
			return new IMarkerResolution[0];
		
		res = new IMarkerResolution[classes.length];
		
		for (int ndx = 0; ndx < classes.length; ndx++) {
			try {
				res[ndx] = classes[ndx].getConstructor(new Class[0]).newInstance(new Object[0]);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		return res;
	}
}
