/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, Mike Fagan <mfagan@tde.com>
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

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Statistics resulting from analyzing a project.
 */
public class ProjectStats {
	private HashMap<String, PackageStats> packageStatsMap;
	private int totalErrors;
	private int totalClasses;

	/**
	 * Constructor. Creates an empty object.
	 */
	public ProjectStats() {
		this.packageStatsMap = new HashMap<String, PackageStats>();
		this.totalErrors = 0;
		this.totalClasses = 0;
	}

	/**
	 * Get the number of classes analyzed.
	 */
	public int getNumClasses() {
		return totalClasses;
	}

	/**
	 * Report that a class has been analyzed.
	 *
	 * @param className   the full name of the class
	 * @param isInterface true if the class is an interface
	 */
	public void addClass(String className, boolean isInterface) {
		String packageName;
		int lastDot = className.lastIndexOf('.');
		if (lastDot < 0)
			packageName = "";
		else
			packageName = className.substring(0, lastDot);
		PackageStats stat = getPackageStats(packageName);
		stat.addClass(className, isInterface);
		totalClasses++;
	}

	/**
	 * Called when a bug is reported.
	 */
	public void addBug(BugInstance bug) {
		PackageStats stat = getPackageStats(bug.getPrimaryClass().getPackageName());
		stat.addError(bug);
		totalErrors++;
	}

	/**
	 * Convert to an XML element.
	 */
	public Element toElement(Branch parent) {
		Element root = parent.addElement("FindBugsSummary");
		DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		Date date = new Date();
		root.addAttribute("timestamp", df.format(date));
		root.addAttribute("total_classes", String.valueOf(totalClasses));
		root.addAttribute("total_bugs", String.valueOf(totalErrors));
		root.addAttribute("num_packages", String.valueOf(packageStatsMap.size()));

		Iterator<PackageStats> i = packageStatsMap.values().iterator();
		while (i.hasNext()) {
			PackageStats stats = i.next();
			stats.toElement(root);
		}

		return root;
	}

	/**
	 * Report statistics as an XML document to given output stream.
	 */
	public void reportSummary(OutputStream out) {
		Document document = DocumentHelper.createDocument();

		toElement(document);

		try {
			XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
			writer.write(document);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Transform summary information to HTML.
	 *
	 * @param htmlWriter the Writer to write the HTML output to
	 */
	public void transformSummaryToHTML(Writer htmlWriter)
	        throws IOException, TransformerException {

		ByteArrayOutputStream summaryOut = new ByteArrayOutputStream(8096);
		reportSummary(summaryOut);
		String summaryXML = summaryOut.toString();


		StreamSource in = new StreamSource(new StringReader(summaryXML));
		StreamResult out = new StreamResult(htmlWriter);
		InputStream xslInputStream = this.getClass().getClassLoader().getResourceAsStream("summary.xsl");
		if (xslInputStream == null)
			throw new IOException("Could not load summary stylesheet");
		StreamSource xsl = new StreamSource(xslInputStream);

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer(xsl);
		transformer.transform(in, out);

		Reader rdr = in.getReader();
		if (rdr != null)
			rdr.close();
		htmlWriter.close();
		InputStream is = xsl.getInputStream();
		if (is != null)
			is.close();
	}

	private PackageStats getPackageStats(String packageName) {
		PackageStats stat = packageStatsMap.get(packageName);
		if (stat == null) {
			stat = new PackageStats(packageName);
			packageStatsMap.put(packageName, stat);
		}
		return stat;
	}
}
