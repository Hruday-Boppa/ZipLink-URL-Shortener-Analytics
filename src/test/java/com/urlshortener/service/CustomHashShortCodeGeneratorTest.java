package com.urlshortener.service;

import java.util.HashSet;
import java.util.Set;

import com.urlshortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomHashShortCodeGeneratorTest {

    @Test
    void shouldGenerateConfiguredLengthBase62Code() {
        AppProperties appProperties = new AppProperties();
        appProperties.getShortCode().setLength(10);
        appProperties.getShortCode().setSecret("test-secret");

        CustomHashShortCodeGenerator generator = new CustomHashShortCodeGenerator(new HashingService(), appProperties);
        String code = generator.generateCode();

        assertThat(code).hasSize(10).matches("^[0-9a-zA-Z]+$");
    }

    @Test
    void shouldGenerateMostlyUniqueCodes() {
        AppProperties appProperties = new AppProperties();
        appProperties.getShortCode().setLength(8);
        appProperties.getShortCode().setSecret("test-secret");

        CustomHashShortCodeGenerator generator = new CustomHashShortCodeGenerator(new HashingService(), appProperties);

        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            generated.add(generator.generateCode());
        }

        assertThat(generated.size()).isGreaterThan(190);
    }
}
