package bugIdeas;

import java.util.HashSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2008_08_06<E> extends HashSet<E> {

    public int bar() {
        throw new UnsupportedOperationException();
    }

    public int foo() {
        return bar();
    }

    @ExpectWarning("GC,Dm")
    public static void main(String args[]) {
        Ideas_2008_08_06<String> i = new Ideas_2008_08_06<String>();
        System.out.println(i.contains(5));
        ScheduledThreadPoolExecutor e = new ScheduledThreadPoolExecutor(0);
        e.setMaximumPoolSize(10);
    }

}
