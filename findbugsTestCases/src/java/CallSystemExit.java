class CallSystemExit {

	@Override
	public boolean equals(Object o) {
		if (o instanceof CallSystemExit)
			return true;
		System.exit(1);
		return false;
	}
}
