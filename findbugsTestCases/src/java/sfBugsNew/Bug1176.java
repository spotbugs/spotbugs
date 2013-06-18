package sfBugsNew;

import java.util.HashSet;
import java.util.Set;

public class Bug1176 {

    public static void main(final String[] args) {
        final Set<String> strings = new HashSet<String>();
        for (final String string : strings) {
            System.out.println(string);
        }
    }
}
