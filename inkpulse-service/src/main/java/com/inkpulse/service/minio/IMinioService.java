package com.inkpulse.service.minio;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IMinioService {
    
    MinioFileInfo uploadFile(
            InputStream inputStream,
            String fileName,
            String contentType,
            long size,
            String objectName,
            Map<String, String> metadata
    ) throws Exception;

    MinioFileInfo uploadFile(
            InputStream inputStream,
            String fileName,
            String contentType,
            long size,
            String objectName,
            String bucketName,
            Map<String, String> metadata
    ) throws Exception;

    InputStream downloadFile(String objectName) throws Exception;

    String getPresignedDownloadUrl(String objectName, int expiryMinutes) throws Exception;

    MinioFileInfo getFileMetadata(String objectName) throws Exception;

    MinioFileInfo getFileMetadata(String objectName, String bucketName) throws Exception;

    boolean deleteFile(String objectName) throws Exception;

    boolean deleteMultipleFiles(List<String> objectNames) throws Exception;

    boolean fileExists(String objectName) throws Exception;

    List<MinioFileInfo> listFiles(String prefix) throws Exception;
}
