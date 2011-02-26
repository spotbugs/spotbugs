package sfBugs;

public class Bug3185865 {
    private float floatFct1() { return 0; }
    private float floatFct2() { return 0; }
    public void methodA(float someFloat) {
        // detects this:
        assert someFloat == floatFct1() + floatFct2();
        //but it does not detect this:
        assert someFloat == floatFct1();

    }
}
