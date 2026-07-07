package com.inkpulse.service.minio.impl;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.exceptions.MinioBusinessException;
import com.inkpulse.corehelpers.exceptions.MinioTechnicalException;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.service.minio.MinioFileInfo;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService implements IMinioService {

    @Value("${" + KeyConstants.MINIO_ENDPOINT + "}")
    private String endpoint;

    @Value("${" + KeyConstants.MINIO_ACCESS_KEY + "}")
    private String accessKey;

    @Value("${" + KeyConstants.MINIO_SECRET_KEY + "}")
    private String secretKey;

    @Value("${" + KeyConstants.MINIO_BUCKET_NAME + "}")
    private String defaultBucket;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + ":}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Value("${" + KeyConstants.MINIO_REGION + ":}")
    private String region;

    @Value("${" + KeyConstants.MINIO_PRESIGNED_EXPIRY_MINUTES + ":60}")
    private int presignedUrlExpiryMinutes;

    private MinioClient minioClient;

    private synchronized MinioClient getMinioClient() {
        if (minioClient == null) {
            var builder = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey);
            if (region != null && !region.isBlank()) {
                builder.region(region);
            }
            minioClient = builder.build();
            log.info("MinIO Service initialized - Endpoint: {}, Public URL: {}, Default Bucket: {}",
                    endpoint, publicUrl.isBlank() ? endpoint : publicUrl, defaultBucket);
        }
        return minioClient;
    }

    @Override
    public MinioFileInfo uploadFile(
            InputStream inputStream,
            String fileName,
            String contentType,
            long size,
            String objectName,
            Map<String, String> metadata) {
        try {
            return uploadFile(inputStream, fileName, contentType, size, objectName, defaultBucket, metadata);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new MinioTechnicalException("UPLOAD_ERROR", "Lỗi khi upload file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public MinioFileInfo uploadFile(
            InputStream inputStream,
            String fileName,
            String contentType,
            long size,
            String objectName,
            String bucketName,
            Map<String, String> metadata) {
        try {
            if (inputStream == null || size == 0) {
                throw new MinioBusinessException("FILE_EMPTY", "File không được rỗng");
            }
            if (fileName == null || fileName.isBlank()) {
                throw new MinioBusinessException("FILENAME_EMPTY", "Tên file không được rỗng");
            }

            MinioClient client = getMinioClient();
            String bucket = bucketName != null && !bucketName.isBlank() ? bucketName : defaultBucket;
            String objName = objectName != null && !objectName.isBlank()
                    ? objectName
                    : generateObjectName(fileName);

            // Ensure bucket exists
            ensureBucketExists(bucket);

            // Prepare metadata
            Map<String, String> userMetadata = new HashMap<>();
            if (metadata != null) {
                userMetadata.putAll(metadata);
            }
            userMetadata.put("uploaded-at", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            userMetadata.put("original-filename", fileName);

            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .userMetadata(userMetadata)
                            .build());

            log.info("Successfully uploaded file to MinIO: Bucket={}, Object={}, Size={}",
                    bucket, objName, size);

            return getFileMetadata(objName, bucket);

        } catch (MinioBusinessException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error while uploading file: {}", fileName, ex);
            throw new MinioTechnicalException("UPLOAD_ERROR", "Lỗi khi upload file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public InputStream downloadFile(String objectName) {
        try {
            if (objectName == null || objectName.isBlank()) {
                throw new MinioBusinessException("OBJECT_NAME_EMPTY", "Tên object không được rỗng");
            }

            MinioClient client = getMinioClient();
            InputStream stream = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build());

            // Read to memory stream so connection stream is not left hanging
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (stream) {
                stream.transferTo(outputStream);
            }

            log.info("Successfully downloaded file from MinIO: Bucket={}, Object={}", defaultBucket, objectName);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (MinioBusinessException e) {
            throw e;
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                throw new MinioBusinessException("FILE_NOT_FOUND", "File không tồn tại: " + objectName, ex);
            }
            throw new MinioTechnicalException("DOWNLOAD_ERROR", "Lỗi MinIO khi download file: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error while downloading file: {}", objectName, ex);
            throw new MinioTechnicalException("DOWNLOAD_ERROR",
                    "Lỗi không xác định khi download file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getPresignedDownloadUrl(String objectName, int expiryMinutes) {
        try {
            if (objectName == null || objectName.isBlank()) {
                throw new MinioBusinessException("OBJECT_NAME_EMPTY", "Tên object không được rỗng");
            }

            MinioClient client = getMinioClient();
            int expiry = expiryMinutes > 0 ? expiryMinutes : presignedUrlExpiryMinutes;

            String url = client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(defaultBucket)
                            .object(objectName)
                            .expiry(expiry, TimeUnit.MINUTES)
                            .build());

            log.info("Generated presigned URL for object: {}, Expiry: {}min", objectName, expiry);
            return url;

        } catch (MinioBusinessException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error while generating presigned URL for object: {}", objectName, ex);
            throw new MinioTechnicalException("PRESIGNED_URL_ERROR", "Lỗi khi tạo URL download: " + ex.getMessage(),
                    ex);
        }
    }

    @Override
    public MinioFileInfo getFileMetadata(String objectName) {
        return getFileMetadata(objectName, defaultBucket);
    }

    @Override
    public MinioFileInfo getFileMetadata(String objectName, String bucketName) {
        try {
            if (objectName == null || objectName.isBlank()) {
                throw new MinioBusinessException("OBJECT_NAME_EMPTY", "Tên object không được rỗng");
            }

            MinioClient client = getMinioClient();
            String bucket = bucketName != null && !bucketName.isBlank() ? bucketName : defaultBucket;
            StatObjectResponse stat = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());

            // Extract metadata mapping
            Map<String, String> metadata = new HashMap<>();
            stat.headers().names().forEach(name -> {
                if (name.startsWith("x-amz-meta-")) {
                    String cleanKey = name.substring(11);
                    metadata.put(cleanKey, stat.headers().get(name));
                }
            });

            ZonedDateTime lastModified = stat.lastModified().withZoneSameInstant(ZoneId.systemDefault());

            return MinioFileInfo.builder()
                    .objectName(objectName)
                    .bucketName(bucket)
                    .url(buildFileUrl(bucket, objectName))
                    .size(stat.size())
                    .contentType(stat.contentType() != null ? stat.contentType() : "application/octet-stream")
                    .etag(stat.etag())
                    .lastModified(lastModified)
                    .metadata(metadata)
                    .build();

        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                throw new MinioBusinessException("FILE_NOT_FOUND", "File không tồn tại: " + objectName, ex);
            }
            throw new MinioTechnicalException("METADATA_ERROR", "Lỗi MinIO khi lấy metadata: " + ex.getMessage(), ex);
        } catch (MinioBusinessException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Unexpected error while getting file metadata: {}", objectName, ex);
            throw new MinioTechnicalException("METADATA_ERROR",
                    "Lỗi không xác định khi lấy metadata: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteFile(String objectName) {
        try {
            if (objectName == null || objectName.isBlank()) {
                throw new MinioBusinessException("OBJECT_NAME_EMPTY", "Tên object không được rỗng");
            }

            MinioClient client = getMinioClient();
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build());

            log.info("Successfully deleted file from MinIO: Bucket={}, Object={}", defaultBucket, objectName);
            return true;

        } catch (MinioBusinessException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error while deleting file: {}", objectName, ex);
            throw new MinioTechnicalException("DELETE_ERROR", "Lỗi khi xóa file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteMultipleFiles(List<String> objectNames) {
        try {
            if (objectNames == null || objectNames.isEmpty()) {
                throw new MinioBusinessException("OBJECT_NAMES_EMPTY", "Danh sách tên object không được rỗng");
            }

            MinioClient client = getMinioClient();
            List<DeleteObject> deleteObjects = objectNames.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            Iterable<io.minio.Result<DeleteError>> results = client.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .objects(deleteObjects)
                            .build());

            List<String> errors = new ArrayList<>();
            for (io.minio.Result<DeleteError> result : results) {
                DeleteError error = result.get();
                errors.add(error.objectName() + ": " + error.message());
                log.warn("Failed to delete object: {} - {}", error.objectName(), error.message());
            }

            if (!errors.isEmpty()) {
                throw new MinioTechnicalException("BATCH_DELETE_ERROR",
                        "Một số file không thể xóa: " + String.join(", ", errors));
            }

            log.info("Successfully deleted {} files from MinIO", objectNames.size());
            return true;

        } catch (MinioBusinessException | MinioTechnicalException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error while deleting multiple files", ex);
            throw new MinioTechnicalException("BATCH_DELETE_ERROR", "Lỗi khi xóa nhiều file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean fileExists(String objectName) {
        try {
            if (objectName == null || objectName.isBlank()) {
                return false;
            }
            getFileMetadata(objectName);
            return true;
        } catch (MinioBusinessException ex) {
            if ("FILE_NOT_FOUND".equals(ex.getErrorCode())) {
                return false;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public List<MinioFileInfo> listFiles(String prefix) {
        try {
            MinioClient client = getMinioClient();
            List<MinioFileInfo> fileInfos = new ArrayList<>();

            Iterable<io.minio.Result<Item>> results = client.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(defaultBucket)
                            .prefix(prefix == null ? "" : prefix)
                            .recursive(true)
                            .build());

            for (io.minio.Result<Item> result : results) {
                Item item = result.get();
                ZonedDateTime lastModified = item.lastModified().withZoneSameInstant(ZoneId.systemDefault());

                fileInfos.add(
                        MinioFileInfo.builder()
                                .objectName(item.objectName())
                                .bucketName(defaultBucket)
                                .url(buildFileUrl(defaultBucket, item.objectName()))
                                .size(item.size())
                                .contentType("application/octet-stream")
                                .etag(item.etag())
                                .lastModified(lastModified)
                                .metadata(new HashMap<>())
                                .build());
            }

            log.info("Listed {} files from MinIO bucket {} with prefix {}", fileInfos.size(), defaultBucket, prefix);
            return fileInfos;

        } catch (Exception ex) {
            log.error("Error while listing files", ex);
            throw new MinioTechnicalException("LIST_ERROR", "Lỗi khi liệt kê file: " + ex.getMessage(), ex);
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        MinioClient client = getMinioClient();
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());

        if (!exists) {
            client.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    private String generateObjectName(String fileName) {
        int extIndex = fileName.lastIndexOf('.');
        String extension = extIndex != -1 ? fileName.substring(extIndex) : "";
        String nameWithoutExt = extIndex != -1 ? fileName.substring(0, extIndex) : fileName;
        String sanitizedName = sanitizeFileName(nameWithoutExt);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());
        String guid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        return timestamp + "_" + guid + "_" + sanitizedName + extension;
    }

    private String sanitizeFileName(String fileName) {
        // Replace invalid file name chars with _
        return fileName.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
    }

    private String buildFileUrl(String bucket, String objectName) {
        String baseUrl = !publicUrl.isBlank() ? publicUrl : endpoint;
        String scheme = useSsl ? "https" : "http";

        String cleanBaseUrl = baseUrl.replaceAll("^https?://", "").replaceAll("/+$", "");
        return scheme + "://" + cleanBaseUrl + "/" + bucket + "/" + objectName;
    }
}
