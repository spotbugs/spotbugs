class AssumeUnsignedBytes {

	int find200(byte[] b) {
		for (int i = 0; i < b.length; i++)
			if (b[i] == 200)
				return i;
		return -1;
	}
}
