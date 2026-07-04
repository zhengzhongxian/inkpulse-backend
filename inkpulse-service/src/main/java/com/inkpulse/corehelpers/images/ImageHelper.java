package com.inkpulse.corehelpers.images;

import lombok.extern.slf4j.Slf4j;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

@Slf4j
public class ImageHelper {

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg"
    );

    /**
     * Kiểm tra dung lượng và định dạng tệp ảnh.
     */
    public static void validateImage(String contentType, long fileSize, long maxSizeBytes) {
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Định dạng tệp không được hỗ trợ. Chỉ cho phép các tệp ảnh (JPEG, PNG, GIF, WEBP).");
        }
        if (fileSize > maxSizeBytes) {
            throw new IllegalArgumentException("Dung lượng ảnh vượt quá giới hạn tối đa cho phép (" + (maxSizeBytes / (1024 * 1024)) + "MB).");
        }
    }

    /**
     * Resize ảnh về kích thước 400x400 và xuất ra dưới dạng Stream JPEG để tối ưu dung lượng.
     */
    public static UploadFileModel resizeTo400x400(InputStream inputStream, String originalFileName, String contentType) {
        try {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new IllegalArgumentException("Tệp tin ảnh bị lỗi hoặc không thể đọc.");
            }

            // Tạo khung ảnh vuông 400x400
            BufferedImage outputImage = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = outputImage.createGraphics();
            
            // Thiết lập các thuộc tính tối ưu chất lượng vẽ lại ảnh
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Vẽ ảnh gốc co giãn đúng tỷ lệ về 400x400
            g2d.drawImage(originalImage, 0, 0, 400, 400, null);
            g2d.dispose();

            // Ghi luồng ảnh kết quả ra ByteArrayOutputStream dưới dạng jpeg
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "jpg", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // Chuẩn hóa lại tên file lưu trữ dạng .jpg
            String newFileName = originalFileName;
            if (originalFileName != null && originalFileName.contains(".")) {
                newFileName = originalFileName.substring(0, originalFileName.lastIndexOf(".")) + ".jpg";
            } else {
                newFileName = "cover.jpg";
            }

            return UploadFileModel.builder()
                    .inputStream(new ByteArrayInputStream(imageBytes))
                    .fileName(newFileName)
                    .contentType("image/jpeg")
                    .fileSize(imageBytes.length)
                    .build();
        } catch (IOException e) {
            log.error("Lỗi khi resize ảnh đại diện: ", e);
            throw new RuntimeException("Không thể xử lý và resize ảnh đại diện.", e);
        }
    }
}
