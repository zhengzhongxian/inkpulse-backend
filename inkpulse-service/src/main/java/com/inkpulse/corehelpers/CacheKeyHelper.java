package com.inkpulse.corehelpers;

public final class CacheKeyHelper {

    private CacheKeyHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String resolve(String prefix, String identifier) {
        return (prefix + ":" + identifier).toLowerCase();
    }
}
