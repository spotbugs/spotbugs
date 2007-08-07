package jsr305.package1;

public interface InterfaceWithDefaultUntaintedParams {
	// This method requires an Untainted parameter because it is
	// inherited from the enclosing package.
	public void requiresUntaintedParam(Object o);
}
