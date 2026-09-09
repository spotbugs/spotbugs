package singletons;

class InappropriateSynchronization {
    private static InappropriateSynchronization instance;

    private InappropriateSynchronization() {
    }

    public static InappropriateSynchronization getInstance() {
        if (instance == null) {
          synchronized (InappropriateSynchronization.class) {
            instance = new InappropriateSynchronization();
          }
        }
        return instance;
    }
}
