package ghIssues;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class Issue4401 {

    public record Update(@CheckForNull Object before, @CheckForNull Object after) {
        public Update {
            Preconditions.checkArgument(before != null || after != null);
        }
    }

    public static final class UpdateClass {
        public final @CheckForNull Object before;
        public final @CheckForNull Object after;

        public UpdateClass(@Nullable Object before, @Nullable Object after) {
            Preconditions.checkArgument(before != null || after != null);
            this.before = before;
            this.after = after;
        }
    }

    public static final class UpdateSwapped {
        public final @CheckForNull Object before;
        public final @CheckForNull Object after;

        public UpdateSwapped(@Nullable Object before, @Nullable Object after) {
            Preconditions.checkArgument(after != null || before != null);
            this.before = before;
            this.after = after;
        }
    }

    public static final class AlwaysNullField {
        public final @CheckForNull Object value;

        public AlwaysNullField() {
            this.value = null;
        }
    }

    public void processUpdateDirectFieldAccess(Update update) {
        if (update.before instanceof String str1 && update.after instanceof String str2) {
            System.out.println("Change: " + str1 + " -> " + str2);
        }
    }

    public void processUpdateClass(UpdateClass update) {
        if (update.before instanceof String str1 && update.after instanceof String str2) {
            System.out.println("Change: " + str1 + " -> " + str2);
        }
    }

    public void processUpdateClassExplicitNullCheck(UpdateClass update) {
        Object before = update.before;
        if (before != null && before instanceof String str1) {
            System.out.println("Before: " + str1);
        }
    }

    public void processUpdateSwapped(UpdateSwapped update) {
        if (update.before instanceof String str1 && update.after instanceof String str2) {
            System.out.println("Change: " + str1 + " -> " + str2);
        }
    }

    public void processAlwaysNullField(AlwaysNullField holder) {
        if (holder.value instanceof String str) {
            System.out.println("Value: " + str);
        }
    }
}
