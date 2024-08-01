package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.List;
import java.util.Objects;

class Placeholder {
    /**
     * Index of the target {@link edu.umd.cs.findbugs.BugAnnotation} to embed.
     */
    final int index;

    /**
     * Key to formatting {@link edu.umd.cs.findbugs.BugAnnotation}.
     * @see edu.umd.cs.findbugs.BugAnnotation#format(String, ClassAnnotation)
     */
    @NonNull
    final String key;

    Placeholder(int index, @NonNull String key) {
        this.index = index;
        this.key = Objects.requireNonNull(key);
    }

    @NonNull
    String toArgument(List<? extends BugAnnotation> bugAnnotations, @Nullable ClassAnnotation primaryClass) {
        if (index < 0) {
            return "?<?" + index + "/" + bugAnnotations.size() + "???";
        } else if (index >= bugAnnotations.size()) {
            return "?>?" + index + "/" + bugAnnotations.size() + "???";
        } else {
            return bugAnnotations.get(index).format(key, primaryClass);
        }
    }
}
