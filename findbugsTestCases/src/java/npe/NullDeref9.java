package npe;

final class NullDeref9 {

    void error() {
        throw new RuntimeException("You've got error");
    }

    void jspError() {
        throw new RuntimeException("You've got error");
    }

    static void oops() {
        throw new RuntimeException("You've got error");
    }

    public void foo(Object o) {
        if (o == null)
            error();
        System.out.println(o.hashCode());
    }

    public void bar(Object o) {
        if (o == null)
            jspError();
        System.out.println(o.hashCode());
    }

    public void baz(Object o) {
        if (o == null)
            oops();
        System.out.println(o.hashCode());
    }
}
