import java.util.Random;

class BadRandomInt {

	static Random r = new Random();

	int nextInt(int n) {
		return (int) (r.nextDouble() * n);
	}


	int nextInt() {
		return (int) (r.nextDouble() * 100);
	}


	int nextInt2(int n) {
		return (int) (n * r.nextDouble());
	}


	int nextInt2() {
		return (int) (100 * r.nextDouble());
	}

	// FIXME: We should generate a warning here about single-use random number generates
	static int randomInt(int n) {
		Random ran = new Random();  
		return  ran.nextInt(n);
	}

//   FIXME: We should generate a warning here about single-use random number generates
	static int randomInt2(int n) {
		return  new Random().nextInt(n);
	}
//  FIXME: We should generate a warning here about single-use random number generates
	static int randomInt3() {
		return  new Random().nextInt();
	}
}
