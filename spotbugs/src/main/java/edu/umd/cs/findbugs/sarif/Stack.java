package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    static Stack fromThrowable(Throwable throwable) {
        List<StackFrame> frames = Arrays.stream(Objects.requireNonNull(throwable).getStackTrace()).map(StackFrame::fromStackTraceElement).collect(
                Collectors.toList());
        String message = throwable.getMessage();
        if (message == null) {
            message = "no message given";
        }
        return new Stack(message, frames);
    }

    JSONObject toJSONObject() {
        JSONObject result = new JSONObject().put("messsage", new JSONObject().put("text", message));
        frames.forEach(stackFrame -> result.append("frames", stackFrame.toJSONObject()));
        return result;
    }

    /**
     * A class representing {@code stackFrame} object (§3.45)
     * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/cs01/sarif-v2.1.0-cs01.html#_Toc16012758">3.45 stackFrame object</a>
     */
    static class StackFrame {
        final String message;
        // TODO how to build Location object?

        StackFrame(@NonNull String message) {
            this.message = Objects.requireNonNull(message);
        }

        static StackFrame fromStackTraceElement(@NonNull StackTraceElement element) {
            return new StackFrame(Objects.requireNonNull(element).toString());
        }

        JSONObject toJSONObject() {
            return new JSONObject().put("location", new JSONObject().put("message", message));
        }
    }
}
