package sfBugs;

public class Bug1679198 {

    static int infiniteRecursion(int x) {
        System.out.println(x);
        if (x != 0) {
            return infiniteRecursion(x - 1);
        } else {
            return 0;
        }
    }

    public static void main(String[] args) {
        int a = 10;

        infiniteRecursion(a);
    }

}
