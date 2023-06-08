package edu.umd.cs.findbugs.sarif;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The GUID calculator class has methods to calculate UUIDs based on UUID version 5. UUID version 5 relies on on the
 * cryptographic hash SHA1 as part of the UUID calculation.
 *
 * @author Jeremias Eppler
 */
public class GUIDCalculator {

    /**
     * UUID version 5, but without using a namespace.
     *
     * @param name
     * @return UUID version 5
     */
    public static UUID fromString(@NonNull String name) {
        return calculateUUID5(null, name);
    }

    /**
     * Returns a UUID version 5 based on RFC 4122 (see: https://www.ietf.org/rfc/rfc4122.txt). It uses SHA1 to calculate
     * the UUID.
     *
     * @param namespace
     * @param name
     * @return UUID version 5
     */
    public static UUID fromNamespaceAndString(@NonNull UUID namespace, @NonNull String name) {
        return calculateUUID5(namespace, name);
    }

    private static UUID calculateUUID5(UUID namespace, String name) {
        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalError("The Hash Algorithm SHA-1 is not supported");
        }

        if (namespace != null) {
            messageDigest.update(convertUUIDToBytes(namespace));
        }

        messageDigest.update(name.getBytes(StandardCharsets.UTF_8));

        // get the digest in bytes
        byte[] digest = messageDigest.digest();

        // for more details have a look at:
        // - https://stackoverflow.com/a/40230410
        // - https://stackoverflow.com/a/28776880
        digest[6] &= 0x0f; // clear version
        digest[6] |= 0x50; // set to version 5
        digest[8] &= 0x3f; // clear variant
        digest[8] |= 0x80; // set to IETF variant

        return convertUUIDToBytes(digest);
    }

    // idea from: https://www.baeldung.com/java-byte-array-to-uuid
    private static UUID convertUUIDToBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, 16);

        // reads the first 8-bytes
        long high = byteBuffer.getLong();
        // reads the second 8-bytes
        long low = byteBuffer.getLong();

        return new UUID(high, low);
    }

    // idea from: https://www.baeldung.com/java-byte-array-to-uuid
    private static byte[] convertUUIDToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        return buffer.array();
    }
}
