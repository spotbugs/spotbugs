class InfiniteRecursiveLoop {
    int x, y;

    InfiniteRecursiveLoop(int x, int y) {

        InfiniteRecursiveLoop c = new InfiniteRecursiveLoop(x, y);
    }

    static int more() {
        return 1 + more();
    }

    int muchMore() {
        return 2 + muchMore();
    }

    @Override
    public boolean equals(Object o) {
        return equals(o);
    }

    @Override
    public int hashCode() {
        int i = System.identityHashCode(this);
        return i + hashCode();
    }
}
