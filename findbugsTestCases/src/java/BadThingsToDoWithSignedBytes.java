public class BadThingsToDoWithSignedBytes {
	public byte[] buf;

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
