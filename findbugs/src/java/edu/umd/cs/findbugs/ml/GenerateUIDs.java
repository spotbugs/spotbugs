/*
 * Machine Learning support for FindBugs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.xml.Dom4JXMLOutput;

/**
 * Add uid attributes to BugInstances in a BugCollection.
 * A uid is an integer that uniquely identifies a BugInstance
 * in a BugCollection.
 * Right now this is only used in machine learning experiments.
 * 
 * @author David Hovemeyer
 */
public class GenerateUIDs {
	private BugCollection bugCollection;
	@NonNull private Project project;
	private String inputFilename;
	private String outputFilename;

	public GenerateUIDs(String inputFilename, String outputFilename) {
		this.bugCollection = new SortedBugCollection();
		this.project = new Project();
		this.inputFilename = inputFilename;
		this.outputFilename = outputFilename;
	}

	@SuppressWarnings("unchecked")
    public void execute() throws IOException, DocumentException {
		InputStream in = null;
		try {
		if (inputFilename.equals("-")) {
			in = System.in;
		} else {
			in = new BufferedInputStream(new FileInputStream(inputFilename));
			if (inputFilename.endsWith(".gz"))
				in = new GZIPInputStream(in);
		}


		bugCollection.readXML(in, project);
		in = null;
		} finally {
			if (in != null) in.close();
		}
		Document document = DocumentFactory.getInstance().createDocument();
		Dom4JXMLOutput xmlOutput = new Dom4JXMLOutput(document);
		bugCollection.writeXML(xmlOutput, project);

		int count = 0;

		List<Element> bugInstanceList = document.selectNodes("/BugCollection/BugInstance");
		for (Element element : bugInstanceList) {
			Attribute uidAttr = element.attribute("uid");
			if (uidAttr == null) {
				element.addAttribute("uid", Integer.toString(count++));
			}
		}

		OutputStream out;
		if (outputFilename.equals("-")) {
			out = System.out;
		} else {
			out = new BufferedOutputStream(new FileOutputStream(outputFilename));
		}
		XMLWriter xmlWriter = new XMLWriter(out, OutputFormat.createPrettyPrint());
		xmlWriter.write(document);
		xmlWriter.close();
	}

	public static void main(String[] args) throws IOException, DocumentException {
		if (args.length != 2) {
			System.err.println("Usage: " + GenerateUIDs.class.getName() +
					" <input file> <output file>");
			System.exit(1);
		}

		String inputFilename = args[0];
		String outputFilename = args[1];

		GenerateUIDs generateUIDs = new GenerateUIDs(inputFilename, outputFilename);
		generateUIDs.execute();
	}
}
