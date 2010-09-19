import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TransientFieldOfNonserializableClass {
    transient public int super_test = 1;

    public TransientFieldOfNonserializableClass() {
        System.out.println("Constructing a TransientFieldOfNonserializableClass");
    }

    public void setTest(int t) {
        super_test = t;
    }

    static class Sub extends TransientFieldOfNonserializableClass implements Serializable {
        public int sub_test = 2;

        public Sub() {
            System.out.println("Constructing Sub");
        }

        public static void main(String[] args) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                Sub s = new Sub();
                s.setTest(99);
                s.sub_test = 98;
                oos.writeObject(s);
                oos.flush();
                byte[] data = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                s = (Sub) ois.readObject();
                if (s.super_test == 99)
                    System.out.println("Super class data serialized");
                else if (s.super_test == 1)
                    System.out.println("Super class data initialized in constructor");
                if (s.sub_test == 98)
                    System.out.println("Sub class data serialized");
                else if (s.super_test == 2)
                    System.out.println("Sub class data initialized in constructor");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
