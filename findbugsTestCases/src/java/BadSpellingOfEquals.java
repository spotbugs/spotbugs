import edu.umd.cs.findbugs.annotations.ExpectWarning;

class BadSpellingOfEquals {
    @ExpectWarning("Nm")
    public boolean equal(Object o) {
        return true;
    }
}
