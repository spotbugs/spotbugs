public class ThreadSafe {
    protected int x;

    protected int y;

    public synchronized void setX(int x) {
        this.x = x;
    }

    public synchronized int getX() {
        return x;
    }

    public synchronized void setXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public synchronized int getY() {
        return y;
    }
}
