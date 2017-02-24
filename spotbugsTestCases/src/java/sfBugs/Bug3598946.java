package sfBugs;

import java.util.Collection;

public class Bug3598946 {

    public void problem(Collection<String> strings, int i) {

        for (String s : strings) {
            someMethod(s, i);
        }

        someMethod("hello world", 0);
    }

    public void someMethod(String s, int i) {
    }
}
