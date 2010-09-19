package bugIdeas;

public class Ideas_2010_08_16 {

    boolean badCheck(String name) {
        return "QUIT" == name;
    }

    boolean veryBadCheck(String name) {
        return "QUIT" == name.toUpperCase();
    }

    boolean badCheck(String name, String tag) {
        return tag == name;
    }

    boolean veryBadCheck(String name, String tag) {
        return tag == name.toUpperCase();
    }

}
