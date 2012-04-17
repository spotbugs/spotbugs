package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;

public class ServerReturnedErrorCodeException extends IOException {

    private static final long serialVersionUID = 1L;

    public ServerReturnedErrorCodeException(int responseCode, String message) {
        super("server returned error code " + responseCode + " " + message);
    }
}
