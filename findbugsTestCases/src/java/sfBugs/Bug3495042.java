package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3495042 {

    @NoWarning("IL_INFINITE_LOOP")
    public static int test1(int n) {
        int i = 0;
        while (n >= 10) {
            n = n / 10;
            i++;
        }
        return i;
    }

    @NoWarning("IL_INFINITE_LOOP")
    public static int test2(int N) {
        int n = N;
        int i = 0;
        while (n >= 10) {
            n = n /10;
            i++;
        }
        return i;
    }


}
