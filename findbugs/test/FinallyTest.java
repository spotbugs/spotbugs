import java.io.*;
import java.util.*;

public class FinallyTest {

  public void finallyTest () {

    InputStream in = null;

    try {
      in = new FileInputStream( "foo.txt" );
      byte head[] = new byte[8];
      if ( in.read( head ) == 8 ) {;
        System.out.println( head );
      }
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    // although bad form it is legal
    finally {
      try {
         if ( in != null ) {
           in.close();
         }
      }
      catch ( Exception e2 ) {
        e2.printStackTrace();
      }
    }
  }

}

