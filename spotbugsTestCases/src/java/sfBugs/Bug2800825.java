package sfBugs;

import javax.annotation.Nonnull;

/**
 * Generates a false redundant null check warning.
 * 
 * Must set FindBugs to show low priority warnings.
 */

public class Bug2800825 {

    public @Nonnull
    Object getNonNullValue() {
        return "dummy";
    }

    public void falsePositive() {
        if (null == getNonNullValue()) {// BUG - should not generate warning
            throw new IllegalStateException();
        }
        System.out.println("bar");
    }

}
