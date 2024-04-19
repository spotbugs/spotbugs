package singletons;

/**
 * See https://github.com/spotbugs/spotbugs/issues/2934
 */
public class NotSingletonWithFactoryMethod {

    private static final NotSingletonWithFactoryMethod INSTANCE = new NotSingletonWithFactoryMethod("1");

    private final String text;

    private NotSingletonWithFactoryMethod(String text) {
        this.text = text;
    }

    public static NotSingletonWithFactoryMethod instance1() {
        return INSTANCE;
    }

    public static NotSingletonWithFactoryMethod create(String text) {
        return new NotSingletonWithFactoryMethod("text");
    }

    public String getText() {
        return text;
    }
}
