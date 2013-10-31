package sfBugsNew;

import java.util.Arrays;
import java.util.List;

public class Bug1214 {

    public static void main(String[] args) {
        String arg1 = args[0];
        String arg2 = args[1];
        String arg3 = args[2];
        if (arg1 != null) {
            System.out.println(arg1);
            return;
        }
        List<?> list = Arrays.asList(arg1, arg2, arg3);
        throw new IllegalArgumentException("Bad args: " + list.toString());
    }
}
