package npe;

public class NullDeref2 {
    public void detectedNullPointerInExceptionPath() {
        Object thisIsNull = null;
        if (thisIsNull == null) {
            try {
                System.out.println("hello");
                thisIsNull = "notnull";
            } catch (RuntimeException ex) {
                // Should generate a low warning here
                System.out.println(thisIsNull.getClass());
            }
        }
    }

    public void possibleNullPointerInExceptionPath() {
        Object thisIsNull = null;
        if (thisIsNull == null) {
            try {
                System.out.println("hello");
                thisIsNull = "notnull";
            } catch (RuntimeException ex) {
            }
            // Should generate a low warning here
            System.out.println(thisIsNull.getClass());
        }
    }

    public void possibleNullPointerInNormalPath() {
        Object thisIsNull = null;
        if (thisIsNull == null) {
            try {
                System.out.println("hello");
            } catch (RuntimeException ex) {
                thisIsNull = "notnull";
            }
            // Should generate a medium warning here
            System.out.println(thisIsNull.getClass());
        }
    }
}
