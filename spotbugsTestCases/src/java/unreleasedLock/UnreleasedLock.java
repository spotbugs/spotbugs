package unreleasedLock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UnreleasedLock {
    private final Lock lock = new ReentrantLock();

    public void doSomething(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            lock.lock();

            // Perform operations on the open file
            byte[] bytes = new byte[10];
            int a = in.read(bytes);
            System.out.println("!!!!AFTER READ!!!!");
            System.out.println(a);

            lock.unlock();
        } catch (IOException x) {
            // Handle exception
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException x) {
                    // Handle exception
                }

            }
        }
    }
}