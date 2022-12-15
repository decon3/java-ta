package me.sk.ta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class Utils {
    public static LocalDateTime UtcNow() {
        return ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime();
    }
    public static LocalDate UtcToday() {
        return UtcNow().toLocalDate();
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    public static boolean IsWithinRange(LocalDate target, LocalDate from, LocalDate to) {
        return !(target.isBefore(from) || target.isAfter(to));
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(final byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static boolean byteArrayStartsWith(byte[] src, byte[] sub) {
        if (src == null || sub == null ){
            throw new RuntimeException("src or sub is null");
        }
        if (src.length < sub.length) {
            return false;
        }
        boolean matched = true;
        int at = 0;
        for (int i=0; i<sub.length; i++){
            if (src[i] != sub[i]) {
                matched = false;
                break;
            }
        }
        return matched;
    }
}
