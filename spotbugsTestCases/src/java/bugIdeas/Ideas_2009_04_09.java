package bugIdeas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Ideas_2009_04_09 {

    @Test
    public void testBad() {

        try {

            shouldThrowAssertion();
            Assertions.fail();
        } catch (AssertionError e) {

        }
    }

    public void shouldThrowAssertion() {

    }

}
