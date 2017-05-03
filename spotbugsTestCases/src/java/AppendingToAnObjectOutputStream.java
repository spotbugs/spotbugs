import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class AppendingToAnObjectOutputStream {

    @ExpectWarning("IO")
    public static void appendObject1(File f, Object o) throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f, true));
        out.writeObject(o);
        out.close();
    }

    @ExpectWarning("IO")
    public static void appendObject2(File f, Object o) throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f, true)));
        out.writeObject(o);
        out.close();
    }

    @ExpectWarning("IO")
    public static void appendObject3(String f, Object o) throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f, true));
        out.writeObject(o);
        out.close();
    }

    @ExpectWarning("IO")
    public static void appendObject4(String f, Object o) throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f, true)));
        out.writeObject(o);
        out.close();
    }
}
