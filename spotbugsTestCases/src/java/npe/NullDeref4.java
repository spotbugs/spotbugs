package npe;

class NullDeref4 {

    void foo() {
        NullDeref4 n = new NullDeref4();
        if (n != null)
            System.out.println("Not null");
        System.out.println(n.hashCode());
    }
}
