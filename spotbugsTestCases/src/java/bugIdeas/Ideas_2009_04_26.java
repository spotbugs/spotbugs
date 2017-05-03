package bugIdeas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Ideas_2009_04_26 {

    private List<String> status_ = new ArrayList<String>();

    public String getStatus(int i) {
        assert i >= 0 && i < (status_ == null ? 0 : status_.size());
        return status_.get(i);
    }

    static abstract class AbstractIterator<T> implements Iterator<T> {

        T nextElement = nextElement();

        abstract T nextElement();

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();
            T result = nextElement;
            nextElement = nextElement();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    static Iterator<Integer> count(final int lastValue) {
        return new AbstractIterator<Integer>() {
            int value = 1;

            @Override
            Integer nextElement() {
                if (value > lastValue)
                    return null;
                return value++;

            }
        };
    }

    public static void main(String arg[]) {
        for (Iterator i = count(10); i.hasNext();)
            System.out.println(i.next());
    }

}
