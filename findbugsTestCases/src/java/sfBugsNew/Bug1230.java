package sfBugsNew;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1230 {

    @DesireNoWarning("OS_OPEN_STREAM")
    public void writeObject(String file, Serializable serializable) throws FileNotFoundException, IOException {
        try (ObjectOutput output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(serializable);
            }
    }
    @DesireNoWarning("OS_OPEN_STREAM")
    public void writeObject2(String file, Serializable serializable) throws FileNotFoundException, IOException {
        try (FileOutputStream fOut = new FileOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(fOut)) {
            output.writeObject(serializable);
            }
    }
    @NoWarning("OS_OPEN_STREAM")
    public void writeObject3(String file, Serializable serializable) throws FileNotFoundException, IOException {
        try (FileOutputStream fOut = new FileOutputStream(file);
                ObjectOutputStream output = new ObjectOutputStream(fOut)) {
            output.writeObject(serializable);
            }
    }
    @NoWarning("OS_OPEN_STREAM")
    public void writeObject(FileOutputStream file, Serializable serializable) throws FileNotFoundException, IOException {
        try (ObjectOutput output = new ObjectOutputStream(file)) {
            output.writeObject(serializable);
            }
    }
}
