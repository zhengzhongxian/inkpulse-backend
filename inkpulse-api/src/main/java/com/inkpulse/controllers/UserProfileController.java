package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.features.user.queries.GetUserProfileByUserIdQuery;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.features.user.commands.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final Pipeline pipeline;

    @GetMapping("/profile")
    public ResponseEntity<ResultRes<UserProfileCacheDto>> getMyProfile(
            @AuthenticationPrincipal String userIdStr) {
        log.info("Request to get profile for authenticated user: {}", userIdStr);
        
        UUID userId = UUID.fromString(userIdStr);
        UserProfileCacheDto profile = pipeline.send(new GetUserProfileByUserIdQuery(userId));

        return ResponseEntity.ok(ResultRes.successResult(profile, "Lấy thông tin cá nhân thành công!", 200));
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultRes<UserProfileCacheDto>> updateMyProfile(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "biography", required = false) String biography,
            @RequestParam(value = "displayMode", required = false) String displayMode,
            @RequestParam(value = "choiceLanguage", required = false) String choiceLanguage,
            @RequestParam(value = "mfaTypes", required = false) List<String> mfaTypes,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {

        log.info("Request to update profile for user: {}", userIdStr);

        UploadFileModel avatarModel = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarModel = new UploadFileModel(
                        avatarFile.getInputStream(),
                        avatarFile.getOriginalFilename(),
                        avatarFile.getContentType(),
                        avatarFile.getSize()
                );
            } catch (IOException e) {
                throw new BusinessValidationException(UserMessageConstants.AVATAR_READ_ERROR, "AVATAR_READ_ERROR");
            }
        }

        LocalDate dobDate = null;
        if (dob != null && !dob.trim().isEmpty() && !dob.equalsIgnoreCase("null")) {
            try {
                dobDate = LocalDate.parse(dob.trim());
            } catch (Exception ignored) {}
        }

        UpdateUserProfileCommand cmd = UpdateUserProfileCommand.builder()
                .userId(UUID.fromString(userIdStr))
                .firstName(firstName)
                .lastName(lastName)
                .gender(gender)
                .dob(dobDate)
                .biography(biography)
                .displayMode(displayMode)
                .choiceLanguage(choiceLanguage)
                .mfaTypes(mfaTypes != null ? mfaTypes : List.of())
                .avatarFile(avatarModel)
                .build();

        UserProfileCacheDto updatedProfile = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(updatedProfile, UserMessageConstants.PROFILE_UPDATE_SUCCESS, 200));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ResultRes<UserProfileCacheDto>> createAddress(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody CreateUserAddressCommand cmd) {
        log.info("Request to create address for user: {}", userIdStr);
        cmd.setUserId(UUID.fromString(userIdStr));
        UserProfileCacheDto updatedProfile = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(updatedProfile, UserMessageConstants.CREATE_ADDRESS_SUCCESS, 200));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ResultRes<UserProfileCacheDto>> updateAddress(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable("id") UUID addressId,
            @RequestBody UpdateUserAddressCommand cmd) {
        log.info("Request to update address {} for user: {}", addressId, userIdStr);
        cmd.setUserId(UUID.fromString(userIdStr));
        cmd.setAddressId(addressId);
        UserProfileCacheDto updatedProfile = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(updatedProfile, UserMessageConstants.UPDATE_ADDRESS_SUCCESS, 200));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ResultRes<UserProfileCacheDto>> deleteAddress(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable("id") UUID addressId) {
        log.info("Request to delete address {} for user: {}", addressId, userIdStr);
        DeleteUserAddressCommand cmd = DeleteUserAddressCommand.builder()
                .userId(UUID.fromString(userIdStr))
                .addressId(addressId)
                .build();
        UserProfileCacheDto updatedProfile = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(updatedProfile, UserMessageConstants.DELETE_ADDRESS_SUCCESS, 200));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ResultRes<Void>> changePassword(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody ChangePasswordCommand cmd) {
        log.info("Request to change password for user: {}", userIdStr);
        cmd.setUserId(UUID.fromString(userIdStr));
        pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(null, UserMessageConstants.CHANGE_PASSWORD_SUCCESS, 200));
    }
}
