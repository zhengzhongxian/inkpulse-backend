package com.inkpulse.features.banner.commands;

import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.models.request.banner.CreateBannerRequest;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateBannerCommand implements Command<BannerResponse> {
    CreateBannerRequest request;
    UploadFileModel imageFile;
    UploadFileModel iconFile;
}
