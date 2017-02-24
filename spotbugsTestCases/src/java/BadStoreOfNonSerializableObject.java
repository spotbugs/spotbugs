import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class BadStoreOfNonSerializableObject implements Serializable {

    private static final long serialVersionUID = 0;

    Object x;

    @ExpectWarning("Se")
    NotSerializable y;

    static final class NotSerializable {
    }

    @DesireWarning("Se")
    BadStoreOfNonSerializableObject() {
        x = new NotSerializable();
        y = new NotSerializable();
    }

}
