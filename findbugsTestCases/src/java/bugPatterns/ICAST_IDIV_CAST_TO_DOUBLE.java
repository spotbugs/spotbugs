package bugPatterns;

public class ICAST_IDIV_CAST_TO_DOUBLE {

    void bug(int x, int y) {
        double d = (x / y);
    }

}
