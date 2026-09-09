package endOfStreamCheck;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class GoodEndOfStreamCheck {
    void goodFileInputStream() {
        try (FileInputStream in = new FileInputStream("test.txt")) {
            int inbuff;
            byte data;
            while ((inbuff = in.read()) != -1) {
                data = (byte) inbuff;
                System.out.println("Read byte: " + data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void goodFileReader() {
        try (FileReader in = new FileReader("test2.txt")) {
            char data;
            int inbuff;
            while ((inbuff = in.read()) != -1) {
                data = (char) inbuff;
                System.out.println("Read character: " + data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
}
