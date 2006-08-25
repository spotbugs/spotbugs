public class BadThingsToDoWithSignedBytes {
	public byte[] buf;

	boolean compareGT127(int pos) {
		int b = buf[pos];
		return b > 127;
	}
	boolean compareGE127(int pos) {
		int b = buf[pos];
		return b >= 127;
	}
	boolean compareNE127doNotReport(int pos) {
		int b = buf[pos];
		return b != 127;
	}
	boolean compareEQ127doNotReport(int pos) {
		int b = buf[pos];
		return b == 127;
	}
	boolean compareLE127(int pos) {
		int b = buf[pos];
		return b <= 127;
	}
	boolean compareLT127(int pos) {
		int b = buf[pos];
		return b < 127;
	}
// 128
	boolean compareGT128(int pos) {
		int b = buf[pos];
		return b > 128;
	}
	boolean compareGE128(int pos) {
		int b = buf[pos];
		return b >= 128;
	}
	boolean compareNE128(int pos) {
		int b = buf[pos];
		return b != 128;
	}
	boolean compareEQ128(int pos) {
		int b = buf[pos];
		return b == 128;
	}
	boolean compareLE128(int pos) {
		int b = buf[pos];
		return b <= 128;
	}
	boolean compareLT128(int pos) {
		int b = buf[pos];
		return b < 128;
	}

	boolean isHundred(int pos) {
		int b = buf[pos];
		return b == 100 || b == 200;
	}

	boolean isHundred2(int pos) {
		int b = buf[pos];
		switch (b) {
		case 100:
		case 200:
			return true;
		default:
			return false;
		}
	}

}
