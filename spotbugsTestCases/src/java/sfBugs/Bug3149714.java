package sfBugs;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Bug3149714 {
    public enum Inner { A }
}
