package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardNumberUtilTest {

    @Test
    void generateCardNumber_shouldHave16Digits() {
        String number = CardNumberUtil.generateCardNumber();

        assertNotNull(number);
        assertEquals(16, number.length());
    }

    @Test
    void generateCardNumber_shouldContainOnlyDigits() {
        String number = CardNumberUtil.generateCardNumber();

        assertTrue(number.matches("\\d{16}"));
    }

    @Test
    void generateCardNumber_shouldBeDifferent() {
        String first = CardNumberUtil.generateCardNumber();
        String second = CardNumberUtil.generateCardNumber();

        assertNotEquals(first, second);
    }

    @Test
    void maskCardNumber_shouldHideFirst12Digits() {
        String masked = CardNumberUtil.maskCardNumber("0123456789012345");

        assertEquals(masked, "**** **** **** 2345");
    }
}
