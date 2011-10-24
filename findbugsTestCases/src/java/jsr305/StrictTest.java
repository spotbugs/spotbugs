package jsr305;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class StrictTest {

    protected abstract void requiresStrict(@Strict Object o);

    protected abstract Object get();

    protected abstract @Strict
    Object getStrict();

    @Strict
    Object f;

    @Strict
    Object f2;

    Object g;

    @NoWarning("TQ")
    public 
    @Strict
    Object coercionFunction(Object unknown) {
        return unknown;
    }

    @ExpectWarning("TQ")
    public void violation2(Object unknown) {
        f = unknown;
    }

    @ExpectWarning("TQ")
    public void violation3(Object unknown) {
        requiresStrict(unknown);
    }

    @ExpectWarning("TQ")
    public  @Strict
    Object violation4() {
        return g;
    }

    @ExpectWarning("TQ")
    public void violation5() {
        f = g;
    }

    @ExpectWarning("TQ")
    public void violation6() {
        requiresStrict(g);
    }

    @ExpectWarning("TQ")
    public @Strict
    Object violation7() {
        return get();
    }

    @ExpectWarning("TQ")
    public void violation8() {
        f = get();
    }

    @ExpectWarning("TQ")
    public void violation9() {
        requiresStrict(get());
    }

    @NoWarning("TQ") 
    public @Strict
    Object ok1DoNotReport(@Strict Object s) {
        return s;
    }

    @NoWarning("TQ") 
    public void ok2DoNotReport(@Strict Object s) {
        f = s;
    }

    @NoWarning("TQ") 
    public void ok3DoNotReport(@Strict Object s) {
        requiresStrict(s);
    }

    @NoWarning("TQ") 
    public @Strict
    Object ok4DoNotReport() {
        return f;
    }

    @NoWarning("TQ") 
    public void ok5DoNotReport() {
        f2 = f;
    }

    @NoWarning("TQ") 
    public void ok6DoNotReport() {
        requiresStrict(f);
    }

    @NoWarning("TQ")
    public   
    @Strict
    Object ok7DoNotReport() {
        return getStrict();
    }
    @NoWarning("TQ") 
    public void ok8DoNotReport() {
        f = getStrict();
    }

    @NoWarning("TQ") 
    public void ok9DoNotReport() {
        requiresStrict(getStrict());
    }
}
