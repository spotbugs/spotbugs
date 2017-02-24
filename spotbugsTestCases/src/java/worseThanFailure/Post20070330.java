package worseThanFailure;

// http://worsethanfailure.com/Articles/The_Longest_Way_to_Zero.aspx
public class Post20070330 {

    public static String f(String amount) {
        if (amount != null && amount.trim().length() > 0) {
            for (int i = 0; i < amount.length(); i++) {
                char c = amount.charAt(i);
                // System.out.println(i + "  " + String.valueOf(c));
                if (!String.valueOf(c).equals("0")) {
                    // System.out.println(i);
                    if (amount.substring(i).endsWith("00")) {
                        amount = amount.substring(i, amount.length() - 2) + ".00";
                    } else {
                        // System.out.println("old amount" + amount);
                        if (i + 2 < amount.length()) {
                            amount = amount.substring(i, amount.length() - 2) + "."
                                    + amount.substring(amount.length() - 2, amount.length());
                        } else {
                            if (i + 2 == amount.length()) {
                                amount = "0." + amount.substring(i);
                            } else {
                                amount = "0.0" + amount.substring(i);
                            }
                        }
                        // System.out.println("new amount" + amount);
                    }
                    break;
                }
            }
        }
        return amount;
    }

    public static void main(String args[]) {
        System.out.println(f("00028000"));
        System.out.println(f("0002800"));
        System.out.println(f("000280"));
        System.out.println(f("00028"));
        System.out.println(f("28000"));
        System.out.println(f("2800"));
        System.out.println(f("280"));
        System.out.println(f("28"));
    }
}
