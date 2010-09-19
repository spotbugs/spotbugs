public class NotThreadSafe extends ThreadSafe {

    public int party(int z) {
        x = 2 * z + (y * x);
        return x;
    }
}
