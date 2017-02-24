package sfBugs;

public enum Bug1619879 {
    ONE, TWO;

    int f;

    // should report something other than EQ_SELF_NO_OBJECT
    public boolean equals(Bug1619879 foo) {
        return f == foo.f;
    }

}
