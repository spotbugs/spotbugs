package sfBugs;

public class Bug3495042 {

    public static int test1(int n) {
        int i = 0;
        while (n >= 10) {
            n = n / 10;
            i++;
        }
        return i;
    }

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
