package sfBugs;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bug1669047 {
    public interface ISessionProvider {
        public @Nullable Object getSession();
    }

    private @NonNull ISessionProvider fProvider;

    public Bug1669047(@NonNull ISessionProvider provider) {
        fProvider = provider;
    }

    public void test() {
        verify(fProvider.getSession());
        Object checkForNullResult = fProvider.getSession();
        verify(checkForNullResult);
    }

    private void verify(@NonNull Object obj) {
        obj.getClass();
    }
}
