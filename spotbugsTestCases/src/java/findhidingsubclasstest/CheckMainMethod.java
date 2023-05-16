package findhidingsubclasstest;

interface Secure {// Or is it?
    final static long[] CreditCardNumbers = { 12848392, 12842642, 12384623, 192369, 1203789 };
}

class DumbPublicClass implements Secure {
    public static void main(String[] args) {
        System.out.println("I control the credit card numbers in Secure, but I'm not final!");
    }
}

public class CheckMainMethod extends DumbPublicClass // Hoohoo!
{
    public static void main(String[] args) {
        // MUAUAHAHAHAHAHAHHAHA
        // MUAUAHAHAHAHAHAHHAHA
        System.out.println(CheckMainMethod.CreditCardNumbers[0]);
    }
}
