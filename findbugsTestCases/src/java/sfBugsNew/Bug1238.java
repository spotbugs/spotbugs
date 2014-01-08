package sfBugsNew;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Bug1238 {

    @ExpectWarning("OS_OPEN_STREAM,OBL")
    public int simpleObviousBug(String f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        return in.read();
    }

    @ExpectWarning("OS_OPEN_STREAM,OBL")
    void test1(File f, byte b) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(b);
    }

    @ExpectWarning("OBL")
    void test2(File f, byte b) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(b);

        } finally {

            foo(fos);
        }
    }

    @NoWarning("OS_OPEN_STREAM,OBL")
    void test3(File f, byte b) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(b);

        } finally {

            close(fos);
        }
    }

    protected abstract void foo(FileOutputStream fos);

    protected abstract void close(FileOutputStream fos);

}
