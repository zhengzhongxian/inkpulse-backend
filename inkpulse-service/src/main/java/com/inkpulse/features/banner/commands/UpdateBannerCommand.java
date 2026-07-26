package com.inkpulse.features.banner.commands;

import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.models.request.banner.UpdateBannerRequest;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class UpdateBannerCommand implements Command<BannerResponse> {
    UUID bannerId;
    UpdateBannerRequest request;
    UploadFileModel imageFile;
    UploadFileModel iconFile;
}
