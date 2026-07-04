package com.inkpulse.service.minio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinioFileInfo {
    private String objectName;
    private String bucketName;
    private String url;
    private long size;
    private String contentType;
    private String etag;
    private ZonedDateTime lastModified;
    private Map<String, String> metadata;
}
