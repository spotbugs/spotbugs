package sfBugsNew;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireWarning;

abstract class Bug1234 implements Serializable, Runnable {

}

class Main {

   private static final String fileName = "testfile.bin";

   private static Bug1234 writeRead(final Bug1234 original) {
      try {
         final ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(fileName));

         outputStream.writeObject(original);
         outputStream.close();

         final ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(fileName));

         final Bug1234 result = (Bug1234)inputStream.readObject();
         inputStream.close();

         return result;
      } catch (final Throwable e) {
         throw new RuntimeException("Error: " + e);
      }
   }


   public static void main(final String[] args) {
        final Bug1234 sr = Utils.create(new Runnable() {
            @Override
            public void run() {
                System.out.println("Will never run!");
            }
        });

      final Bug1234 newObject = writeRead(sr);

      newObject.run();
   }
}

class Utils {
    @DesireWarning("Se")
    public static Bug1234 create(final Runnable action) {
        return new Bug1234() {
            @Override
            public void run() {
                action.run();
            }
        };
   }
}
