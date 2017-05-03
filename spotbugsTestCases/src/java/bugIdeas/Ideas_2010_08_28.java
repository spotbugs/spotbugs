package bugIdeas;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Condition;

public class Ideas_2010_08_28 {

    public static void test(Condition c) throws InterruptedException {
        c.wait();
        c.wait(1000);
        c.wait(0, 50);

        c.notify();
        c.notifyAll();

    }

    public static void test(CyclicBarrier c) throws InterruptedException {

        c.wait();
        c.wait(1000);
        c.wait(0, 50);
    }

    public static void test2(CyclicBarrier c) {
        c.notify();
        c.notifyAll();
    }

    public static void test(CountDownLatch c) throws InterruptedException {

        c.wait();
        c.wait(1000);
        c.wait(0, 50);
        c.notify();
        c.notifyAll();

    }
}
