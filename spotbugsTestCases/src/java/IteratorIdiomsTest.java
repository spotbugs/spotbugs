//IteratorIdioms examples

import java.util.Iterator;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.IteratorIdioms;

@DetectorUnderTest(IteratorIdioms.class)
public class IteratorIdiomsTest implements Iterator {
    @ExpectWarning("IT_NO_SUCH_ELEMENT")
    @Override
    public Object next() {
        return null;
    }

    @NoWarning("IT_NO_SUCH_ELEMENT")
    public Object next(boolean dummy) {
        return null;
    }
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    protected Object next(int dummy) {
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
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    public Object next() {
        return null;
    }

    @NoWarning("IT_NO_SUCH_ELEMENT")
    protected Object next(int dummy) {
        return null;
    }
    
    public boolean hasNext() {
        return false;
    }

    public void remove() {
    }
}

class StringIterator implements Iterator<String> {
    @ExpectWarning("IT_NO_SUCH_ELEMENT")
    @Override
    public String next() {
        return null;
    }
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    public Object next(boolean dummy) {
        return null;
    }

    @NoWarning("IT_NO_SUCH_ELEMENT")
    protected Object next(int dummy) {
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

class GenericIterator<X> implements Iterator<X> {
    @ExpectWarning("IT_NO_SUCH_ELEMENT")
    @Override
    public X next() {
        return null;
    }
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    public Object next(boolean dummy) {
        return null;
    }
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    protected Object next(int dummy) {
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

class GenericIterator2<X extends Number> implements Iterator<X> {
    @ExpectWarning("IT_NO_SUCH_ELEMENT")
    @Override
    public X next() {
        return null;
    }
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    public Object next(boolean dummy) {
        return null;
    }
    
    @NoWarning("IT_NO_SUCH_ELEMENT")
    protected Object next(int dummy) {
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
