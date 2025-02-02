package rangeArray;

import java.io.File;
import java.io.FileReader;

/**
 * java/io read operations Ok
 * 
 * @author yerayrodriguez@gmail.com
 *
 */
public class IoOperationOk {

    private static final int BUFFER_LENGTH = 10;

    public void ioOperation1() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            fr.read(buffer, 0, 1);
        } catch (Exception e) {
        }
    }

    public void ioOperation2() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            fr.read(buffer, 0, BUFFER_LENGTH);
        } catch (Exception e) {
        }
    }

    public void ioOperation3() {
        File file = new File("temp.txt");
        try (FileReader fr = new FileReader(file)) {
            char[] buffer = new char[BUFFER_LENGTH];
            fr.read(buffer, BUFFER_LENGTH, 0);
        } catch (Exception e) {
        }
    }

}
