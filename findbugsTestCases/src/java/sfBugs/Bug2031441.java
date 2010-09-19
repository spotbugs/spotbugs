package sfBugs;

public class Bug2031441 {

    int foo(int size, int priority) {
        return (int) (size / 2 * Math.pow(0.8, priority));
    }
}
