
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class BadlyOverriddenAdapterTest extends JFrame
{
	public BadlyOverriddenAdapterTest()
	{
		addWindowListener( new WindowAdapter()
		{
			public void windowClosing()
			{
				dispose();
				System.exit(0);
			}		
		});
		
		Container cp = getContentPane();
		cp.add( new JButton( "Click Me" ));
	}
}