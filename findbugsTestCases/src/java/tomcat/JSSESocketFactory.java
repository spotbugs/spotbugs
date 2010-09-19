package tomcat;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class JSSESocketFactory {
    private KeyStore initKeyStore(String keystoreFile, String keyPass) throws IOException {
        java.io.InputStream istream = null;
        KeyStore kstore;
        try {
            kstore = KeyStore.getInstance("JKS");
            istream = new FileInputStream(keystoreFile);
            kstore.load(istream, keyPass.toCharArray());
            return kstore;
        } catch (KeyStoreException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        } catch (CertificateException e) {
            throw new IOException(e.getMessage());
        }

    }

}
