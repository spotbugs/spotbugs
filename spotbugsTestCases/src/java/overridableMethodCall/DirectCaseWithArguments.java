package overridableMethodCall;

/**
 * Test case for GitHub issue #4309:
 * MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR should be reported when a constructor
 * or clone method calls an overridable method that takes arguments (e.g. a setter).
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4309">GitHub issue</a>
 */
public class DirectCaseWithArguments implements Cloneable {
    int value;

    DirectCaseWithArguments(int value) {
        setValue(value);
        privateMethod(value);
        finalMethod(value);
    }

    DirectCaseWithArguments(DirectCaseWithArguments other) {
        other.setValue(other.value);
    }

    @Override
    public DirectCaseWithArguments clone() throws CloneNotSupportedException {
        DirectCaseWithArguments omc = (DirectCaseWithArguments) super.clone();
        omc.setValue(42);
        omc.privateMethod(42);
        omc.finalMethod(42);
        return omc;
    }

    void setValue(int value) {
        this.value = value;
    }

    private void privateMethod(int value) {
        this.value = value;
    }

    final void finalMethod(int value) {
        this.value = value;
    }
}
