package bugIdeas;

public class Ideas_2009_05_30 {

    int x;

    Ideas_2009_05_30 next;

    void copyInto(Ideas_2009_05_30 that) {
        that.x = that.x;
    }

    static void copy(Ideas_2009_05_30 a, Ideas_2009_05_30 b) {
        a.x = a.x;
    }

    static void messWith(Ideas_2009_05_30 a) {
        a.x = a.x;
    }

}
