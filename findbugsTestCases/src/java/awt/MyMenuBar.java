package awt;

import java.awt.Menu;
import java.awt.MenuBar;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class MyMenuBar extends MenuBar {
	Menu helpMenu;

	@Override
	@ExpectWarning("RCN")
	public void setHelpMenu(Menu m) {
		synchronized (getTreeLock()) {
			if (helpMenu == m) {
				return;
			}
			if (helpMenu != null) {
				remove(helpMenu);
			}
			if (m.getParent() != this) {
				add(m);
			}
			helpMenu = m;
			if (m != null) {
				super.setHelpMenu(m);
			}
		}
	}
}
