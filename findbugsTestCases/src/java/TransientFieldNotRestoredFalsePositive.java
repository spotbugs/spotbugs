import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TransientFieldNotRestoredFalsePositive implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public void setX(int x) {
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        System.out.println("read object " + this.getClass().getName());
        setX(10);
    }

    static class SubClass extends TransientFieldNotRestoredFalsePositive {
        private static final long serialVersionUID = 1L;

        transient Integer x = 18;

        public void increment() {
            x = x + 1;
        }

    }

    public static void main(String args[]) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(new TransientFieldNotRestoredFalsePositive());
        o.writeObject(new SubClass());
        o.close();
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b.toByteArray()));
        in.readObject();
        in.readObject();
    }
}
