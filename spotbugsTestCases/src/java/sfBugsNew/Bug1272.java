package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1272 {

    @NoWarning("UPM_UNCALLED_PRIVATE_METHOD")
    public static void main(String... args) {
        // TODO we should compile FB with 1.8
//        Runnable r1 = () -> {System.out.println("Hello Lambdas");};
//        r1.run();
    }
}
