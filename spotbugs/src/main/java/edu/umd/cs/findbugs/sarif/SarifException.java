package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317904">3.59 exception object</a>
 */
class SarifException {
    @NonNull
    final String kind;
    @NonNull
    final String message;
    @NonNull
    final Stack stack;
    @NonNull
    final List<SarifException> innerExceptions;

    SarifException(@NonNull String kind, @NonNull String message, @NonNull Stack stack, @NonNull List<SarifException> innerExceptions) {
        this.kind = Objects.requireNonNull(kind);
        this.message = Objects.requireNonNull(message);
        this.stack = Objects.requireNonNull(stack);
        this.innerExceptions = Collections.unmodifiableList(Objects.requireNonNull(innerExceptions));
    }

    @NonNull
    static SarifException fromThrowable(@NonNull Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            message = "no message given";
        }
        List<Throwable> innerThrowables = new ArrayList<>();
        innerThrowables.add(throwable.getCause());
        innerThrowables.addAll(Arrays.asList(throwable.getSuppressed()));
        List<SarifException> innerExceptions = innerThrowables.stream().filter(Objects::nonNull).map(SarifException::fromThrowable).collect(Collectors.toList());
        return new SarifException(throwable.getClass().getName(), message, Stack.fromThrowable(throwable),innerExceptions);
    }

    JSONObject toJsonObject() {
        JSONObject result = new JSONObject().put("exception", new JSONObject().put("kind", kind).put("message", new JSONObject().put("text", message))).put("stack", stack.toJsonObject());
        innerExceptions.forEach(innerException -> result.append("innerExceptions", innerException.toJsonObject()));
        return result;
    }
}

