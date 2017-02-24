package jsr305.validation;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestSlashedClassNameValidator {

    public void needsSlashedClassname(@SlashedClassName String name) {
    }

    public void needsDottedClassname(@DottedClassName String name) {
    }

    @ExpectWarning("TQ")
    public void testFooDotBarAsSlashedClassName() {
        needsSlashedClassname("foo.Bar");
    }

    @NoWarning("TQ")
    public void testFooSlashBarAsSlashedClassName() {
        needsSlashedClassname("foo/Bar");
    }

    @NoWarning("TQ")
    public void testFooDotBarAsDottedClassName() {
        needsDottedClassname("foo.Bar");
    }

    @ExpectWarning("TQ")
    public void testFooSlashBarAsDottedClassName() {
        needsDottedClassname("foo/Bar");
    }
}
