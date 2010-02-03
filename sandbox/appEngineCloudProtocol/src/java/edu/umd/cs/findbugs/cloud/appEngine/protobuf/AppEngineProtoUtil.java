package edu.umd.cs.findbugs.cloud.appEngine.protobuf;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.protobuf.ByteString;

public final class AppEngineProtoUtil {
	private AppEngineProtoUtil() { }

	public static ByteString encodeHash(String hash) {
		ByteString result = ByteString.copyFrom(new BigInteger(hash, 16).toByteArray());
		assert hash.equals(decodeHash(result));
		return result;
	}

	public static String decodeHash(ByteString hash) {
		String hashStr = new BigInteger(1, hash.toByteArray()).toString(16);
		return normalizeHash(hashStr);
	}

	public static String normalizeHash(String hashStr) {
		hashStr = hashStr.toLowerCase();
		if (hashStr.length() < 32) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < (32 - hashStr.length()); i++) {
				builder.append('0');
			}
			builder.append(hashStr);
			hashStr = builder.toString();
			assert hashStr.length() == 32;
		}
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
