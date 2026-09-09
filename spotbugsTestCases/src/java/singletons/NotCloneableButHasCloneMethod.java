package singletons;

public class NotCloneableButHasCloneMethod {
    private static NotCloneableButHasCloneMethod instance;

    private NotCloneableButHasCloneMethod() {
    }

    public static synchronized NotCloneableButHasCloneMethod getInstance() {
        if (instance == null) {
            instance = new NotCloneableButHasCloneMethod();
        }
        return instance;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
