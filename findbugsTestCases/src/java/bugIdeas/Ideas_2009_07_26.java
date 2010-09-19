package bugIdeas;

import javax.annotation.CheckForNull;

public class Ideas_2009_07_26 {
    int x;

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != Ideas_2009_07_26.class)
            return false;
        Ideas_2009_07_26 other = (Ideas_2009_07_26) obj;
        if (x != other.x)
            return false;
        return true;
    }

    public static int getHash(@CheckForNull Object o) {
        return o.hashCode();
    }

    public Ideas_2009_07_26(int x) {
        this.x = x;
    }

    @Override
    public int hashCode() {
        return x;
    }

}
