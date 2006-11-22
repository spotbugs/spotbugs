public class InfiniteIterativeLoop {
	int sum(int n) {
		int result = 0;
		for (int i = 0; i < n;)
			result += i;
		return result;
	}

	int sumDoWhile(int n) {
		int result = 0;
		int i = 0;
		do {
			result += i;
		} while (i < n);
		return result;
	}

	int sumWhileDo(int n) {
		int result = 0;
		int i = 0;
		while (i < n) {
			result += i;
		}
		return result;
	}

	int sumOfOdd(int n) {
		int result = 0;
		for (int i = 0; i < n;) {
			if (i % 2 != 0)
				result += i;
		}
		return result;
	}

	int sumOfOddDoWhile(int n) {
		int result = 0;
		int i = 0;
		do {
			if (i % 2 != 0)
				result += i;
		} while (i < n);
		return result;
	}

	int sumOfOddWhileDo(int n) {
		int result = 0;
		int i = 0;
		while (i < n) {
			if (i % 2 != 0)
				result += i;
		}
		return result;
	}
}
