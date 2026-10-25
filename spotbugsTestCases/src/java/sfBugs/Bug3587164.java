package sfBugs;

import org.jspecify.annotations.NonNull;

public class Bug3587164 {

    static final @NonNull String field1 = "yyyyMMdd";

    static final @NonNull String field2 = field1.toLowerCase();

}
