package ghIssues;

import java.util.Date;

public class Issue3824 {
    private final Date date;
    private final Object launcher;

    public Issue3824(Date date) {
        this(date, createLauncher());
    }

    private Issue3824(Date date, Object launcher) {
        this.date = date;
        this.launcher = launcher;
    }

    public static class DirectAssignment {
        private final Date date;
        private final Object launcher;

        public DirectAssignment(Date date) {
            this.date = date;
            this.launcher = createLauncher();
        }
    }

    private static Object createLauncher() {
        return new Object();
    }
}
