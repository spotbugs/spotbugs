package bugPatterns;

public class DMI_INVOKING_HASHCODE_ON_ARRAY {
	void bug(int[] any) {
		any.hashCode();
	}
	void bug(long [] any ) {
		any.hashCode();
	}
	void bug(Object [] any ) {
		any.hashCode();
	}

}
