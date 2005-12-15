class EncapsulationViolation {
	private byte extensionValue[] = { 1, 2, 3 };

	public byte[] getExtensionValue() {
		if (extensionValue == null)
			return null;

		byte[] dup = new byte[extensionValue.length];
		System.arraycopy(extensionValue, 0, dup, 0, dup.length);
		return (dup);
	}
}
