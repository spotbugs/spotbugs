/*
 * Contributions to FindBugs
 * Copyright (C) 2006, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 * 
 * Author: Thierry Wyss, Marco Busarello
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ConditionCheck.checkForNull;
import static java.lang.Double.parseDouble;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ui.IMarkerResolution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * Loades the <CODE>BugResolution</CODE>s form a xml document. The document
 * specifies the supported <CODE>BugResolution</CODE>s for the bug-types. An
 * entry has the form:<br><br>
 * <CODE>
 * &lt;bug type="BUG_TYPE"&gt;<br>
 * &nbsp;&nbsp;&lt;resolution classname="bugResolutionClassName"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;attr name="property"&gt;value&lt;/attr&gt;<br>
 * &nbsp;&nbsp;&lt;/resolution&gt;<br>
 * &lt;/bug&gt;<br><br>
 * </CODE>
 * The attributes specified for a <CODE>BugResolution</CODE> supports all
 * primitive types and strings. If an error occurs while loading a 
 * <CODE>BugResolution</CODE>, the error will be reported to the error log. 
 * 
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 * @version 1.0
 */
public class BugResolutionLoader {

	private static final String BUG = "bug"; //$NON-NLS-1$

	private static final String BUG_TYPE = "type"; //$NON-NLS-1$

	private static final String RESOLUTION = "resolution"; //$NON-NLS-1$

	private static final String RESOLUTION_CLASS = "classname"; //$NON-NLS-1$

	private static final String ATTR = "attr"; //$NON-NLS-1$

	private static final String ATTR_NAME = "name"; //$NON-NLS-1$

	private DocumentBuilder builder;

	protected BugResolutionLoader(DocumentBuilder builder) {
		super();
		this.builder = builder;
    }

	public BugResolutionLoader() {
		this(null);
	}

	public BugResolutionAssociations loadBugResolutions(File xmlFile) {
		return loadBugResolutions(xmlFile, null);
	}

	public BugResolutionAssociations loadBugResolutions(File xmlFile, BugResolutionAssociations associations) {
		return loadBugResolutions(parseDocument(xmlFile), associations);
	}

	public BugResolutionAssociations loadBugResolutions(Document document) {
		return loadBugResolutions(document, null);
	}

	/**
	 * Loades the <CODE>BugResolutions</CODE> from the given XML-Document into
	 * the specified <CODE>BugResolutionAssociations</CODE>.
     * 
	 * @param fixesDoc
	 *            the XML-Document that contains the quick-fixes.
	 * @param associations
     *            the <CODE>BugResolutionAssociations</CODE> to load the
	 *            <CODE>BugResolutions</CODE> in.
	 * @return the <CODE>associations</CODE> or a new instance.
	 * @throws IllegalArgumentException
     *             if the <CODE>fixesDoc</CODE> is <CODE>null</CODE>.
	 */
	public BugResolutionAssociations loadBugResolutions(Document fixesDoc, BugResolutionAssociations associations) {
		checkForNull(fixesDoc, "xml document with bug-resolutions");

		if (associations == null) {
			associations = new BugResolutionAssociations();
		}
        NodeList bugFixList = fixesDoc.getElementsByTagName(BUG);
		int length = bugFixList.getLength();
		for (int i = 0; i < length; i++) {
			loadBugResolution((Element) bugFixList.item(i), associations);
        }
		return associations;
	}

	private void loadBugResolution(Element bugFixElement, BugResolutionAssociations associations) {
		String bugType = bugFixElement.getAttribute(BUG_TYPE);
		if (bugType == null) {
            FindbugsPlugin.getDefault().logError("No bug type found in BugResolution-Element.");
			return;
		}

		Set<Class<? extends IMarkerResolution>> resolutionClasses = new HashSet<Class<? extends IMarkerResolution>>();
		Set<IMarkerResolution> resolutions = new HashSet<IMarkerResolution>();

		NodeList resolutionNodes = bugFixElement.getElementsByTagName(RESOLUTION);
		int length = resolutionNodes.getLength();
		for (int i = 0; i < length; i++) {
            Class<? extends IMarkerResolution> resolutionClass = parseBugResolutionClass((Element) resolutionNodes.item(i));
			if (resolutionClass == null) {
				continue;
			}

			Map<String, String> attributes = parseAttributes((Element) resolutionNodes.item(i));
			if (attributes.isEmpty()) {
				resolutionClasses.add(resolutionClass);
            } else {
				IMarkerResolution resolution = instantiateBugResolution(resolutionClass, attributes);
				if (resolution != null) {
					resolutions.add(resolution);
                }
			}
		}

		associations.registerBugResolutions(bugType, resolutionClasses);
		associations.addBugResolutions(bugType, resolutions);
	}

	@CheckForNull
	private IMarkerResolution instantiateBugResolution(Class<? extends IMarkerResolution> resolutionClass, Map<String, String> attributes) {
		try {
            IMarkerResolution resolution = resolutionClass.newInstance();
			loadAttributes(resolution, attributes);
			return resolution;
		} catch (InstantiationException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to instaniate BugResolution '" + 
					TigerSubstitutes.getSimpleName(resolutionClass)+ "'.");
			return null;
		} catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
		}
	}

	private void loadAttributes(IMarkerResolution resolution, Map<String, String> attributes) {
		for (Entry<String, String> attr : attributes.entrySet()) {
			String name = attr.getKey();
            String value = attr.getValue();
			loadAttribute(resolution, name, value);
		}
	}

	private void loadAttribute(IMarkerResolution resolution, String name, String value) {
		Class<? extends IMarkerResolution> typeClass = resolution.getClass();
		for (Method method : typeClass.getMethods()) {
            if (!isPropertySetterMethod(method, name)) {
				continue;
			}
			try {
                Class<?> paramType = method.getParameterTypes()[0];
				Object val = parseValue(value, paramType);
				method.invoke(resolution, val);
				return;
            } catch (IllegalArgumentException e) {
				FindbugsPlugin.getDefault().logException(e, "Failed to parse attribute '" + name + " = " + value + "'.");
			} catch (InvocationTargetException e) {
				FindbugsPlugin.getDefault().logException(e, "Failed to load attribute '" + name + "' = " + value + "'.");
            } catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
    }

	private boolean isPropertySetterMethod(Method method, String propertyName) {
		return method.getParameterTypes().length == 1 && method.getName().startsWith("set") && method.getName().length() > 3 && method.getName().substring(3).equalsIgnoreCase(propertyName);
	}

	/**
	 * Parse a given string value into the specified type. Only primitive types
	 * are currently supported.
     * 
	 * @param value
	 *            the string value
	 * @param type
     *            the type of the parsed value.
	 * @return the parsed value.
	 * @throws IllegalArgumentException
	 *             if the specified <CODE>type</CODE> isn't parseable.
     */
	private <T> Object parseValue(String value, Class<T> type) throws IllegalArgumentException {
		if (String.class == type) {
			return value;
        }
		if (boolean.class == type || Boolean.class == type) {
			return TigerSubstitutes.parseBoolean(value);
		}
        if (int.class == type || Integer.class == type) {
			return parseInt(value);
		}
		if (long.class == type || Long.class == type) {
            return parseLong(value);
		}
		if (float.class == type || Float.class == type) {
			return parseFloat(value);
        }
		if (double.class == type || Double.class == type) {
			return parseDouble(value);
		}
        throw new IllegalArgumentException("Unknown value type '" + type.getName() + "'.");
	}

	@CheckForNull
	private Class<? extends IMarkerResolution> parseBugResolutionClass(Element resolutionElement) {
		String className = resolutionElement.getAttribute(RESOLUTION_CLASS);
        if (className == null) {
			FindbugsPlugin.getDefault().logWarning("Missing a classname in the resolution element.");
			return null;
		}
        try {
			Class<?> resolutionClass = Class.forName(className);
			if (IMarkerResolution.class.isAssignableFrom(resolutionClass)) {
				return TigerSubstitutes.asSubclass(resolutionClass, IMarkerResolution.class);
            }

			FindbugsPlugin.getDefault().logError("BugResolution '" + className + "' not a IMarkerResolution");
		} catch (ClassNotFoundException e) {
			FindbugsPlugin.getDefault().logException(e, "BugResolution '" + className + "' not found.");
        }
		return null;
	}

	private Map<String, String> parseAttributes(Element resolutionElement) {
		Map<String, String> attributes = new Hashtable<String, String>();
		try {
            NodeList attrList = resolutionElement.getElementsByTagName(ATTR);
			int length = attrList.getLength();
			for (int i = 0; i < length; i++) {
				Element attrElement = (Element) attrList.item(i);
                String name = attrElement.getAttribute(ATTR_NAME);
				String value = TigerSubstitutes.getTextContent(attrElement);
				if (false && SystemProperties.ASSERTIONS_ENABLED) {
					if (value.equals(attrElement.getTextContent())) {
                        System.out.println("Expected " + attrElement.getTextContent() + ", got " + value);
					}
				}
				if (name != null && value != null) {
                    attributes.put(name, value);
				}
			}
		} catch (RuntimeException e) {
            AnalysisContext.logError("Error parsing attributes", e);
		}
		return attributes;
	}

	@CheckForNull
	private Document parseDocument(File xmlFile) {
		try {
            if (builder == null) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				builder = factory.newDocumentBuilder();
			}
            return builder.parse(xmlFile);
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to parse xml file '" + xmlFile.getPath() + "'.");
			return null;
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(e, "Failed to read the xml file '" + xmlFile.getPath() + "'.");
            return null;
		}
	}

}
