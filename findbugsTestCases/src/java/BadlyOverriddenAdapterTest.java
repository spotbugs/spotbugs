import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class BadlyOverriddenAdapterTest extends JFrame {
	public BadlyOverriddenAdapterTest() {
		addWindowListener(new WindowAdapter() {
			@SuppressWarnings("DM_EXIT")
			public void windowClosing() {
				dispose();
				System.exit(0);
			}
		});

		Container cp = getContentPane();
		cp.add(new JButton("Click Me"));
	}
}

class GoodlyOverridenAdapterTest extends JFrame {
	public GoodlyOverridenAdapterTest() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing() {
				dispose();
				System.exit(0);
			}

			@Override
            public void windowClosing(WindowEvent we) {
				windowClosing();
			}
		});

		Container cp = getContentPane();
		cp.add(new JButton("Click Me"));
	}
}