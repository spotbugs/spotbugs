class BadEquals {
	public static void main(String args[]) {
		Object o[] = new Object[args.length];
		String s[] = new String[args.length];
		for (int i = 0; i < args.length; i++)
			o[i] = s[i] = args[i];
		if (args.equals(args))
			System.out.println("args.equals(args)");
		if (o.equals(args))
			System.out.println("o.equals(args)");
		if (s.equals(args))
			System.out.println("s.equals(args)");
		if (args.equals("test"))
			System.out.println("FOund test");
		if ("test".equals(args))
			System.out.println("Found test 2");
	}

	public boolean b(int [] a, Object [] b) {
		// TODO: Report this as a H C EC
		return a.equals(b); 
	}
	public boolean b(int [] a, String b) {
		return a.equals(b); 
	}
	public boolean b(int [] a, int[][] b) {
		return a.equals(b); 
	}
	public boolean b(int [] a, double [] b) {
		return a.equals(b); 
	}
}
