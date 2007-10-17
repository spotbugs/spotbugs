package sfBugs;

public class Bug1811106 {

    public static void main(String args[]) {
        Bug1811106 b = new Bug1811106();
        b.method0(args);
        b.method1(args);
        b.method2(args);
    }
    
    public void method0(String[] args) {
        // not detected
        String foo = "";
        int i;
        i = 0;
        if (i < args.length) {
            do {
                foo = foo + " AND " + args[i];
                i++;
            } while (i < args.length);
        }
    }

    public void method1(String[] args) {
        // detected
        String foo = "";
        int i;
        i = 0;
        while (i < args.length)
        {
            foo = foo + " AND " + args[i];
            i++;
        }
    }

    public void method2(String[] args) {
        // detected
        String foo = "";
        int i = 0;
        for (; i < args.length; i++) {
            foo = foo + " AND " + args[i];
        }
    }
    
    public void all(String[] args) {
        // not detected
        String foo = "";
        int i;
        i = 0;
        if (i < args.length) {
            do {
                foo = foo + " AND " + args[i];
                i++;
            } while (i < args.length);
        }
        i = 0;
        while (i < args.length)
        {
            foo = foo + " AND " + args[i];
            i++;
        }
        i = 0;
        for (; i < args.length; i++) {
            foo = foo + " AND " + args[i];
        }
    }
}
