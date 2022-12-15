package me.sk.ta.domain;

// source: https://stackoverflow.com/a/42531620/19731482
public class DelayedFormatterUtility {
    public static Object format(String format, Object... args) {
        return new Object() {
            @Override
            public String toString() {
                return String.format(format, args);
            }
        };
    }
}
