package ghIssues.issue543;

import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
public abstract class FoobarValue {

    public abstract int foo();

    public abstract String bar();

    public abstract List<Integer> buz();

    public abstract Set<Long> crux();
}
