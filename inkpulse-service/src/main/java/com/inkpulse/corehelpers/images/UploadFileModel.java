package com.inkpulse.corehelpers.images;

import lombok.*;
import java.io.InputStream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadFileModel {
    private InputStream inputStream;
    private String fileName;
    private String contentType;
    private long fileSize;
}
