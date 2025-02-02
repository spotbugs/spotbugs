package rangeArray;

/**
 * String constructors OK
 * 
 * @author yerayrodriguez@gmail.com
 *
 */
public class StringConstructorOk {

    private static final char[] CHAR_ARRAY = { 't', 'e', 's', 't' };

    public String stringConstructor1() {
        return new String(CHAR_ARRAY, 0, CHAR_ARRAY.length);
    }

    public String stringConstructor2() {
        return new String(CHAR_ARRAY, 0, 1);
    }

    public String stringConstructor3() {
        return new String(CHAR_ARRAY, CHAR_ARRAY.length, 0);
    }

}
