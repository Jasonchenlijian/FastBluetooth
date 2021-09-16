package com.clj.fastbluetooth.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class ConvertUtils {

    private static final char[] HEX_DIGITS_UPPER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] HEX_DIGITS_LOWER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytes2HexString(byte[] bytes) {
        return bytes2HexString(bytes, false);
    }

    public static String bytes2HexString(byte[] bytes, boolean isUpperCase) {
        if (bytes == null) {
            return "";
        } else {
            char[] hexDigits = isUpperCase ? HEX_DIGITS_UPPER : HEX_DIGITS_LOWER;
            int len = bytes.length;
            if (len <= 0) {
                return "";
            } else {
                char[] ret = new char[len << 1];
                int i = 0;

                for (int var6 = 0; i < len; ++i) {
                    ret[var6++] = hexDigits[bytes[i] >> 4 & 15];
                    ret[var6++] = hexDigits[bytes[i] & 15];
                }

                return new String(ret);
            }
        }
    }

    public static byte[] string2Bytes(String string) {
        return string2Bytes(string, "");
    }

    public static byte[] string2Bytes(String string, String charsetName) {
        if (string == null) {
            return null;
        } else {
            try {
                return string.getBytes(getSafeCharset(charsetName));
            } catch (UnsupportedEncodingException var3) {
                var3.printStackTrace();
                return string.getBytes();
            }
        }
    }

    private static String getSafeCharset(String charsetName) {
        String cn = charsetName;
        if (isSpace(charsetName) || !Charset.isSupported(charsetName)) {
            cn = "UTF-8";
        }
        return cn;
    }

    public static boolean isSpace(String s) {
        if (s != null) {
            int i = 0;

            for (int len = s.length(); i < len; ++i) {
                if (!Character.isWhitespace(s.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

}
