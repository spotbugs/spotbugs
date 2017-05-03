package npe;

class NullDeref10 {

    int foo(Object o) {
        System.out.println(o == null);
        return o.hashCode();
    }
}
