class ShortCircuitZealousness {
	public static final int BIT0 = 1; // 1st bit

	protected int m_iType;

	public ShortCircuitZealousness(boolean available) {
		m_iType |= available ? BIT0 : 0;
	}
}
