package ghIssues;

public class Issue3235 implements Issue3235BuilderInterface {
    private Issue3235BuilderInterface delegate;

    public Issue3235(Issue3235BuilderInterface delegate) {
        this.delegate = delegate;
    }

    @Override
    public Issue3235BuilderInterface setParent(Issue3235Context context) {
        delegate.setParent(context);
        return this;
    }
}

interface Issue3235BuilderInterface {
    Issue3235BuilderInterface setParent(Issue3235Context context);
}

interface Issue3235Context {

}
