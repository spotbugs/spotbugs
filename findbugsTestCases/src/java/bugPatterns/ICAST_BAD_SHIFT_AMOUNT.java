package bugPatterns;

public class ICAST_BAD_SHIFT_AMOUNT {
	
	int bug(int any) {
		return any >> 32;
	}
	int bug2(int any) {
		return any << 32;
	}
	long highPriorityBug(int any) {
		return any << 32;
	}
	long highPriorityBug(int any1, int any2) {
		return (any1 << 32) | any2;
	}
	long highPriorityBug2(int any1, int any2) {
		return any2  | (any1 << 32);
	}
}
