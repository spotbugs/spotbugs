package jsr305;

public abstract class StrictTest {
	
	protected abstract void requiresStrict(@Strict Object o);
	protected abstract Object get();
	protected abstract @Strict Object getStrict();
	
	@Strict Object f;
	@Strict Object f2;
	Object g;
	
	public @Strict Object violation1(Object unknown) {
		return unknown;
	}
	
	public void violation2(Object unknown) {
		f = unknown;
	}
	
	public void violation3(Object unknown) {
		requiresStrict(unknown);
	}
	
	public @Strict Object violation4() {
		return g;
	}
	
	public void violation5() {
		f = g;
	}
	
	public void violation6() {
		requiresStrict(g);
	}
	
	public @Strict Object violation7() {
		return get();
	}
	
	public void violation8() {
		f = get();
	}
	
	public void violation9() {
		requiresStrict(get());
	}
	
	public @Strict Object ok1DoNotReport(@Strict Object s) {
		return s;
	}
	
	public void ok2DoNotReport(@Strict Object s) {
		f = s;
	}
	
	public void ok3DoNotReport(@Strict Object s) {
		requiresStrict(s);
	}
	
	public @Strict Object ok4DoNotReport() {
		return f;
	}
	
	public void ok5DoNotReport() {
		f2 = f;
	}
	
	public void ok6DoNotReport() {
		requiresStrict(f);
	}
	
	public @Strict Object ok7DoNotReport() {
		return getStrict();
	}
	
	public void ok8DoNotReport() {
		f = getStrict();
	}
	
	public void ok9DoNotReport() {
		requiresStrict(getStrict());
	}
}
