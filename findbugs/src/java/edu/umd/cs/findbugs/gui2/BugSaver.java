package edu.umd.cs.findbugs.gui2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;

/**
 * Save bugs here, uses SortedBugCollection.writeXML()
 * @author Dan
 *
 */
public class BugSaver {
	
	private static String lastPlaceSaved;
	public static void saveBugs(OutputStream out, BugSet data, Project p)
	{
		SortedBugCollection col=new SortedBugCollection();
		Iterator<BugLeafNode> iter = data.iterator();
		
		while (iter.hasNext())
		{
			col.add(iter.next().getBug());
		}
		
		try {
			col.writeXML(out,p);
		} catch (IOException e) {
			Debug.println(e);
		}
	}

	public static void saveBugs(File out, BugSet data, Project p)
	{
		try {
			out.createNewFile();
			saveBugs(new FileOutputStream(out),data,p);
			lastPlaceSaved=out.getAbsolutePath();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "An error has occured in saving your file");
		}
	}

	public static String getLastPlaceSaved()
	{
		return lastPlaceSaved;
	}
	
}
