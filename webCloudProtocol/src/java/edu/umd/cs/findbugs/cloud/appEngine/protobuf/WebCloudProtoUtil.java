package edu.umd.cs.findbugs.cloud.appEngine.protobuf;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.protobuf.ByteString;

public final class WebCloudProtoUtil {
    private WebCloudProtoUtil() {
    }

    public static ByteString encodeHash(String hash) {
        ByteString result = ByteString.copyFrom(new BigInteger(hash, 16).toByteArray());
        return result;
    }

    public static String decodeHash(ByteString hash) {
        String hashStr = new BigInteger(1, hash.toByteArray()).toString(16);
        hashStr = normalizeHash(hashStr);
        ByteString reEncoded = encodeHash(hashStr);
        assert hashStr.equals("0") || hash.equals(reEncoded) : "re-encoding failed for " + hashStr + ": got "
                + decodeHash(reEncoded);
        return hashStr;
    }

    public static String normalizeHash(String hashStr) {
        hashStr = hashStr.toLowerCase();
        int leadingZeroes;
        for (leadingZeroes = 0; hashStr.charAt(leadingZeroes) == '0' && leadingZeroes < hashStr.length() - 1; leadingZeroes++)
            ;
        if (leadingZeroes > 0)
            return hashStr.substring(leadingZeroes);
        return hashStr;
    }

    public static List<String> decodeHashes(Collection<ByteString> myIssueHashesList) {
        List<String> result = new ArrayList<String>(myIssueHashesList.size());
        for (ByteString byteString : myIssueHashesList) {
            result.add(decodeHash(byteString));
        }
        return result;
    }

    public static List<ByteString> encodeHashes(Collection<String> hashStrings) {
        List<ByteString> result = new ArrayList<ByteString>(hashStrings.size());
        for (String string : hashStrings) {
            result.add(encodeHash(string));
        }
        return result;
    }
}
