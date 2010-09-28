package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;

public class ServerReturnedErrorCodeException extends IOException {
    public ServerReturnedErrorCodeException(int responseCode, String message) {
        super("server returned error code " + responseCode + " " + message);
    }
}
