package sfBugs;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public final class Bug3343304 {
    public static void parse() {
        new FindBugs().getResult();
    }

    private static abstract class Abstract {
        protected abstract void parseLine();

        public void getResult() {
            parseLine();
        }

        protected void doSomething() {
        }
    }

    private static final class FindBugs extends Abstract {
        private @Nullable Map<Integer, String> metrics = null;

        @Override
        protected void parseLine() {
            if (metrics == null) {
                metrics = new HashMap<Integer, String>();
            }

            doSomething();
            System.err.println(metrics.get(0));

        }
    }

}
