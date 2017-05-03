package sfBugs;

public class Bug1710812 {

    int f(int len, int end) {
        int off = 0;
        int sbOff = off;
        int sbEnd = off + len;
        while (sbOff < sbEnd) {
            int d = 7;
            sbOff += d;
            end += d;

        }
        return end;
    }
}
