import java.util.Random;

class BadRandomInt {

	static Random r = new Random();

	// FIXME: We should recommend use of nextInt here instead
	int nextInt(int n) {
		return (int) (r.nextDouble() * n);
	}
	

	int nextInt() {
		return (int) (r.nextDouble() * 100);
	}
	

	// FIXME: We should recommend use of nextInt here instead
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
}
