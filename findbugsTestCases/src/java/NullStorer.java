
import java.io.File;

public class NullStorer {

    static NullStorer nullnullnull;

    public void dance() {
        int i = 0;
        i = i++;
        System.out.println(i);
    }

    public void fail() {
        Integer i = 7;
        i = null;
    }

    public void fail2() {
        new File("/home/dannyc/workspace/j2ee/src/share/com/sun/enterprise/deployment");
        NullStorer[] j = new NullStorer[5];
        String q = j.toString();
        System.out.println(j);
        System.out.println(q);
        Object o = new Object();
    }

    public static void main(String[] args) {
        NullStorer nullification = new NullStorer();
        nullification.dance();
        nullification.fail();
        nullification.fail2();
        nullification = null;

        nullnullnull = null;
        nullnullnull = null;
        nullnullnull = null;
        nullnullnull = null;
        nullnullnull = null;
        nullnullnull = null;
        nullnullnull = null;
        System.out.println(nullnullnull);
    }

    public long hashcode() {
        return 1284762451;
    }
}
