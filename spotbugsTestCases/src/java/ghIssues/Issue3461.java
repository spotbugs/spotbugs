package ghIssues;

import java.util.Random;

public class Issue3461 {
	private Random random = new Random();

	public int showBug() {
		double randomValue = random.nextDouble();
		int coercedValue = (int) randomValue;
		
		return coercedValue;
	}
}
