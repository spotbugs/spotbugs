package constructorthrow;

import java.util.Arrays;

/**
 * In this test case the constructor throws an unchecked exception from
 * directly in a stream lambda.
 */
public class ConstructorThrowTest9 {
    public ConstructorThrowTest9() {
        Arrays.asList(new RuntimeException()).stream()
                .forEach(re -> {throw re;});
    }
}
