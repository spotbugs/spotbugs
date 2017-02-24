package sfBugs;

public class Bug1868666 {
    static String falsePositive(int a[]) {

        if (a.length % 2 == 1)
            return "odd length";
        else
            return "even length";
    }

}
