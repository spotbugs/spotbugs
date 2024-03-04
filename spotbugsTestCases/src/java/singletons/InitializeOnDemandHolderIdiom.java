package singletons;

public class InitializeOnDemandHolderIdiom {
    static class SingletonHolder {
        static InitializeOnDemandHolderIdiom instance = new InitializeOnDemandHolderIdiom();
    }

    public static InitializeOnDemandHolderIdiom getInstance() {
        return SingletonHolder.instance;
    }
}
