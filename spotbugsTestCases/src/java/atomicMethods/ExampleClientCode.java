package atomicMethods;

public class ExampleClientCode {

    private final USCurrency currency = new USCurrency();
    // ...

    public ExampleClientCode() {

        Thread t1 = new Thread(() -> currency.setQuarters(1).setDimes(1));
        t1.start();

        Thread t2 = new Thread(this::setProperties);
        t2.start();

        //...
    }

    private void setProperties() {
        currency.setQuarters(2).setDimes(2);
    }

    private void hami() {
        currency.setQuarters(1).setDimes(1);
    }
}
