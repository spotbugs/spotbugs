package sfBugs;

import javax.annotation.Nonnull;



public class Bug3587164 {
    @Nonnull
    static final String field1 = "yyyyMMdd";

    @Nonnull
    static final String field2 = field1.toLowerCase();

}
