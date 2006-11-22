
public class InfiniteIterativeLoop {
	int sum(int n) {
		int result = 0;
		for(int i = 0; i < n; )
			result += i;
		return result;
	}

	int sumOfOdd(int n) {
		int result = 0;
		for(int i = 0; i < n; ) {
			if (i % 2 != 0)
				result += i;
		}
		return result;
	}
}
