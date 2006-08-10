package awt;

import java.awt.Menu;
import java.awt.MenuBar;

public class MyMenuBar extends MenuBar {
	Menu helpMenu;

	@Override
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
