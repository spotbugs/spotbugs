import javax.swing.*;
import java.awt.*;

public class Swing1 {
	public static void main(String args[]) {
		JFrame frame = new JFrame();
		frame.setTitle("Title");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton button = new JButton();
		button.setText("Hello, World!");
		frame.getContentPane().add(button, BorderLayout.CENTER);
		frame.setSize(200, 100);
		frame.pack();
		frame.setVisible(true);
		frame.show();
	}
}
