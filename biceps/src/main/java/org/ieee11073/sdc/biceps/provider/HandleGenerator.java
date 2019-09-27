package org.ieee11073.sdc.biceps.provider;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility to create handles.
 *
 * \todo Unit test is missing.
 */
public class HandleGenerator {
    private final String prefix;
    private static final AtomicInteger handleCounter = new AtomicInteger(0);

    /**
     * Create instance that generates handles with a fixed prefix.
     */
    public static HandleGenerator create(String prefix) {
        return new HandleGenerator(prefix);
    }

    /**
     * Create one handle with a given prefix.
     *
     * Format: prefix + non-negative integer.
     */
    public static String createOne(String prefix) {
        return concatHandle(prefix, handleCounter.incrementAndGet());
    }

    /**
     * Creates a handle that is globally unique.
     *
     * Global uniqueness is established by using UUIDs.
     */
    public static String createGloballyUnique() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates the next handle with the prefix given during construction.
     *
     * Format: prefix + non-negative integer.
     */
    public String next() {
        return concatHandle(prefix, handleCounter.incrementAndGet());
    }

    private HandleGenerator(String prefix) {
        this.prefix = prefix;
    }

    private static String concatHandle(String prefix, int suffix) {
        return String.format("%s%s", prefix, suffix);
    }
}
