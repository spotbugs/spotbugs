
public class Confusify {

    String arr;

    public Confusify() {
        String arr = "Mateys";
        System.out.println(this.arr);
    }

    public void pirate() {
        System.out.println("I'm a pirate!");
    }

    public void highSeas() {
        Helper myHelper = new Helper();
        myHelper.yarrOrBlarr();
    }

    public static void main(String[] args) {
        new Confusify().highSeas();
    }

    public void yarr() {
        System.out.println("YARRRRR");
    }

    private static class YarrBlarr {
        public void yarr() {
            System.out.println("BLARRRR");
        }
    }

    private class Helper extends YarrBlarr {
        public void yarrOrBlarr() {
            yarr();
            pirate();
        }
    }

}
