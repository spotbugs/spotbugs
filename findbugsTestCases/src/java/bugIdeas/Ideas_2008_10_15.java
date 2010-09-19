package bugIdeas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Ideas_2008_10_15<E> {

    public synchronized void wasModified(Collection<E> objects) {

    }

    public synchronized void wasModified(Object object) {
        wasModified(listOf(object));
    }

    public static <T> List<T> listOf(T e) {
        ArrayList<T> a = new ArrayList<T>();
        a.add(e);
        return a;
    }

    static class A<E> {
        void handle(E e) {
        }
    }

    static class B<E> extends A<E> {
        @Override
        void handle(Object o) {
        }
    }
}
