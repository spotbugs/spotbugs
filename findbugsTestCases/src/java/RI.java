import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class SuperRI implements ActionListener, Cloneable {
	public void actionPerformed(ActionEvent ae) {
	}
}

// This is fine
class Sub extends SuperRI {
}

// This is redundant
public class RI extends SuperRI implements ActionListener {
	public static void main(String[] args) {
		RI ri = new RI();
		ri.actionPerformed(new ActionEvent(ri, 0, "Yo"));
	}
}