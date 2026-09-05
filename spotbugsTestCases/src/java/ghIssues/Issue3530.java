package ghIssues;

public class Issue3530 {

	// Issue 3530
    public String switchForLoop(int value) {
        String valueType = null;
        switch (value % 2) {
            case 0:
                valueType = "even";        // this falls through and is over-written by valueType = "odd";
                for (int i = 0; i < 5; i++) {
                    System.out.println("Nice even value: " + i);
                }
            case 1:
                valueType = "odd";
                break;
            default:
                valueType = "invalid number";
        }
        return valueType;
    }
    
    // Issue 3449
    public int switchWhileLoop(int input) {
        int i = 0;
        int result = 0;
        switch (input) {
            case 1:
                result = 10;
                while (i < input) {
                    System.out.println(i);
                    i++;
                }
            case 2:
                throw new IllegalArgumentException("Invalid input");
            default:
                result = input * 2;
        }
        return result;
    }
}
