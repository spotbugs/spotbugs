class SwitchFallthrough {

	void test1(int i) {
		switch (i) {
		case -1:
		case 0:
			System.out.println("zero");
		case 1:
		case 2:
			System.out.println("one");
		case 10:
			System.out.println("four");
		case 4:
		case 5:
			System.out.println("two");
		// fallthrough
		case 6:
		case 7:
			System.out.println("three");
		default:
			System.out.println("something else");
		}
	}

	void test2(int i, int j) {
		switch (i) {
		case 0:
			switch (j) {
			case 0:
				System.out.println("zero zero");
			}

		case 1:
			System.out.println("zero");
		}
	}

	void test3(int i, int j) {
		switch (i) {
		case 0:
			if (j == 0)
				return;

		case 1:
			j = 1;
			break;
		}
	}

}
