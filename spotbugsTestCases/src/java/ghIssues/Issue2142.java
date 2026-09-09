package ghIssues;

public class Issue2142 {
    int foo;
    void foo1() {
        foo = foo++;  // can report a warning in this line
    }
    class Inner {
        void foo2() {
            foo = foo++; // should report a warning in this line
        }
    }    
}
