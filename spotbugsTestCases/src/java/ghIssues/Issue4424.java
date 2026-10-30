package ghIssues;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Issue4424 {
    interface HasListener<I> {
        void setListener(@Nullable Consumer<@NonNull I> listener);

        void setOtherListener(@Nullable Consumer<I> listener);

        @NonNull List<I> getItems();
    }

    static class SpecificHasListener<I> implements HasListener<I> {
        // Annotations on type arguments are not about the parameter or return value itself
        @Override
        public void setListener(@Nullable Consumer<@NonNull I> listener) {
        }

        @Override
        public void setOtherListener(@Nullable Consumer<@NonNull I> listener) {
        }

        @Override
        public @NonNull List<@Nullable I> getItems() {
            return List.of();
        }
    }

    static class TighteningHasListener<I> implements HasListener<I> {
        // Annotation on the parameter type itself is still reported
        @Override
        public void setListener(@NonNull Consumer<@Nullable I> listener) {
        }

        @Override
        public void setOtherListener(@Nullable Consumer<I> listener) {
        }

        @Override
        public @Nullable List<@NonNull I> getItems() {
            return null;
        }
    }
}
