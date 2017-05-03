package sfBugs;

public class Bug1584086 {

    public static void falsePositive(String... args) {
        loop: for (int i = 0; i < 5; i++) {
            switch (args.length) {
            case 0:
            case 1:
                System.out.println("some thing");
                break loop;

            case 2:
                System.out.println("some thing");
                break;

            default:
                System.out.println("Some other number of arguments");
                break;
            }
        }
    }

    public static void falsePositive2(String... args) {

        loop: for (int i = 0; i < 5; i++) {
            switch (args.length) {
            case 0:
            case 1:
                System.out.println("some thing");
                break loop;

            case 2:
                System.out.println("some thing");
                continue loop;

            default:
                System.out.println("Some other number of arguments");
                break;
            }
            System.out.println("some other");

        }

    }

    public static void main(String... args) {

        loop: for (int i = 0; i < 5; i++) {
            switch (args.length) {
            case 0:
            case 1:
                System.out.println("some thing");
                break;

            case 2:
                System.out.println("some thing");
                break;

            default:
                System.out.println("Some other number of arguments");
                break;
            }
            System.out.println("some other");

        }

    }

}
