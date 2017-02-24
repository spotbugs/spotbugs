package sfBugs;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

public class Bug3031977 {

    public void m() {
        final ArrayList arr = new ArrayList();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                arr.add("done");
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
