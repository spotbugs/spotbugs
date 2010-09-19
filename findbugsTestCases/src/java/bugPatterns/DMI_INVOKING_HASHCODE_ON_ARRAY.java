package bugPatterns;

public class DMI_INVOKING_HASHCODE_ON_ARRAY {

    /*
     * Entities: v1: int [] o1: invoke int [].hashCode v2: int Connections: o1
     * is invoked on v1 v2 is the result of o1
     */
    void bug(int[] any) {
        any.hashCode();
    }

    /*
     * Entities: v1: Object [] o1: invoke Object [].hashCode v2: int
     * Connections: o1 is invoked on v1 v2 is the result of o1
     */
    void bug(Object[] any) {
        any.hashCode();
    }

    void bug(long[] any) {
        any.hashCode();
    }

    void notBug(Object any) {
        any.hashCode();
    }

}
