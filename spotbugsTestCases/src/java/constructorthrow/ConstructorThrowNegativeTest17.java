package constructorthrow;

import java.io.IOException;
import java.util.Arrays;

/**
 * In this test case the constructor throws an checked exception from
 * directly in a stream lambda.
 */
public class ConstructorThrowNegativeTest17 {
    public ConstructorThrowNegativeTest17() {
        Arrays.asList(1,2,3,4).stream()
                .forEach(i -> {
                    try {
                        test(i);
                    } catch (IOException e) {
                        System.err.println("Error happened");
                    }
                });
    }

    public void test(int i) throws IOException {
        throw new IOException();
    }
}
