package sfBugsNew;

import java.util.Random;

import javax.annotation.CheckForNull;

public class Bug1208 {

    Random r = new Random();
    // Some shoot-your-leg code
    public @CheckForNull Bug1208 m1() {
        return r.nextBoolean() ? this : null;
    }

    public Bug1208 m2() {
        return this;
    }

    public void print() {
        System.out.println("hi");
    }

    public static void main(String... args) {
        new Bug1208().m1().m2().print();
    }
}
