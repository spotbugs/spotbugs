package npe;

public class FalsePositive {

    public int falsePositive(String s) {
        int len = (s != null) ? s.length() : 0;
        if (len < 14 || s.charAt(12) != ':')
            throw new IllegalArgumentException();
        return s.length();
    }

}
