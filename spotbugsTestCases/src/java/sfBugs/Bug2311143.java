package sfBugs;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@DefaultAnnotation(NonNull.class)
public class Bug2311143 {

    private static @Nullable List<String> getMagic() {
        return Collections.emptyList();
    }

    public int complain() {
        return getMagic().size();
    }

    public static final class InnerClass {

        public void doMagic() {
            List<String> contextualTabs = getMagic();
            if (contextualTabs != null) {
                System.out.println("checked for null, but still generated a warning");
            }
        }
    }

}
