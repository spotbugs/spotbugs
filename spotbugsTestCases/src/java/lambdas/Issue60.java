package lambdas;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.stream.Stream;

public class Issue60 {

    public static void create(URL url) {
        try (InputStream in = url.openStream()) {
            Properties p1 = new Properties();
            p1.load(in);
        } catch (IOException e) {
        }
    }

    public Stream<String> keys() {
        return Stream.<Properties> of()
                .flatMap(p -> p.stringPropertyNames().stream());
    }

}
