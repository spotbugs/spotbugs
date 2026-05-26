package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class Issue3457WithLiteral {
    private static Issue3457WithLiteral instance;

    private Issue3457WithLiteral() {
    }

    @ExpectWarning("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED")
    public static Issue3457WithLiteral getInstance() {
        if (instance == null) {
            int value = 0;
            switch (value) {
            case 1:
                break;
            }
            instance = new Issue3457WithLiteral();
        }
        return instance;
    }
}

class Issue3457WithHelper {
    private static Issue3457WithHelper instance;

    private Issue3457WithHelper() {
    }

    @ExpectWarning("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED")
    public static Issue3457WithHelper getInstance() {
        if (instance == null) {
            int value = getUnreachableValue();
            switch (value) {
            case 1:
                break;
            }
            instance = new Issue3457WithHelper();
        }
        return instance;
    }

    private static int getUnreachableValue() {
        return 0;
    }
}

class Issue3457SynchronizedHelper {
    private static Issue3457SynchronizedHelper instance;

    private Issue3457SynchronizedHelper() {
    }

    @NoWarning("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED")
    public static Issue3457SynchronizedHelper getInstance() {
        if (instance == null) {
            int value = getSynchronizedValue();
            instance = new Issue3457SynchronizedHelper();
        }
        return instance;
    }

    private static int getSynchronizedValue() {
        synchronized (Issue3457SynchronizedHelper.class) {
            return 0;
        }
    }
}
