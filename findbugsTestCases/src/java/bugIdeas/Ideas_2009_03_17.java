package bugIdeas;

public class Ideas_2009_03_17 {

    public static final Long SPECIAL = -1L;

    public static boolean isSpecial(Long l) {
        return l == null || l == SPECIAL;
    }

    public static boolean areEqual(Long l1, Long l2) {
        return l1 == l2;
    }

}
