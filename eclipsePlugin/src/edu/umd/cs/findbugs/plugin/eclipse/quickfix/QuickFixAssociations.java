package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ui.IMarkerResolution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tobject.findbugs.FindbugsPlugin;

public class QuickFixAssociations {
	private Map<String, List<Class<? extends IMarkerResolution>>> fixMap;
	
	public QuickFixAssociations() {
		fixMap = new HashMap<String, List<Class<? extends IMarkerResolution>>>();
	}
	
	public void load(File fixFile) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();		
			Document doc = builder.parse(fixFile);
			NodeList list = ((Element)doc.getFirstChild()).getElementsByTagName("BugFix");
			
			for (int ndx = 0; ndx < list.getLength(); ndx++) {
				Element item = (Element)list.item(ndx);
				String type = item.getAttribute("type");

				try {
					fixMap.put(type,  loadClasses(item));
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

	@SuppressWarnings("unchecked")
	private List<Class<? extends IMarkerResolution>> loadClasses(Element item) throws ClassNotFoundException {
		NodeList list = item.getElementsByTagName("Fixer");

		ArrayList<Class<? extends IMarkerResolution>> classes
			= new ArrayList<Class<? extends IMarkerResolution>>(list.getLength());
		
		
		for (int ndx = 0; ndx < list.getLength(); ndx++) {
			Element classElement = (Element)list.item(ndx);
			String className = classElement.getAttribute("classname");

			Class<?> fixerClass = Class.forName(className);
			if (!IMarkerResolution.class.isAssignableFrom(fixerClass)) {
				FindbugsPlugin.getDefault().logError(className + " is not the name of a IMarkerResolution");
			}
			else classes.add((Class<? extends IMarkerResolution>) fixerClass);
		}
		return classes;
	}

	public IMarkerResolution[] createFixers(String type) {
		List<Class<? extends IMarkerResolution>> classes = fixMap.get(type);
		IMarkerResolution[] res;
		if (classes == null)
			return new IMarkerResolution[0];
		
		res = new IMarkerResolution[classes.size()];
		
		for (int ndx = 0; ndx < classes.size(); ndx++) {
			try {
				res[ndx] = classes.get(ndx).newInstance();
			} catch (IllegalArgumentException e) {
				FindbugsPlugin.getDefault().logException(e, "Could not instantiate " + classes.get(ndx).getName());
			} catch (SecurityException e) {
				FindbugsPlugin.getDefault().logException(e, "Could not instantiate " + classes.get(ndx).getName());
			} catch (InstantiationException e) {
				FindbugsPlugin.getDefault().logException(e, "Could not instantiate " + classes.get(ndx).getName());
			} catch (IllegalAccessException e) {
				FindbugsPlugin.getDefault().logException(e, "Could not instantiate " + classes.get(ndx).getName());
			}
		}
		return res;
	}
}
