package edu.umd.cs.findbugs.bluej;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import bluej.extensions.BlueJ;
import bluej.extensions.PreferenceGenerator;

@SuppressWarnings("serial")
public class FindBugsPreferences extends JPanel implements PreferenceGenerator {

	private	BlueJ bluej;
	private static JRadioButton[] radioList = new JRadioButton[3];
	
	private static String[] radioDescription = {"Compile all classes not already compiled.", 
			"Do not compile classes not already compiled.",
			"Show dialogue box."};
	
	//Warning if this is changed must check and/or change loadValues()
	//and RunFindbugs method compileSucceeded()
	static String[] radioCommand = {"Compile", "DoNotCompile", "DialogueBox"};
	
	static final String PROFILE_LABEL = "FindBugsPreference";
	private ButtonGroup compileGroup;
	
	public	FindBugsPreferences(BlueJ bluej){
		this.bluej = bluej;
		compileGroup = new ButtonGroup();
				
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		add(new JLabel("When FindBugs runs:"));
		
		for(int i = 0; i < radioList.length; i++){
			radioList[i] = new JRadioButton(radioDescription[i]);
			radioList[i].setActionCommand(radioCommand[i]);
			compileGroup.add(radioList[i]);
			add(radioList[i]);
		}
		
		loadValues();
	}
	
	public JPanel getPanel() {
		return this;
	}

	public void loadValues() {
		String strButton = bluej.getExtensionPropertyString(PROFILE_LABEL,"");
		
		if(strButton.equals(""))
			radioList[2].setSelected(true);
		else
			for(int i = 0; i < radioCommand.length; i++){
				if(strButton.equalsIgnoreCase(radioCommand[i]))
					radioList[i].setSelected(true);
			}
	}

	public void saveValues() {
		bluej.setExtensionPropertyString(PROFILE_LABEL, compileGroup.getSelection().getActionCommand());
	}

}
