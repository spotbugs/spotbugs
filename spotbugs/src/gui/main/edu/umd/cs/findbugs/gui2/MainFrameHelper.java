package edu.umd.cs.findbugs.gui2;

import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import edu.umd.cs.findbugs.gui.AnnotatedString;

public class MainFrameHelper {
    public static JButton newButton(String key, String name) {
        JButton b = new JButton();
        AnnotatedString.localiseButton(b, key, name, false);
        return b;
    }

    public static JMenuItem newJMenuItem(String key, String string, int vkF) {
        JMenuItem m = new JMenuItem();
        AnnotatedString.localiseButton(m, key, string, false);
        m.setMnemonic(vkF);
        return m;

    }

    public static JMenuItem newJMenuItem(String key, String string) {
        JMenuItem m = new JMenuItem();
        AnnotatedString.localiseButton(m, key, string, true);
        return m;

    }

    public static JMenu newJMenu(String key, String string) {
        JMenu m = new JMenu();
        AnnotatedString.localiseButton(m, key, string, true);
        return m;
    }

    public static boolean isMacLookAndFeel() {
        String name = UIManager.getLookAndFeel().getClass().getName();
        return name.startsWith("com.apple");
    }

    public static void attachAcceleratorKey(JMenuItem item, int keystroke) {
        attachAcceleratorKey(item, keystroke, 0);
    }

    public static void attachAcceleratorKey(JMenuItem item, int keystroke, int additionalMask) {
        // As far as I know, Mac is the only platform on which it is normal
        // practice to use accelerator masks such as Shift and Alt, so
        // if we're not running on Mac, just ignore them
        if (!MainFrame.MAC_OS_X && additionalMask != 0) {
            return;
        }

        item.setAccelerator(KeyStroke.getKeyStroke(keystroke, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | additionalMask));
    }

    public static void attachAcceleratorKeyNoCtrl(JMenuItem item, int keyEvent) {
        item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, 0));
    }
}
