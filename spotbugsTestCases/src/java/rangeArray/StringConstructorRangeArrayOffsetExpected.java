package rangeArray;

/**
 * String constructor - RANGE_ARRAY_OFFSET expected
 * 
 * @author yerayrodriguez@gmail.com
 *
 */
public class StringConstructorRangeArrayOffsetExpected {

    private static final char[] CHAR_ARRAY = { 't', 'e', 's', 't' };

    public String stringConstructor1() {
        return new String(CHAR_ARRAY, CHAR_ARRAY.length + 1, 1);
    }

    public String stringConstructor2() {
        return new String(CHAR_ARRAY, CHAR_ARRAY.length + 2, 1);
    }
}
