package singletons;

import java.util.Arrays;

public enum EnumSingleton {
    ; // Empty list of enum values

    private static EnumSingleton instance;

    // Nontransient field
    private String[] str = { "one", "two", "three" };

    public void displayStr() {
        System.out.println(Arrays.toString(str));
    }
}
