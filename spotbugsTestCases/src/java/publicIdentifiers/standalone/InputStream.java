package publicIdentifiers.standalone;

public class InputStream {
    private int size = 0;
    private int seek = 0;

    public InputStream(int size, int seek) {
        this.size = size;
        this.seek = seek;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean hasRemaining() {
        return seek < size;
    }
}
