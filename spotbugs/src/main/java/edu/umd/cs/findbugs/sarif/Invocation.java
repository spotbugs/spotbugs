package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317567">3.20 invocation object</a>
 */
class Invocation {
    private final int exitCode;
    @NonNull
    private final String exitSignalName;
    private final boolean executionSuccessful;
    @NonNull
    private final List<Notification> toolExecutionNotifications;
    @NonNull
    private final List<Notification> toolConfigurationNotifications;

    Invocation(int exitCode, @NonNull String exitSignalName, boolean executionSuccessful, @NonNull List<Notification> toolExecutionNotifications,
            @NonNull List<Notification> toolConfigurationNotifications) {
        this.exitCode = exitCode;
        this.exitSignalName = Objects.requireNonNull(exitSignalName);
        this.executionSuccessful = executionSuccessful;
        this.toolExecutionNotifications = Collections.unmodifiableList(toolExecutionNotifications);
        this.toolConfigurationNotifications = Collections.unmodifiableList(toolConfigurationNotifications);
    }

    @NonNull
    JSONObject toJSONObject() {
        JSONObject result = new JSONObject()
                .put("exitCode", exitCode)
                .put("exitSignalName", exitSignalName)
                .put("executionSuccessful", executionSuccessful);
        toolExecutionNotifications.stream()
                .map(Notification::toJSONObject)
                .forEach(json -> result.append("toolExecutionNotifications", json));
        toolConfigurationNotifications.stream()
                .map(Notification::toJSONObject)
                .forEach(json -> result.append("toolConfigurationNotifications", json));
        return result;
    }
}
