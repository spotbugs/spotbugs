package ghIssues;

import org.junit.jupiter.api.Nested;

public class Issue560 {

	// non compliant
	public class NotANestedTest {
	}
	
	// compliant
	@Nested
	public class NestedTest {
	}
}
