class FieldsNotSetInConstructor {

	Object a, b, c, d, e;

	FieldsNotSetInConstructor() {
		a = this;
	}

	FieldsNotSetInConstructor(Object x) {
		a = x;
		b = x;
	}

	public int hashCode() {
		return a.hashCode() + b.hashCode() + c.hashCode() + e.hashCode();
	}

	public String toString() {
		if (c == null || d == null)
			return a.toString();
		return a.toString() + b.toString() + c.toString() + d.toString();
	}

	public void setE(Object x) {
		e = x;
	}
}
