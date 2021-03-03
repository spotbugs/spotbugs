package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A class representing {@code stack} object (§3.44)
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012754">3.44 stack object</a>
 */
class Stack {
    final String message;
    final List<StackFrame> frames;

    Stack(@NonNull String message, @NonNull List<StackFrame> frames) {
        this.message = Objects.requireNonNull(Objects.requireNonNull(message));
        this.frames = Collections.unmodifiableList(Objects.requireNonNull(frames));
    }

    static Stack fromThrowable(Throwable throwable, @NonNull SourceFinder sourceFinder, @NonNull Map<URI, String> baseToId) {
        List<StackFrame> frames = Arrays.stream(Objects.requireNonNull(throwable).getStackTrace()).map(element -> StackFrame.fromStackTraceElement(
                element, sourceFinder, baseToId)).collect(
                        Collectors.toList());
        String message = throwable.getMessage();
        if (message == null) {
            message = "no message given";
        }
        return new Stack(message, frames);
    }

    JsonObject toJsonObject() {
        JsonObject textJson = new JsonObject();
        textJson.addProperty("text", message);

        JsonObject result = new JsonObject();
        result.add("message", textJson);

        JsonArray frameArray = new JsonArray();
        frames.forEach(stackFrame -> frameArray.add(stackFrame.toJsonObject()));
        if (frameArray.size() > 0) {
            result.add("frames", frameArray);
        }
        return result;
    }

    /**
     * A class representing {@code stackFrame} object (§3.45)
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012758">3.45 stackFrame object</a>
     */
    static class StackFrame {
        @NonNull
        final Location location;

        StackFrame(@NonNull Location location) {
            this.location = Objects.requireNonNull(location);
        }

        static StackFrame fromStackTraceElement(@NonNull StackTraceElement element, @NonNull SourceFinder sourceFinder,
                @NonNull Map<URI, String> baseToId) {
            Location location = Location.fromStackTraceElement(element, sourceFinder, baseToId);
            return new StackFrame(location);
        }

        JsonObject toJsonObject() {
            JsonObject result = new JsonObject();
            result.add("location", location.toJsonObject());
            return result;
        }
    }
}
