
class InfiniteRecursiveLoop {
	int x,y;

	InfiniteRecursiveLoop(int x, int y) {

		InfiniteRecursiveLoop c = new InfiniteRecursiveLoop(x,y);
		}

	static int more() {
		return 1 + more();
		}

	int muchMore() {
		return 2 + muchMore();
		}
}
