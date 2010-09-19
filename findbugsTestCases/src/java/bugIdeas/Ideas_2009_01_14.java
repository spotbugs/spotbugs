package bugIdeas;

public class Ideas_2009_01_14 {

//	static String getNameCorrect(int value) {
//		String result = "";
//		switch (value) {
//		case 0:
//				result = "zero";
//			break;
//		case 1:
//			result = "one";
//		break;
//		case 2:
//			result = "two";
//		break;
//		case 3:
//			result = "three";
//		break;
//		case 4:
//			result = "four";
//		break;
//		default:
//			throw new IllegalArgumentException("Illegal agrument: " + value);
//
//		}
//		return "Answer is " + result;
//	}
    static String getNameBroken(int value) {
        String result = "";
        switch (value) {
		case 0:
                result = "zero";
            break;
        case 1:
			result = "one";
        break;
        case 2:
            result = "two";
			break;
        case 3:
            result = "three";
        break;
		case 4:
            result = "four";
        default:
            throw new IllegalArgumentException("Illegal agrument: " + value);
			
        }
        return "Answer is " + result;
    }
	
    public static void main(String args[]) {
        System.out.printf("%d\n", 100.0);
    }
}
