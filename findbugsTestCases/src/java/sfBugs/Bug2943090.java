package sfBugs;

import java.util.Random;

public class Bug2943090 {
	
	Random r = new Random();
	
	int getNegativeNumber() {
		return (int) (r.nextDouble() * -100);
	}
	int getNegativeNumber2() {
		return (int) (-r.nextDouble() * 100);
	}
	int getNegativeNumber(int n) {
		return (int) (r.nextDouble() * n);
	}
	
	public static void main(String args[]) {
		Bug2943090 b = new Bug2943090();
		System.out.println(b.getNegativeNumber());
		System.out.println(b.getNegativeNumber2());
		System.out.println(b.getNegativeNumber(-100));
	}

}
