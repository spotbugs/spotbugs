import edu.umd.cs.findbugs.annotations.ExpectWarning;

class UselessAssignments {
    @ExpectWarning("UR")
    int foo;
    
    @ExpectWarning("UrF")
    int bar, g1;
    
    @ExpectWarning("UuF")
    int g2;

    @ExpectWarning("DLS,SA")
    public UselessAssignments(int Foo, int Bar) {
        int x, y;
        foo = foo;
        Bar = Bar;
        g1 = g1 = 0;
        x = x = 0;
    }

    static void setFoo(UselessAssignments ua1, UselessAssignments ua2) {
        ua1.foo = ua2.foo = 1;
        ua1.bar = ua1.bar = ua1.bar = 2;
    }

    int oops() {
        int i = 7;
        i = ++i;
        i = i++;
        foo = foo++;
        foo = ++foo;
        return i;
    }

    public static void report(UselessAssignments ua1, UselessAssignments ua2) {
        ua1.foo = ua1.foo = ua1.foo = 17;
    }

    public static void doNotReport(UselessAssignments ua1, UselessAssignments ua2) {
        ua1.bar = ua2.bar = 42;
    }

}
