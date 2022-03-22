package increasedAccessibilityOfMethods;

public class CloneableImplementation implements Cloneable {

    @Override
    public CloneableImplementation clone() {
        try {
            return (CloneableImplementation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
