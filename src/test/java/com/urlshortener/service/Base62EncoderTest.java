package com.urlshortener.service;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Base62EncoderTest {

    @Test
    void shouldEncodeBigInteger() {
        String encoded = Base62Encoder.encode(BigInteger.valueOf(125));
        assertThat(encoded).isEqualTo("21");
    }

    @Test
    void shouldGenerateFixedLengthCodeFromBytes() {
        String code = Base62Encoder.toFixedLength(new byte[]{1, 2, 3, 4, 5}, 8);
        assertThat(code).hasSize(8).matches("^[0-9a-zA-Z]+$");
    }
}
