package sfBugsNew;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1230 {

    @NoWarning(value="OS_OPEN_STREAM", confidence=Confidence.MEDIUM)
    public void writeObject(String file, Serializable serializable) throws FileNotFoundException, IOException {
        try (ObjectOutput output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(serializable);
            }
    }
    @NoWarning("OS_OPEN_STREAM")
    public void writeObject2(String file, Serializable serializable) throws FileNotFoundException, IOException {
        try (FileOutputStream fOut = new FileOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(fOut)) {
            output.writeObject(serializable);
            }
    }
    @NoWarning("OS_OPEN_STREAM")
    public void writeObject2a(String file, Serializable serializable) throws FileNotFoundException, IOException {
        try (FileOutputStream fOut = new FileOutputStream(file);
                ObjectOutputStream output = new ObjectOutputStream(fOut)) {
            output.writeObject(serializable);
            }
    }
    @NoWarning("OS_OPEN_STREAM")
    public void writeObject2b(String file, byte b) throws FileNotFoundException, IOException {
        try (FileOutputStream fOut = new FileOutputStream(file);
                DataOutputStream output = new DataOutputStream(fOut)) {
            output.write(b);
            }
    }
    @NoWarning("OS_OPEN_STREAM")
    public void writeObject2c(String file, byte b) throws FileNotFoundException, IOException {
        try (FileOutputStream fOut = new FileOutputStream(file);
                BufferedOutputStream output = new BufferedOutputStream(fOut)) {
            output.write(b);
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
    public void writeObject4(FileOutputStream file, Serializable serializable) throws FileNotFoundException, IOException {
        try (ObjectOutput output = new ObjectOutputStream(file)) {
            output.writeObject(serializable);
            }
    }
}
