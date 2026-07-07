package com.inkpulse.corehelpers;

import java.util.Random;

public final class OrderCodeHelper {

    private static final Random RANDOM = new Random();

    private OrderCodeHelper() {
        // Prevent instantiation
    }

    /**
     * Generates a unique numeric order code based on current timestamp and a random offset.
     * Guaranteed to be compatible with PayOS numeric constraint.
     */
    public static String generateOrderCode() {
        return String.valueOf(System.currentTimeMillis() * 100 + RANDOM.nextInt(100));
    }
}
