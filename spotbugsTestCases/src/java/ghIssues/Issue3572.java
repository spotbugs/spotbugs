package ghIssues;

public class Issue3572 {

	// Per the issue report:
	// the warning is only reported if one of the enum cases is missing and existing cases are in order that not match enum definition order. 
	// So remove commented out "C" case - warning disappears. Leave "C" but remove "D" - warning disappears.
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
