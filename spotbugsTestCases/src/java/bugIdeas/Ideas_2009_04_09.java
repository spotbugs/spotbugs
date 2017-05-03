package bugIdeas;

import junit.framework.TestCase;

public class Ideas_2009_04_09 extends TestCase {

    public void testBad() {

        try {

            shouldThrowAssertion();
            fail();
        } catch (AssertionError e) {

        }
    }

    public void shouldThrowAssertion() {

    }

}
