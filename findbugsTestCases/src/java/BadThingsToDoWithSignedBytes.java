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

	// 200
	boolean compareGT200(int pos) {
		int b = buf[pos];
		return b > 200;
	}
	boolean compareGE200(int pos) {
		int b = buf[pos];
		return b >= 200;
	}
	boolean compareNE200(int pos) {
		int b = buf[pos];
		return b != 200;
	}
	boolean compareEQ200(int pos) {
		int b = buf[pos];
		return b == 200;
	}
	boolean compareLE200(int pos) {
		int b = buf[pos];
		return b <= 200;
	}
	boolean compareLT200(int pos) {
		int b = buf[pos];
		return b < 200;
	}

	boolean isHundred(int pos) {
		int b = buf[pos];
		return b == 100 || b == 200;
	}

	long getPosByteDoNotReport(int pos) {

		long result = 0;
		for(int i = pos; i < pos+8; i++) {

			int b = buf[pos];
			if (b >= 0) result = result << 8 | b;
			else result = result << 8 | (256 + b);
		}
		return result;
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