package sfBugs;

import com.github.spotbugs.jsr305.annotation.concurrent.Immutable;

@Immutable
public class Bug3149714 {
    public enum Inner { A }
}
