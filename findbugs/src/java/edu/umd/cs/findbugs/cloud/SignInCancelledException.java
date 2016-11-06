package edu.umd.cs.findbugs.cloud;

public class SignInCancelledException extends Exception {
    public SignInCancelledException() {
        super("User is not signed into SpotBugs Cloud");
    }

    public SignInCancelledException(Throwable cause) {
        super("User is not signed into SpotBugs Cloud", cause);
    }
}
