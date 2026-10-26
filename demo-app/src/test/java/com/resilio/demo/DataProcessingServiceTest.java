package com.resilio.demo;

import com.resilio.demo.service.DataProcessingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataProcessingServiceTest {

    private DataProcessingService service;

    @BeforeEach
    void setUp() {
        service = new DataProcessingService();
    }

    @Test
    void testValidInputSucceeds() {
        var result = service.processPayload("Resilio_2026");
        Assertions.assertTrue(result.valid());
        Assertions.assertTrue(result.message().contains("Validation Passed"));
        Assertions.assertEquals("PROCESSED::RESILIO_2026", result.processedValue());
    }

    @Test
    void testShortInputRejected() {
        var result = service.processPayload("abc"); // Less than 4 characters
        Assertions.assertFalse(result.valid());
        Assertions.assertTrue(result.message().contains("Validation Error"));
        Assertions.assertNull(result.processedValue());
    }

    @Test
    void testNullOrEmptyInputRejected() {
        var nullResult = service.processPayload(null);
        Assertions.assertFalse(nullResult.valid());
        Assertions.assertTrue(nullResult.message().contains("cannot be empty"));

        var emptyResult = service.processPayload("   ");
        Assertions.assertFalse(emptyResult.valid());
        Assertions.assertTrue(emptyResult.message().contains("cannot be empty"));
    }

    @Test
    void testSpecialCharactersRejected() {
        var result = service.processPayload("invalid@payload!#$");
        Assertions.assertFalse(result.valid());
        Assertions.assertTrue(result.message().contains("Validation Error"));
    }
}
