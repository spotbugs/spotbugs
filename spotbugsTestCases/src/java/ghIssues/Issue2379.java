package ghIssues;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.MethodSource;

public class Issue2379 {
	private static Stream<String> values() {
		return Stream.of("one", "two");
	}
	
	@MethodSource("values")
	public void test(String value) {
	}
}
