import java.util.Random;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

class BadRandomInt {

	static Random r = new Random();

	@ExpectWarning("Dm")
	int nextInt(int n) {
		return (int) (r.nextDouble() * n);
	}

	@ExpectWarning("Dm")
	int nextInt() {
		return (int) (r.nextDouble() * 100);
	}

	@ExpectWarning("Dm")
	int nextInt2(int n) {
		return (int) (n * r.nextDouble());
	}

	@ExpectWarning("Dm")
	int nextInt2() {
		return (int) (100 * r.nextDouble());
	}

	// FIXME: We should generate a warning here about single-use random number generates
	@DesireWarning("BC")
	static int randomInt(int n) {
		Random ran = new Random();  
		return  ran.nextInt(n);
	}

	@ExpectWarning("BC")
	static int randomInt2(int n) {
		return  new Random().nextInt(n);
	}
	@ExpectWarning("BC")
	static int randomInt3() {
		return  new Random().nextInt();
	}
}
