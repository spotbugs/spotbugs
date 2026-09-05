package ghIssues;

import java.util.Iterator;

public class Issue3501<T> implements Iterator<T> {
    private final T value;

    private Issue3501(T value) {
        this.value = value;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public T next() {
        return value;
    }
}
