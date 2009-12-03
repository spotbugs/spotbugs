package sfBugs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class Bug2893480 {

    private String message = "Actie";

    private AbstractAction a = new Actie1();
    private AbstractAction b = new Actie2();


    public void test() {
        a.actionPerformed(null);
        b.actionPerformed(null);
    }


    private final class Actie1 extends AbstractAction {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            System.out.println(message);
        }

    }

    private final class Actie2 extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            System.out.println(message);
        }

    }
}
