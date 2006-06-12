public class BinarySearch {

	// This one is buggy
	public static int binarySearchUsingDivision(int a[], int x) {
		int low = 0;
		int high = a.length - 1;
		while (low <= high) {
			int mid = (low + high) / 2;
			int midValue = a[mid];
			if (x == midValue)
				return mid;
			if (x < midValue) {
				high = mid - 1;
			} else
				low = high + 1;
		}
		return -1;
	}

	// This one is also buggy
	public static int binarySearchUsingRightShift(int a[], int x) {
		int low = 0;
		int high = a.length - 1;
		while (low <= high) {
			int mid = (low + high) >> 1;
			int midValue = a[mid];
			if (x == midValue)
				return mid;
			if (x < midValue) {
				high = mid - 1;
			} else
				low = high + 1;
		}
		return -1;
	}

	// This one is fine
	public static int binarySearchUsingUnsignedRightShift(int a[], int x) {
		int low = 0;
		int high = a.length - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midValue = a[mid];
			if (x == midValue)
				return mid;
			if (x < midValue) {
				high = mid - 1;
			} else
				low = high + 1;
		}
		return -1;
	}

}
