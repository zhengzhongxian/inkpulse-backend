package com.inkpulse.corehelpers;

public class UrlHelper {

    public static String buildAbsoluteUrl(String publicUrlProp, String relativePath, boolean useSsl) {
        if (relativePath == null || publicUrlProp == null) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        String scheme = useSsl ? "https" : "http";
        String cleanBaseUrl = publicUrlProp.replaceAll("^https?://", "").replaceAll("/+$", "");
        return scheme + "://" + cleanBaseUrl + "/" + relativePath;
    }
}
