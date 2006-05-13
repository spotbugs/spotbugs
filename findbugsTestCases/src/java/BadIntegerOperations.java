import java.util.Random;

class BadIntegerOperations {

	
	boolean lessThanOrEqualToMaxInt(int i) {
		return i <= Integer.MAX_VALUE;
	}
	
	boolean maxIntGreaterThanOrEqualTo(int i) {
		return Integer.MAX_VALUE >= i;
	}

	boolean lessThanMaxInt(int i) {
		return i < Integer.MAX_VALUE;
	}
	
	boolean greaterThanMinInt(int i) {
		return i > Integer.MIN_VALUE;
	}
	
	boolean greaterThanOrEqualToMinInt(int i) {
		return i >= Integer.MIN_VALUE;
	}
	boolean minIntLessThanOrEqualTo(int i) {
		return Integer.MIN_VALUE <= i;
	}
	
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

	int shiftInByte(int partialResult, byte b[], int i) {
		return  partialResult << 8 | b[i];
	}
	
	int shiftInByte2(int partialResult, byte b[], int i) {
		return  b[i] | partialResult << 8;
	}
	
	int orInByte(int partialResult, byte b[], int i) {
		return  (partialResult &0xffffff00) | b[i];
	}
	
	int orInByte2(int partialResult, byte b[], int i) {
		return  b[i] | (partialResult &0xffffff00) ;
	}
	
	long shiftInByte(long partialResult, byte b[], int i) {
		return  partialResult << 8 | b[i];
	}
	
	long shiftInByte2(long partialResult, byte b[], int i) {
		return  b[i] | partialResult << 8;
	}
	
	long orInByte(long partialResult, byte b[], int i) {
		return  (partialResult &0xffffffffffffff00L) | b[i];
	}
	
	long orInByte2(long partialResult, byte b[], int i) {
		return  b[i] | (partialResult & 0xffffffffffffff00L) ;
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
