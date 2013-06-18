package sfBugsNew;

public class Bug1172 {
    
    public int testFallThrough(int x) {
        
        switch (x) {
        case 0:
            System.out.println("Hello");
            // intentional fall-through"
        case 1:
            return 17;
        case 2:
            return 345;
        }
        return 0;
    }

}
