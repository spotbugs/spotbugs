package ghIssues;

public abstract class Issue688 {
	public void invoker() {
		// Method invocation in MethodHandles in Java 9,
		// that caused problem to generate OpcodeStack.Item instance
		method(boolean.class, true);
	}

	abstract void method(Class<?> clazz, Object object);
}
