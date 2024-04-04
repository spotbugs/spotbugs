package atomicMethods;

public class USCurrency {

    private int quarters = 0;
    private int dimes = 0;
    private int nickels = 0;
    private int pennies = 0;

    public USCurrency() {}

    // Setter methods
    public USCurrency setQuarters(int quantity) {
        quarters = quantity;
        return this;
    }
    public USCurrency setDimes(int quantity) {
        dimes = quantity;
        return this;
    }
    public USCurrency setNickels(int quantity) {
        nickels = quantity;
        return this;
    }
    public USCurrency setPennies(int quantity) {
        pennies = quantity;
        return this;
    }
}
