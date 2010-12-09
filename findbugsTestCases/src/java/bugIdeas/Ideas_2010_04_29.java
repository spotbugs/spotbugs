package bugIdeas;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Assert;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * More eclipse Null pointer false positives
 * 
 * From https://bugs.eclipse.org/bugs/show_bug.cgi?id=195638
 * 
 * 
 */
public class Ideas_2010_04_29 {

    @ExpectWarning("NP_NULL_ON_SOME_PATH_EXCEPTION")
    @NoWarning("NP_ALWAYS_NULL,NP_NULL_ON_SOME_PATH")
    public void test1() {
        String str = null;
        for (int i = 0; i < 2; i++) {
            try {
                str = new String("Test");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            str.charAt(i);
            // Eclipse Error : "Null pointer access: The
            // variable str can only be null at
            // this location"
            str = null;
        }
    }

    @NoWarning("NP")
    public void test2() {
        String str = null;
        for (int i = 0; i < 2; i++) {
            try {
                str = new String("Test");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            str.charAt(i);
            // Error : "Null pointer access: The
            // variable str can only be null at
            // this location"

        }
    }

    static class Test3 {

        @NoWarning("NP_ALWAYS_NULL,NP_NULL_ON_SOME_PATH")
        void m() throws SQLException {
            Connection conn = null;
            try {
                conn = createConnection();

                for (;;) {
                    throwSomething();
                }
            } catch (MyException e) {
                conn.rollback(); // The variable can never be null here...
            }
        }

        private void throwSomething() throws MyException {
            throw new MyException();
        }

        class MyException extends Exception {

        }

        private Connection createConnection() throws SQLException {
            return DriverManager.getConnection("", "", "");
        }
    }

    @NoWarning("NP_ALWAYS_NULL,NP_NULL_ON_SOME_PATH")
    public void test4() {
        String dummy = null;
        for (int i = 0; i < 10; i++) {
            if (i % 2 != 0) {
                dummy = "Foo";
            }
            System.out.println("Hello");
            if (i % 2 != 0) {
                System.out.println(dummy.toLowerCase());
            }
            dummy = null;
        }
    }

    public void test4a() {
        String dummy = null;
        for (int i = 0; i < 10; i++) {
            if (i % 2 != 0) {
                dummy = "Foo";
            }
            System.out.println("Hello");
            if (dummy != null && i % 2 != 0) {
                System.out.println(dummy.toLowerCase());
            }
            dummy = null;
        }
    }

    @NoWarning("NP_ALWAYS_NULL")
    private void test5() {
        String tblVarRpl = null;
        while (true) {
            boolean isOpenVariableMortageRateProduct = true;
            boolean tblVarRplAllElementAddedIndicator = false;
            if (isOpenVariableMortageRateProduct) {
                if (tblVarRplAllElementAddedIndicator == false)
                    tblVarRpl = "";
                tblVarRpl = tblVarRpl.substring(1); // Can only be null???
                return;
            }
        }
    }

    @NoWarning("NP_ALWAYS_NULL,NP_NULL_ON_SOME_PATH")
    public void test8(boolean fail) throws Exception {
        Object v = null;
        try {
            try {
                v = "Hello";
            } finally {
                if (fail)
                    throw new IOException();
            }
        } catch (IOException e) {
            Assert.assertEquals("Hello", v.toString());
        }
    }

    @ExpectWarning("NP_LOAD_OF_KNOWN_NULL_VALUE")
    void test9(boolean b) {
        Object o = null;
        for (int i = 0; i < 25; i++) {
            if (b) {
                if (o == null) { // should report always null
                    o = new Object();
                }
                return;
            }
        }
    }
}
