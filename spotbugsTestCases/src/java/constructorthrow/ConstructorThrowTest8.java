package constructorthrow;

import java.util.Arrays;

/**
 * In this test case the constructor throws an unchecked exception from
 * directly in a stream lambda.
 */
public class ConstructorThrowTest8 {
    public ConstructorThrowTest8() {
        Arrays.asList(1,2,3,4).stream()
                .forEach(i -> { throw new RuntimeException(); });
    }
}
