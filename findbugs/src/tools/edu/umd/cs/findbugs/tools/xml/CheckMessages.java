/*
 * Check FindBugs XML message files
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.tools.xml;

import java.io.File;

import java.net.MalformedURLException;

import org.dom4j.Document;
import org.dom4j.DocumentException;

import org.dom4j.io.SAXReader;

/**
 * Ensure that the XML messages files in a FindBugs plugin
 * are valid and complete.
 */
public class CheckMessages {
	private static class MessagesDoc {
		private String filename;
		private Document document;

		public MessagesDoc(String filename) throws DocumentException, MalformedURLException {
			this.filename = filename;

			File file = new File(filename);
			SAXReader saxReader = new SAXReader();
			this.document = saxReader.read(file);
		}

		public String getFilename() { return filename; }

		public Document getDocument() { return document; }
	}

	private MessagesDoc masterMessagesDoc;

	public CheckMessages(String filename) throws DocumentException, MalformedURLException {
		masterMessagesDoc = new MessagesDoc(filename);
		checkMessages(masterMessagesDoc);
	}

	public void checkMessages(MessagesDoc messagesDoc) throws DocumentException {
	}

	public void checkTranslation(MessagesDoc translatedMessagesDoc) throws DocumentException {
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			System.err.println("Usage: " + CheckMessages.class.getName() +
				" <master messages xml> [<translation xml> ...]");
			System.exit(1);
		}

		String masterFile = argv[0];

		CheckMessages checkMessages = new CheckMessages(masterFile);
		for (int i = 1; i < argv.length; ++i) {
			checkMessages.checkTranslation(new MessagesDoc(argv[i]));
		}
	}
}

// vim:ts=3
