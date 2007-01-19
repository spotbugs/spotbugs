package npe;

public class CorrectlyLocalizeWarning {

    int f(int x) {
        String s = null;
        if (x == 1)
            s = "1";
        else if (x == 2)
            throw new IllegalArgumentException();
        else if (x == 3) {
           System.out.println("Impossible");
        }else if (x == 4)
            s = "4";
        else if (x == 5)
            s = "5";
        else throw new IllegalArgumentException();
        
        return s.hashCode();

    }

}
