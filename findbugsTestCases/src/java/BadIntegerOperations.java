import java.util.Random;

class BadIntegerOperations {

	int getBytesAsInt(byte b[]) {
		int l = 0;
		for (int i = 0; i < b.length; i++)
			l = (l << 8) | b[i];
		return l;
	}

	long getBytesAsLong(byte b[]) {
		long l = 0;
		for (int i = 0; i < b.length; i++)
			l = (l << 8) | b[i];
		return l;
	}

	long getBytesAsLong2(byte b[]) {
		long l = 0;
		for (int i = 0; i < b.length; i++)
			l = b[i] | (l << 8);
		return l;
	}

	int getBytesAsInt2(byte b[]) {
		int l = 0;
		for (int i = 0; i < b.length; i++)
			l = b[i] | (l << 8);
		return l;
	}

	Random r = new Random();

	int getRandomElement(int a[]) {
		return a[r.nextInt() % a.length];
	}

	public BadIntegerOperations() {
	}
}
