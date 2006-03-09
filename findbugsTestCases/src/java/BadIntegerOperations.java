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

	/** (bug 1291650) false positive: Bitwise OR of signed byte value computed */
	void boolFalse(String[] args) {
		final boolean[] values = { false };
		values[0] |= (args.length > 0);
	}

	void boolTrue(String[] args) {
		final boolean[] values = { true };
		values[0] |= (args.length > 0);
	}

	void byteZero(final byte len) {
		final byte[] values = { 0 };
		values[0] |= len;
	}

	
	Random r = new Random();

	int getRandomElement(int a[]) {
		return a[r.nextInt() % a.length];
	}


	public void operationsOnBooleanArrays(String[] args)
	{
	final boolean[] values = { false };
	values[0] |= (args.length > 0);
	}


	public BadIntegerOperations() {
	}
}
