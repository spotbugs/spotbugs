//IteratorIdioms examples

import java.util.Iterator;

public class IteratorIdiomsTest implements Iterator {
    @Override
    public Object next() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public void remove() {
    }
}

class NotAnIterator {
    public Object next() {
        return null;
    }

    public boolean hasNext() {
        return false;
    }

    public void remove() {
    }
}
