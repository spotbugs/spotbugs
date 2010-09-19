package npe;

import java.io.IOException;

/**
 * False positive on exception path, reported by John Penix.
 * 
 * <pre>
 * I'm surprised about this false positive - seems obvious.  Am I missing
 * something?
 * 
 * thanks
 * 
 * John
 * 
 * ---
 * 
 * try {
 *  r = a.MyClass.getInstance()
 * }
 * catch (IOException e) {
 *  log.warn("IO Exeption" + e.getMessage());
 *  return null;
 * }
 * catch (MyException e) {
 *  log.warn("My Exeption" + e.getMessage());
 *    return null;
 * }
 * 
 * size = r.getSize();  // <--NP_NULL_ON_SOME_PATH_EXCEPTION
 * </pre>
 **/

public class FalsePositiveOnExceptionPath {

    Object getInstance(int i) throws IOException {
        switch (i) {
        case 0:
            throw new IOException();
        case 1:
            throw new IllegalArgumentException();
        case 2:
            return null;
        default:
            return "x";
        }
    }

    Object f(int i) {
        Object r;

        try {
            r = getInstance(i);
        } catch (IOException e) {
            System.out.println("IO Exeption" + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            System.out.println("My Exeption" + e.getMessage());
            return null;
        }

        int h = r.hashCode(); // <--NP_NULL_ON_SOME_PATH_EXCEPTION
        if (h == 42)
            return null;
        return r;
    }

}
