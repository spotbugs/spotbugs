package edu.umd.cs.findbugs.sourceViewer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

public class MyTest extends JPanel {

	JEditorPane textArea;

	JScrollPane scrollPane;

	
	MyTest(String fileName) throws IOException, BadLocationException {
		setLayout(new BorderLayout());
		textArea = new JEditorPane();
		textArea.setPreferredSize(new Dimension(500, 500));
		
		scrollPane = new JScrollPane(textArea);

		add(scrollPane);
	
		JavaSourceDocument source = new JavaSourceDocument(fileName, new FileReader(fileName));

		textArea.setEditorKit(source.getEditorKit());
		textArea.setDocument(source.getDocument());
		for(int i = 1; i < 200; i++)
			if (i%3 ==0 || i % 5 == 0)
				source.getHighlightInformation().setHighlight(i, Color.LIGHT_GRAY);
		
	}

		private static final long serialVersionUID = 0L;

	private static void createAndShowGUI() throws Exception {
		//Create and set up the window.
		JFrame frame = new JFrame("MyTest");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Create and set up the content pane.
		JComponent newContentPane = new MyTest("MyTest.java");
		newContentPane.setOpaque(true); //content panes must be opaque
		frame.setContentPane(newContentPane);

		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					createAndShowGUI();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

}
