package bugIdeas;

public class Ideas_2009_03_14 {

    int foo(int x) {
        String tmp = null;
        switch (x) {
        case 0:
            tmp = "zero";
            break;
        case 1:
            tmp = "one";
            break;
        case 2:
            tmp = "two";
            break;
        }
        return tmp.hashCode();
    }

}
