package infiniteLoop;

public class Issue4307 {

    public void test() {
        int start = 1_234_567_890;
        for (float value = start; value < start + 50; value++) {
        }
    }
}
