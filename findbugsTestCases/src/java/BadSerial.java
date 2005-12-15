import java.io.Serializable;

class BadSerial {
	static class NotFinal implements Serializable {
		static long serialVersionUID = 1;
	}

	static class NotStatic implements Serializable {
		final long serialVersionUID = 2;
	}

	static class NotLong implements Serializable {
		static final int serialVersionUID = 3;
	}

	static class Good implements Serializable {
		static final long serialVersionUID = 4;
	}
}
