
public class UselessCode {
    void method(int cmp) {
        assert cmp <= 0;

        if (cmp < 0) {
            System.out.println("cmp < 0");
        } else if (cmp == 0) {
            System.out.println("cmp == 0");
        }
    }
}
