package sfBugs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class Bug1669047 {
	public interface ISessionProvider {
		public @CheckForNull Object getSession();
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
