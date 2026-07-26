package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.BannerMessageConstants;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.features.banner.commands.CreateBannerCommand;
import com.inkpulse.features.banner.commands.DeleteBannerCommand;
import com.inkpulse.features.banner.commands.ToggleBannerStatusCommand;
import com.inkpulse.features.banner.commands.UpdateBannerCommand;
import com.inkpulse.features.banner.queries.GetBannerDetailQuery;
import com.inkpulse.features.banner.queries.GetPagedBannersQuery;
import com.inkpulse.features.banner.queries.GetPublicBannersQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.request.banner.CreateBannerRequest;
import com.inkpulse.models.request.banner.UpdateBannerRequest;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BannerController {

    private final Pipeline pipeline;
    private final ObjectMapper objectMapper;

    @GetMapping("/api/v1/banners/public")
    public ResponseEntity<ResultRes<List<BannerResponse>>> getPublicBanners() {
        List<BannerResponse> responses = pipeline.send(new GetPublicBannersQuery());
        return ResponseEntity.ok(ResultRes.successResult(responses));
    }

    @GetMapping("/api/v1/internal/banners")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Banners.VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<BannerResponse>>> getPagedBanners(
            @ModelAttribute GetPagedBannersQuery query
    ) {
        PagedList<BannerResponse> pagedList = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(pagedList));
    }

    @GetMapping("/api/v1/internal/banners/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Banners.VIEW + "')")
    public ResponseEntity<ResultRes<BannerResponse>> getBannerDetail(@PathVariable("id") UUID id) {
        BannerResponse response = pipeline.send(new GetBannerDetailQuery(id));
        return ResponseEntity.ok(ResultRes.successResult(response));
    }

    @PostMapping(value = "/api/v1/internal/banners", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @PreAuthorize("hasAuthority('" + PermissionConstants.Banners.CREATE + "')")
    public ResponseEntity<ResultRes<BannerResponse>> createBanner(
            @RequestPart(value = "request", required = false) String requestJson,
            @RequestBody(required = false) CreateBannerRequest bodyRequest,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile
    ) throws Exception {
        CreateBannerRequest request = bodyRequest;
        if (request == null && requestJson != null) {
            request = objectMapper.readValue(requestJson, CreateBannerRequest.class);
        }

        UploadFileModel imageModel = toUploadFileModel(imageFile);
        UploadFileModel iconModel = toUploadFileModel(iconFile);

        CreateBannerCommand command = CreateBannerCommand.builder()
                .request(request)
                .imageFile(imageModel)
                .iconFile(iconModel)
                .build();

        BannerResponse response = pipeline.send(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultRes.successResult(response, BannerMessageConstants.BANNER_CREATED_SUCCESS, 201));
    }

    @PutMapping(value = "/api/v1/internal/banners/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @PreAuthorize("hasAuthority('" + PermissionConstants.Banners.EDIT + "')")
    public ResponseEntity<ResultRes<BannerResponse>> updateBanner(
            @PathVariable("id") UUID id,
            @RequestPart(value = "request", required = false) String requestJson,
            @RequestBody(required = false) UpdateBannerRequest bodyRequest,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile
    ) throws Exception {
        UpdateBannerRequest request = bodyRequest;
        if (request == null && requestJson != null) {
            request = objectMapper.readValue(requestJson, UpdateBannerRequest.class);
        }

        UploadFileModel imageModel = toUploadFileModel(imageFile);
        UploadFileModel iconModel = toUploadFileModel(iconFile);

        UpdateBannerCommand command = UpdateBannerCommand.builder()
                .bannerId(id)
                .request(request)
                .imageFile(imageModel)
                .iconFile(iconModel)
                .build();

        BannerResponse response = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(response, BannerMessageConstants.BANNER_UPDATED_SUCCESS, 200));
    }

    @DeleteMapping("/api/v1/internal/banners/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Banners.DELETE + "')")
    public ResponseEntity<ResultRes<Object>> deleteBanner(@PathVariable("id") UUID id) {
        pipeline.send(new DeleteBannerCommand(id));
        return ResponseEntity.ok(ResultRes.successResult(BannerMessageConstants.BANNER_DELETED_SUCCESS, 200));
    }

    @PatchMapping("/api/v1/internal/banners/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Banners.EDIT + "')")
    public ResponseEntity<ResultRes<BannerResponse>> toggleBannerStatus(@PathVariable("id") UUID id) {
        BannerResponse response = pipeline.send(new ToggleBannerStatusCommand(id));
        return ResponseEntity.ok(ResultRes.successResult(response, BannerMessageConstants.BANNER_STATUS_UPDATED, 200));
    }

    private UploadFileModel toUploadFileModel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) return null;
        return UploadFileModel.builder()
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .inputStream(file.getInputStream())
                .build();
    }
}
