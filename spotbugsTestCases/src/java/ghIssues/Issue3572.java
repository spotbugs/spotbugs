package ghIssues;

public class Issue3572 {

	public Object withSwitchDefault(MyEnum e) {
		Object o = null;
		switch (e) {
		case A:
			o = "A";
			break;
		case B:
			o = "B";
			break;
//        case C:
//            o = "C";
//            break;
		case D:
			o = "D";
			break;
		default:
			break;
		}
		return o;
	}


	public Object missingSwitchDefault(MyEnum e) {
		Object o = null;
		switch (e) {
			case A:
				o = "A";
				break;
			case B:
				o = "B";
				break;
//        case C:
//            o = "C";
//            break;
			case D:
				o = "D";
				break;
		}
		return o;
	}

	public enum MyEnum {
		A, B, C, D, E
	}
}
