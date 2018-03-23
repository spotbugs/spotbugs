package rangeArray;

import java.io.File;
import java.io.FileReader;

/**
 * RANGE_ARRAY_LENGHT and RANGE_ARRAY_OFFSET false negative examples
 * 
 * @author yerayrodriguez@gmail.com
 *
 */
public class RangeArrayLengthAndOffsetBugExample {

    private static final char[] CHAR_ARRAY = { 't', 'e', 's', 't' };
    private static final int BUFFER_LENGTH = 10;

    public void ioOperation() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            // OK
            fr.read(buffer, 0, BUFFER_LENGTH);
        } catch (Exception e) {
        }
    }

    public void ioOperation2() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            // Expected RANGE_ARRAY_LENGHT #1
            fr.read(buffer, 0, BUFFER_LENGTH + 1);
        } catch (Exception e) {
        }
    }

    public void ioOperation3() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            // Expected RANGE_ARRAY_LENGHT #2
            fr.read(buffer, 0, BUFFER_LENGTH + 2);
        } catch (Exception e) {
        }
    }

    public void ioOperation4() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            // Expected RANGE_ARRAY_LENGHT #3
            fr.read(buffer, BUFFER_LENGTH, 1);
        } catch (Exception e) {
        }
    }

    public void ioOperation5() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            // Expected RANGE_ARRAY_OFFSET #1
            // Expected RANGE_ARRAY_LENGHT #4
            fr.read(buffer, BUFFER_LENGTH + 1, 1);
        } catch (Exception e) {
        }
    }

    public void ioOperation6() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            // Expected RANGE_ARRAY_OFFSET #2
            // Expected RANGE_ARRAY_LENGHT #5
            fr.read(buffer, BUFFER_LENGTH + 2, 1);
        } catch (Exception e) {
        }
    }

    public String stringConstructor() {
        // OK
        return new String(CHAR_ARRAY, 0, CHAR_ARRAY.length);
    }

    public String stringConstructor2() {
        // Expected RANGE_ARRAY_LENGHT #6
        return new String(CHAR_ARRAY, 0, CHAR_ARRAY.length + 1);
    }

    public String stringConstructor3() {
        // Expected RANGE_ARRAY_LENGHT #7
        return new String(CHAR_ARRAY, 0, CHAR_ARRAY.length + 2);
    }

    public String stringConstructor4() {
        // Expected RANGE_ARRAY_LENGHT #8
        return new String(CHAR_ARRAY, 1, CHAR_ARRAY.length);
    }

    public String stringConstructor5() {
        // Expected RANGE_ARRAY_LENGHT #9
        return new String(CHAR_ARRAY, 2, CHAR_ARRAY.length);
    }

    public String stringConstructor6() {
        // Expected RANGE_ARRAY_LENGHT #10
        return new String(CHAR_ARRAY, 1, CHAR_ARRAY.length + 1);
    }

    public String stringConstructor7() {
        // OK
        return new String(CHAR_ARRAY, CHAR_ARRAY.length, 0);
    }

    public String stringConstructor8() {
        // Expected RANGE_ARRAY_LENGTH #11
        return new String(CHAR_ARRAY, CHAR_ARRAY.length, 1);
    }

    public String stringConstructor9() {
        // Expected RANGE_ARRAY_OFFSET #3
        // Expected RANGE_ARRAY_LENGTH #12
        return new String(CHAR_ARRAY, CHAR_ARRAY.length + 1, 1);
    }

    public String stringConstructor10() {
        // Expected RANGE_ARRAY_OFFSET #4
        // Expected RANGE_ARRAY_LENGTH #13
        return new String(CHAR_ARRAY, CHAR_ARRAY.length + 2, 1);
    }
}
