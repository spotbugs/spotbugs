package edu.umd.cs.findbugs;

/**
 * @author David Hovemeyer
 */
public class NoOpFindBugsProgress implements FindBugsProgress {
    @Override
    public void reportNumberOfArchives(int numArchives) {
    }

    @Override
    public void finishArchive() {
    }

    @Override
    public void startAnalysis(int numClasses) {
    }

    @Override
    public void finishClass() {
    }

    @Override
    public void finishPerClassAnalysis() {
    }

    @Override
    public void predictPassCount(int[] classesPerPass) {
        // noop
    }

    @Override
    public void startArchive(String name) {
        // noop
    }
}
