package sfBugs;

import java.net.URI;

import javax.annotation.Nonnull;

import com.google.common.base.Function;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3589328 {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public static class UriToString implements Function<URI, String> {

        @Override
        @NoWarning(value="NP")
        public String apply(URI input) {
          if (input == null) {
            return null;
          } else {
            return input.toString();
          }
        }
    }


}
