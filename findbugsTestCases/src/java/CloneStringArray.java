class CloneStringArray {
	public static void main(String args[]) {
		String[] copy = (String[]) args.clone();
		System.out.println(copy.toString());
	}
}
