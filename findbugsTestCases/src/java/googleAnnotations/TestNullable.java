package googleAnnotations;

import com.google.common.base.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TestNullable {
	static void foo(@Nullable Object o) {}
	static void bar() {
		foo(null);
	}

}
