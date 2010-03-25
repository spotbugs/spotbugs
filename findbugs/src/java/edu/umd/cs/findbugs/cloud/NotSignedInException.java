package edu.umd.cs.findbugs.cloud;

import java.io.IOException;

public class NotSignedInException extends Exception {
    public NotSignedInException() {
        super("User is not signed into FindBugs Cloud");
    }

    public NotSignedInException(Throwable cause) {
        super("User is not signed into FindBugs Cloud", cause);
    }
}
