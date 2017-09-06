package lambdas;

public class Issue338 {

    public static void main(String[] args) {
        Runnable a = System.err::println;
        Runnable b = () -> {
            if (System.getenv("SOME_RANDOM_VAR") == null) {
                a.run();
            }
            a.run();
        };
        a.run();
        b.run();
    }

}
