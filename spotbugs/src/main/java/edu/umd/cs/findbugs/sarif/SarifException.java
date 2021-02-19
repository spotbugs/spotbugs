package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    static SarifException fromThrowable(@NonNull Throwable throwable, @NonNull SourceFinder sourceFinder, @NonNull Map<URI, String> baseToId) {
        String message = throwable.getMessage();
        if (message == null) {
            message = "no message given";
        }
        List<Throwable> innerThrowables = new ArrayList<>();
        innerThrowables.add(throwable.getCause());
        innerThrowables.addAll(Arrays.asList(throwable.getSuppressed()));
        List<SarifException> innerExceptions = innerThrowables.stream()
                .filter(Objects::nonNull)
                .map(t -> fromThrowable(t, sourceFinder, baseToId))
                .collect(Collectors.toList());
        return new SarifException(throwable.getClass().getName(), message, Stack.fromThrowable(throwable, sourceFinder, baseToId), innerExceptions);
    }

    JsonObject toJsonObject() {
        JsonObject textJson = new JsonObject();
        textJson.addProperty("text", message);

        JsonObject result = new JsonObject();
        result.addProperty("kind", kind);
        result.add("message", textJson);
        result.add("stack", stack.toJsonObject());

        JsonArray exceptionArray = new JsonArray();
        innerExceptions.forEach(innerException -> exceptionArray.add(innerException.toJsonObject()));
        if (exceptionArray.size() > 0) {
            result.add("innerExceptions", exceptionArray);
        }
        return result;
    }
}
