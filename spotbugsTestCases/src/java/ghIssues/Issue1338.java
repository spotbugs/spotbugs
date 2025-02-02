package ghIssues;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;

public class Issue1338 {
    public static KeyStore loadKeyStore(final Path path) throws IOException, KeyStoreException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            // final KeyStore keyStore = KeyStore.getInstance("JKS");
            // keyStore.load(inputStream, password.toCharArray());
            // return keyStore;
            return KeyStore.getInstance("JKS");
        }
    }
}
