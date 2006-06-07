package edu.umd.cs.findbugs.bluej;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;

@SuppressWarnings("serial")
public class ResultsFrame extends JFrame
{
	private static final String[] columnNames = {"File", "Line", "Description"};
	private static final int[] columnWidths = {150, 50, 475};
	
	private JEditorPane description;
	
	public ResultsFrame()
	{
		this(new SortedBugCollection());
	}
	
	public ResultsFrame(final SortedBugCollection bugs)
	{
		final ArrayList<BugInstance> bugList = new ArrayList<BugInstance>(bugs.getCollection());
		
		final JTable table = new JTable(new MyTableModel(bugList));
		
		for (int i = 0; i < columnNames.length; i++)
			table.getColumn(columnNames[i]).setPreferredWidth(columnWidths[i]);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent evt)
			{
				description.setText(bugList.get(table.getSelectedRow()).getBugPattern().getDetailHTML());
			}	
		});
		
		JScrollPane topScroll = new JScrollPane(table);
		topScroll.setPreferredSize(new Dimension(675, 275));
			
		
		description = new JEditorPane();
		description.setContentType("text/html");
		JScrollPane bottomScroll = new JScrollPane(description);
		bottomScroll.setPreferredSize(new Dimension(675, 100));
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScroll, bottomScroll);
		setContentPane(splitPane);
		
		pack();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}
	
//	private ArrayList<String[]> parseBugInstances(SortedBugCollection bugs)
//	{
//		ArrayList<String[]> result = new ArrayList<String[]>();
//		Iterator<BugInstance> i = bugs.iterator();
//		while (i.hasNext())
//		{
//			BugInstance bug = i.next();
//			result.add(new String[3]);
//			result.get(result.size() - 1)[0] = bug.getPrimarySourceLineAnnotation().getSourceFile();
//			result.get(result.size() - 1)[1] = String.valueOf(bug.getPrimarySourceLineAnnotation().getStartLine());
//			result.get(result.size() - 1)[2] = bug.getMessageWithoutPrefix();
//		}
//		return result;
//	}
	
	private class MyTableModel extends AbstractTableModel
	{
		private ArrayList<BugInstance> bugList;
		
		public MyTableModel(ArrayList<BugInstance> bugList)
		{
			this.bugList = bugList;
		}
		
		public int getRowCount()
		{
			return bugList.size();
		}

		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		}
		
		public Object getValueAt(int row, int column)
		{
			switch (column)
			{
			case 0:
				return bugList.get(row).getPrimarySourceLineAnnotation().getSourceFile();
			case 1:
				return bugList.get(row).getPrimarySourceLineAnnotation().getStartLine();
			case 2:
				return bugList.get(row).getMessageWithoutPrefix();
			default:
				throw new ArrayIndexOutOfBoundsException("Column " + column + " must be < 3");
			}
		}
		
	}
}
