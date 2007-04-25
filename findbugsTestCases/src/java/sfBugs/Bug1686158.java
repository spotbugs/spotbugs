package sfBugs;
/**
 * @author Bogdan Dimitriu
 *
 */
public class Bug1686158 {
	public static void main(String[] args) {
		Integer i = getSomeI();
		if(i != null && i.intValue() != 0) {
            processValueA(i);
		} else {
			processValueB(i);
		}
    }

	private static Integer getSomeI() {
		return null;
    }

	public static void processValueA(Integer i) {
		System.out.println(i + 1);
    }

	public static void processValueB(Integer i) {
		System.out.println(i + 2);
    }
}
