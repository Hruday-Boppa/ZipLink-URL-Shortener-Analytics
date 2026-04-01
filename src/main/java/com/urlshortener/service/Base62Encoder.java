package com.urlshortener.service;

import java.math.BigInteger;
import java.util.Objects;

public final class Base62Encoder {

    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length);

    private Base62Encoder() {
    }

    public static String encode(BigInteger value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
        if (value.equals(BigInteger.ZERO)) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        BigInteger current = value;
        while (current.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = current.divideAndRemainder(BASE);
            builder.append(ALPHABET[divRem[1].intValue()]);
            current = divRem[0];
        }
        return builder.reverse().toString();
    }

    public static String toFixedLength(byte[] bytes, int length) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (length < 4) {
            throw new IllegalArgumentException("length must be at least 4");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("bytes must not be empty");
        }

        String encoded = encode(new BigInteger(1, bytes));
        if (encoded.length() >= length) {
            return encoded.substring(0, length);
        }

        StringBuilder builder = new StringBuilder(encoded);
        for (int i = 0; builder.length() < length; i++) {
            int alphabetIndex = Byte.toUnsignedInt(bytes[i % bytes.length]) % ALPHABET.length;
            builder.append(ALPHABET[alphabetIndex]);
        }
        return builder.toString();
    }
}
