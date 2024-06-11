package singletons;

public enum OneEnumSingleton {
    INSTANCE; // Only one element

    // Nontransient field
    int value;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
