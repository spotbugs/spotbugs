/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.util.LaunchBrowser;

/**
 * @author pugh
 */
public class TestDesktopIntegration extends JPanel {

	public static void main(String args[]) throws Exception {
		url = new URL("http://www.sv.com");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private static void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("TextSamplerDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new TestDesktopIntegration());

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	JTextArea console = new JTextArea(24, 80);

	static URL url;

	class ConsoleWriter extends Writer {

		@Override
		public void close() throws IOException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.Writer#flush()
		 */
		@Override
		public void flush() throws IOException {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.Writer#write(char[], int, int)
		 */
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {

			console.append(new String(cbuf, off, len));

		}

	}

	PrintWriter writer = new PrintWriter(new ConsoleWriter());

	public TestDesktopIntegration() {
		setLayout(new BorderLayout());
		JPanel top = new JPanel();
		top.setLayout(new FlowLayout());
		add(top, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		console.setEditable(false);
		console.setLineWrap(true);
		add(scrollPane);
		JButton desktop = new JButton("Launch via Desktop");
		desktop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {

					writer.println("Launch via desktop");
					LaunchBrowser.viaDesktop(url.toURI());
				} catch (Exception e1) {
					e1.printStackTrace(writer);
				}
			}
		});
		top.add(desktop);

		JButton jnlp = new JButton("Launch via jnlp");
		jnlp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {

					writer.println("Launch via jnlp");
					LaunchBrowser.viaWebStart(url);
				} catch (Exception e1) {
					e1.printStackTrace(writer);
				}
			}
		});
		top.add(jnlp);

		JButton exec = new JButton("Launch via exec");
		exec.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					writer.println("Launch via exec firefox");
					Process p = LaunchBrowser.launchFirefox(url);
					Thread.sleep(3000);
					int exitValue = p.exitValue();
					writer.println("Exit code: " + exitValue);
				} catch (Exception e1) {
					e1.printStackTrace(writer);
				}
			}
		});
		top.add(exec);

		JButton chooseFile = new JButton("Choose file");
		top.add(chooseFile);
		chooseFile.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();
				int retvel = fc.showOpenDialog(TestDesktopIntegration.this);
				if (retvel == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						writer.println("File choosen:");
						writer.println("File path: " + file.getAbsolutePath());
						writer.println("File canonical path: " + file.getCanonicalPath());

						writer.println("File uri: " + file.toURI());

						writer.println("File url: " + file.toURL());
					} catch (Exception e1) {
						e1.printStackTrace(writer);
					}
				}
			}
		});

		writer.println("System properties:");
		for (Map.Entry e : System.getProperties().entrySet()) {
			writer.println((e.getKey() + "=" + e.getValue()));
		}

		try {
			Class<?> serviceManagerClass = Class.forName("javax.jnlp.ServiceManager");
			Method getServiceNamesMethod = serviceManagerClass.getMethod("getServiceNames", new Class[] {});
			Method lookupMethod = serviceManagerClass.getMethod("lookup", new Class[] { String.class });
			String[] serviceNames = (String[]) getServiceNamesMethod.invoke(null, new Object[] {});
			writer.println("JNLP service providers:");
			for (String s : serviceNames) {
				Object o = lookupMethod.invoke(null, new Object[] { s });
				writer.println(s + " = " + o.getClass().getName());
			}
		} catch (Exception e) {
			writer.println("unable to get JNLP service provider:");
			e.printStackTrace(writer);

		}

	}

}
