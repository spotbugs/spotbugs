package sfBugs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Bug1862705 implements Serializable {

    private static final long serialVersionUID = 1L;

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {

        s.defaultReadObject();

    }

    private void writeObject(ObjectOutputStream s) throws IOException {

        s.defaultWriteObject();

    }

    public static class Bar extends Bug1862705 {

        private static final long serialVersionUID = 1L;

        private transient String s = null;

        public Bar(String ss) {

            s = ss;

        }

        public String getS() {

            return s;

        }

        public void setS(String ss) {

            s = ss;

        }

    }

}
