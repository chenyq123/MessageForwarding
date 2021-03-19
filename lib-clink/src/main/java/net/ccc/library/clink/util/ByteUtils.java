package net.ccc.library.clink.util;

public class ByteUtils {
    public static boolean startsWith(byte[] source, byte[] match) {
        return startsWith(source, 0, match);
    }

    public static boolean startsWith(byte[] source, int offset, byte[] match) {
        if (match.length > (source.length - offset)) {
            return false;
        }
        for (int i = 0; i < match.length; i++) {
            if (source[offset + i] != match[i]) {
                return false;
            }
        }
        return true;
    }
}
