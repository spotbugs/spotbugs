package constructorthrow;

import java.util.Arrays;

/**
 * In this test case the constructor calls an unchecked exception
 * throwing method in a stream lambda.
 */
public class ConstructorThrowTest7 {
    public ConstructorThrowTest7() {
        Arrays.asList(1,2,3,4).stream()
                .forEach(i -> test(i));
    }

    public void test(int i) {
        throw new RuntimeException();
    }
}
