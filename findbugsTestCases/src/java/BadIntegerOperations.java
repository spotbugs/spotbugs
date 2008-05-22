import java.security.SecureRandom;
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
	SecureRandom sr = new SecureRandom();
	public int getRandomElement(int a[]) {
		return a[r.nextInt() % a.length];
	}
	public int getRandomElement2(int a[]) {
		int i = r.nextInt() % a.length;
		return a[i];
	}
	public int getSecureRandomElement(int a[]) {
		return a[sr.nextInt() % a.length];
	}

	public static Object getHashBucket(Object a[], Object x) {
		return a[x.hashCode() % a.length];
	}
	public static Object getHashBucket2(Object a[], Object x) {
		int i = x.hashCode() % a.length;
		return a[i];
	}
	public void operationsOnBooleanArrays(String[] args)
	{
	final boolean[] values = { false };
	values[0] |= (args.length > 0);
	}


	public int getNonNegativeRandomInt() {
		return Math.abs(r.nextInt());
	}

	public int getNonNegativeSecureRandomInt() {
		return Math.abs(sr.nextInt());
	}
	public int getNonNegativeHashCode() {
		return Math.abs(hashCode());
	}
	public int getNonNegativeIdentityHashCode() {
		return Math.abs(System.identityHashCode(this));
	}

	public int getNonNegativeHashCodeLowPriority256(Object x) {
		return Math.abs(x.hashCode()) % 256;
	}
	public int getNonNegativeHashCode257(Object x) {
		return Math.abs(x.hashCode()) % 257;
	}
	/** This method is OK */
	public int getRandomIntFalsePositive(int n) {
		return Math.abs(r.nextInt() % n);
	}

	/** This method is OK */
	public static int getHashBucketFalsePositive(Object x, int n) {
		return Math.abs(x.hashCode() % n);
	}

	/** This method is OK */
	public int getRandomIntFalsePositive2(int n) {
		int i = r.nextInt() % n;
		return Math.abs(i);
	}

	/** This method is OK */
	public static int getHashBucketFalsePositive2(Object x, int n) {
		int i = x.hashCode() % n;
		return Math.abs(i);
	}

	public BadIntegerOperations() {
	}
}
