package sfBugs;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/*
 * Pattern that can be found in various NetBeans sources
 is to find ancestor of some component in AWT hierarchy
 using SwingUtilities.getAncestorOfClass. Although this
 method returns Container according to method contract
 it is safe to cast it to a class that is used as first
 argument. An example of code looks like this:

 */
public class Bug1557886 {
    public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        JPopupMenu jpm = (JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, c);
    }
}
