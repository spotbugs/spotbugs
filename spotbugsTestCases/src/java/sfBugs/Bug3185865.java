package sfBugs;

public class Bug3185865 {
    private float f1() {
        return 0;
    }

    private float f2() {
        return 0;
    }

    public void methodA(float someFloat) {
        // detects this:
        assert someFloat == f1() + f2();
    }

    public void methodB(float someFloat) {
        // but it does not detect this:
        assert someFloat == f1();

    }

    public void test() {
        double i = Math.random();
        double j = Math.random();
        double k = Math.random();

        assert (i == 1.0f);
        assert (i == j);
        assert (k == f1());
        assert (i == f1() + f2()); // (4)
        assert (f1() == f2());
    }

}
