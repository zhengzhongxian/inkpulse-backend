package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.response.auth.LoginResult;
import com.inkpulse.models.response.auth.MfaStatusResponse;
import com.inkpulse.models.request.auth.LoginRequest;
import com.inkpulse.models.request.auth.SendOtpRequest;
import com.inkpulse.models.request.auth.VerifyMfaRequest;
import com.inkpulse.models.request.auth.InitiatePushRequest;
import com.inkpulse.models.request.auth.ApprovePushRequest;
import com.inkpulse.models.request.auth.RefreshTokenRequest;
import com.inkpulse.models.request.auth.LogoutRequest;
import com.inkpulse.models.request.auth.SendRegisterOtpRequest;
import com.inkpulse.models.request.auth.VerifyRegisterRequest;
import com.inkpulse.features.auth.service.MfaService;
import com.inkpulse.features.auth.commands.LoginCommand;
import com.inkpulse.features.auth.commands.InternalLoginCommand;
import com.inkpulse.models.request.InternalLoginRequest;
import com.inkpulse.features.auth.commands.ForgotPasswordCommand;
import com.inkpulse.features.auth.commands.ResetPasswordCommand;
import com.inkpulse.features.auth.commands.GoogleLoginCommand;
import com.inkpulse.features.auth.commands.GoogleRegisterCommand;
import com.inkpulse.models.request.auth.ForgotPasswordRequest;
import com.inkpulse.models.request.auth.ResetPasswordRequest;
import com.inkpulse.models.request.auth.GoogleLoginRequest;
import com.inkpulse.models.request.auth.GoogleRegisterRequest;
import com.inkpulse.models.response.auth.GoogleLoginResult;
import com.inkpulse.features.auth.commands.LogoutCommand;
import com.inkpulse.features.auth.commands.RefreshTokenCommand;
import com.inkpulse.features.auth.commands.VerifyMfaCommand;
import com.inkpulse.features.auth.commands.SendRegisterOtpCommand;
import com.inkpulse.features.auth.commands.VerifyRegisterCommand;
import com.inkpulse.features.auth.dto.MfaVerificationSessionDto;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.constants.message.MfaMessageConstants;
import com.inkpulse.constants.message.OtpMessageConstants;
import com.inkpulse.constants.message.RegisterMessageConstants;
import com.inkpulse.constants.message.TokenMessageConstants;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final Pipeline pipeline;
    private final MfaService mfaService;

    @PostMapping("/register/send-otp")
    public ResponseEntity<ResultRes<Void>> sendRegisterOtp(
            @Valid @RequestBody SendRegisterOtpRequest request) {

        SendRegisterOtpCommand cmd = new SendRegisterOtpCommand(
                request.getEmail(),
                request.getDeviceId(),
                request.getBrowserFingerprint()
        );
        pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(null, OtpMessageConstants.SENT, 200));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<ResultRes<LoginResult>> verifyRegister(
            @Valid @RequestBody VerifyRegisterRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        VerifyRegisterCommand cmd = new VerifyRegisterCommand(
                request.getEmail(),
                request.getOtpCode(),
                request.getUserName(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getGender(),
                request.getDob(),
                request.getDeviceId(),
                request.getDeviceName(),
                request.getDeviceType(),
                request.getBrowserFingerprint(),
                clientIp,
                request.getChoiceLanguage(),
                request.getRecipientPhone(),
                request.getProvinceId(),
                request.getDistrictId(),
                request.getWardCode(),
                request.getStreetAddress(),
                request.getAddressLabel()
        );

        LoginResult result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, RegisterMessageConstants.SUCCESS, 200));
    }

    @PostMapping("/login")
    public ResponseEntity<ResultRes<LoginResult>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        LoginCommand cmd = new LoginCommand(
                request.getLogin(),
                request.getPassword(),
                request.getDeviceId(),
                request.getBrowserFingerprint(),
                request.getDeviceName(),
                request.getDeviceType(),
                clientIp
        );

        LoginResult result = pipeline.send(cmd);

        if (result.isMfaRequired()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ResultRes.successResult(result, AuthMessageConstants.LOGIN_MFA_REQUIRED, 202));
        }

        return ResponseEntity.ok(ResultRes.successResult(result, AuthMessageConstants.LOGIN_SUCCESS, 200));
    }

    @PostMapping("/internal/login")
    public ResponseEntity<ResultRes<LoginResult>> internalLogin(
            @Valid @RequestBody InternalLoginRequest request) {
        log.info("REST request to internal login: login={}", request.getLogin());

        InternalLoginCommand cmd = new InternalLoginCommand(request.getLogin(), request.getPassword());
        LoginResult result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthMessageConstants.INTERNAL_LOGIN_SUCCESS, 200));
    }

    @PostMapping("/mfa/send-otp")
    public ResponseEntity<ResultRes<Integer>> sendMfaOtp(
            @RequestBody SendOtpRequest request) {

        // Returns the challengeNumber to display on login screen.
        // Email sends 3 shuffled options — user picks the matching one.
        int challengeNumber = mfaService.sendEmailNumberChallenge(
                request.getMfaSessionId(), request.getEmail());

        return ResponseEntity.ok(ResultRes.successResult(challengeNumber, OtpMessageConstants.SENT, 200));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<ResultRes<LoginResult>> verifyMfa(
            @RequestBody VerifyMfaRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        VerifyMfaCommand cmd = new VerifyMfaCommand(
                request.getMfaSessionId(),
                request.getCode(),
                request.getDeviceId(),
                request.getBrowserFingerprint(),
                clientIp
        );

        LoginResult result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, MfaMessageConstants.VERIFIED, 200));
    }

    @PostMapping("/mfa/init-push")
    public ResponseEntity<ResultRes<String>> initPushMfa(
            @RequestBody InitiatePushRequest request) {
        String challenge = mfaService.initiatePushMfa(request.getMfaSessionId());
        return ResponseEntity.ok(ResultRes.successResult(challenge, "Push challenge initiated", 200));
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<ResultRes<MfaStatusResponse>> getMfaStatus(@RequestParam String sessionId) {
        MfaVerificationSessionDto session = mfaService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.ok(ResultRes.successResult(
                    new MfaStatusResponse("EXPIRED", false),
                    "Session expired or not found", 200));
        }
        boolean approved = "APPROVED".equals(session.mfaType());
        return ResponseEntity.ok(ResultRes.successResult(
                new MfaStatusResponse(session.mfaType(), approved),
                "MFA session status retrieved", 200));
    }

    @PostMapping("/mfa/approve")
    public ResponseEntity<ResultRes<Object>> approveMfa(
            @RequestBody ApprovePushRequest request) {
        mfaService.approveMfaSession(request.getMfaSessionId());
        return ResponseEntity.ok(ResultRes.successResult("MFA session approved successfully", 200));
    }

    @GetMapping("/mfa/verify-click")
    @ResponseBody
    public String verifyMfaClick(
            @RequestParam String sessionId,
            @RequestParam String code) {
        try {
            boolean verified = mfaService.verifyOtp(sessionId, code);
            if (verified) {
                return "<html>" +
                        "<head>" +
                        "  <meta charset='utf-8'>" +
                        "  <link href='https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,700;1,700&family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                        "  <style>" +
                        "    body { font-family: 'Be Vietnam Pro', sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #F8F8F8; margin: 0; }" +
                        "    .card { background: white; padding: 40px; border-radius: 0; border: 1px solid #EBEBEB; text-align: center; max-width: 440px; box-shadow: 0 4px 16px rgba(0,0,0,0.03); }" +
                        "    .icon { font-size: 48px; color: #F66398; margin-bottom: 20px; }" +
                        "    h2 { font-family: 'Playfair Display', Georgia, serif; font-style: italic; color: #ec4899; margin: 0 0 16px 0; font-size: 26px; }" +
                        "    p { color: #5A5A5A; font-size: 15px; line-height: 1.6; margin: 0; }" +
                        "  </style>" +
                        "</head>" +
                        "<body>" +
                        "  <div class='card'>" +
                        "    <div class='icon'>✓</div>" +
                        "    <h2>Xác thực thành công!</h2>" +
                        "    <p>Bạn đã chọn đúng số khớp với yêu cầu. Vui lòng quay lại tab đăng nhập để tiếp tục.</p>" +
                        "  </div>" +
                        "</body>" +
                        "</html>";
            }
        } catch (Exception e) {
            log.error("Click verification failed", e);
        }
        return "<html>" +
                "<head>" +
                "  <meta charset='utf-8'>" +
                "  <link href='https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,700;1,700&family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                "  <style>" +
                "    body { font-family: 'Be Vietnam Pro', sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #F8F8F8; margin: 0; }" +
                "    .card { background: white; padding: 40px; border-radius: 0; border: 1px solid #EBEBEB; text-align: center; max-width: 440px; box-shadow: 0 4px 16px rgba(0,0,0,0.03); }" +
                "    .icon { font-size: 48px; color: #dc3545; margin-bottom: 20px; }" +
                "    h2 { font-family: 'Playfair Display', Georgia, serif; font-style: italic; color: #dc3545; margin: 0 0 16px 0; font-size: 26px; }" +
                "    p { color: #5A5A5A; font-size: 15px; line-height: 1.6; margin: 0; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='card'>" +
                "    <div class='icon'>✗</div>" +
                "    <h2>Xác thực thất bại</h2>" +
                "    <p>Con số bạn chọn không khớp với yêu cầu, hoặc phiên xác thực đã hết hạn hoặc bị chặn. Vui lòng quay lại tab đăng nhập để gửi lại yêu cầu.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResultRes<LoginResult>> refresh(
            @RequestBody RefreshTokenRequest request) {

        RefreshTokenCommand cmd = new RefreshTokenCommand(request.getRefreshToken());
        LoginResult result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, TokenMessageConstants.REFRESHED, 200));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResultRes<Object>> logout(
            @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = "";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        // reason_code: client may specify (e.g. CHANGE_PASSWORD). Defaults to DIRECTLY_LOGOUT.
        String reasonCode = (request.getReasonCode() != null && !request.getReasonCode().isBlank())
                ? request.getReasonCode()
                : "DIRECTLY_LOGOUT";

        LogoutCommand cmd = new LogoutCommand(request.getRefreshToken(), accessToken, reasonCode);
        pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(TokenMessageConstants.REVOKED, 200));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResultRes<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordCommand cmd = new ForgotPasswordCommand(
                request.getEmail(),
                request.getDeviceId(),
                request.getBrowserFingerprint()
        );
        pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(null, AuthMessageConstants.FORGOT_PASSWORD_EMAIL_SENT, 200));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResultRes<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordCommand cmd = new ResetPasswordCommand(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(null, AuthMessageConstants.RESET_PASSWORD_SUCCESS, 200));
    }

    @PostMapping("/google")
    public ResponseEntity<ResultRes<GoogleLoginResult>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        GoogleLoginCommand cmd = GoogleLoginCommand.builder()
                .idToken(request.getIdToken())
                .deviceId(request.getDeviceId())
                .browserFingerprint(request.getBrowserFingerprint())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .clientIp(clientIp)
                .build();
        GoogleLoginResult result = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(result));
    }

    @PostMapping("/google/register")
    public ResponseEntity<ResultRes<LoginResult>> googleRegister(
            @Valid @RequestBody GoogleRegisterRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        GoogleRegisterCommand cmd = GoogleRegisterCommand.builder()
                .googleUserId(request.getGoogleUserId())
                .email(request.getEmail())
                .name(request.getName())
                .picture(request.getPicture())
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(request.getGender())
                .dob(request.getDob())
                .choiceLanguage(request.getChoiceLanguage())
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .browserFingerprint(request.getBrowserFingerprint())
                .clientIp(clientIp)
                .recipientPhone(request.getRecipientPhone())
                .provinceId(request.getProvinceId())
                .districtId(request.getDistrictId())
                .wardCode(request.getWardCode())
                .streetAddress(request.getStreetAddress())
                .addressLabel(request.getAddressLabel())
                .build();
        LoginResult result = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(result, RegisterMessageConstants.SUCCESS, 200));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
