import java.util.Random;

class BadRandomInt {

	static Random r = new Random();

	int nextInt(int n) {
		return (int) (r.nextDouble() * n);
	}
}
