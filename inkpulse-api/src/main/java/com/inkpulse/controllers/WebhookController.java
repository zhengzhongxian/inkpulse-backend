package com.inkpulse.controllers;

import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.message.PayOsWebhookMessage;
import com.inkpulse.models.message.GhnStatusUpdateMessage;
import com.inkpulse.service.payos.IPayOsService;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/v1/public/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final IPayOsService payOsService;
    private final OutboxPublisher outboxPublisher;

    @PostMapping("/payos")
    public ResponseEntity<ResultRes<Void>> handlePayOsWebhook(@RequestBody Webhook webhookBody) {
        log.info("Received PayOS webhook request");

        try {
            // 1. Verify Signature
            WebhookData verifiedData = payOsService.verifyWebhook(webhookBody);
            if (verifiedData == null) {
                log.warn("PayOS webhook verification returned null");
                return ResponseEntity.badRequest().body(ResultRes.errorResult(OrderMessageConstants.WEBHOOK_INVALID, 400, java.util.Collections.emptyList()));
            }

            log.info("Verified PayOS webhook data. OrderCode: {}, Description: {}, Code: {}", 
                    verifiedData.getOrderCode(), verifiedData.getDescription(), verifiedData.getCode());

            // 2. Ignore PayOS registration test webhook
            if (verifiedData.getDescription() != null && 
                (verifiedData.getDescription().contains("ma giao dich thu") || 
                 verifiedData.getDescription().contains("trang thai thanh toan thu"))) {
                log.info("Ignoring PayOS mock webhook transaction registration");
                return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.WEBHOOK_TEST, 200));
            }

            // 3. Map to Outbox message
            PayOsWebhookMessage msg = PayOsWebhookMessage.builder()
                    .orderCode(String.valueOf(verifiedData.getOrderCode()))
                    .paymentLinkId(verifiedData.getPaymentLinkId())
                    .amount(verifiedData.getAmount() != null ? verifiedData.getAmount().intValue() : 0)
                    .description(verifiedData.getDescription())
                    .code(verifiedData.getCode())
                    .success("00".equals(verifiedData.getCode()))
                    .build();

            // 4. Publish to Outbox
            outboxPublisher.publish(
                    QueueConstants.PAYOS_WEBHOOK,
                    msg,
                    "urn:message:InkPulse.Worker.Features.Order.Messages:PayOsWebhookMessage");

            log.info("Successfully wrote PayOS Webhook event to Outbox for Order: {}", msg.getOrderCode());

            return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.WEBHOOK_PROCESSED, 200));
        } catch (Exception e) {
            log.error("Exception handling PayOS webhook", e);
            return ResponseEntity.status(500).body(ResultRes.errorResult("Internal error processing webhook: " + e.getMessage(), 500, java.util.Collections.emptyList()));
        }
    }

    @PostMapping("/mock")
    public ResponseEntity<ResultRes<Void>> mockPayOsWebhook(@RequestParam("orderCode") String orderCode) {
        log.info("Received request to mock successful PayOS webhook for order: {}", orderCode);
        try {
            PayOsWebhookMessage msg = PayOsWebhookMessage.builder()
                    .orderCode(orderCode)
                    .paymentLinkId("mock-link-id")
                    .amount(0)
                    .description("Mock transaction successful")
                    .code("00")
                    .success(true)
                    .build();

            outboxPublisher.publish(
                    QueueConstants.PAYOS_WEBHOOK,
                    msg,
                    "urn:message:InkPulse.Worker.Features.Order.Messages:PayOsWebhookMessage");

            log.info("Mock PayOS webhook event written to Outbox for Order: {}", orderCode);
            return ResponseEntity.ok(ResultRes.successResult(null, "Mock Webhook triggered!", 200));
        } catch (Exception e) {
            log.error("Exception handling mock PayOS webhook", e);
            return ResponseEntity.status(500).body(ResultRes.errorResult("Internal error processing mock webhook: " + e.getMessage(), 500, java.util.Collections.emptyList()));
        }
    }

    @PostMapping("/ghn")
    public ResponseEntity<ResultRes<Void>> handleGhnWebhook(@RequestBody java.util.Map<String, Object> payload) {
        log.info("Received GHN webhook request with payload: {}", payload);

        try {
            String status = payload.containsKey("Status") ? String.valueOf(payload.get("Status")) : null;
            if (status == null && payload.containsKey("status")) {
                status = String.valueOf(payload.get("status"));
            }

            String orderCode = payload.containsKey("OrderCode") ? String.valueOf(payload.get("OrderCode")) : null;
            if (orderCode == null && payload.containsKey("order_code")) {
                orderCode = String.valueOf(payload.get("order_code"));
            }

            String type = payload.containsKey("Type") ? String.valueOf(payload.get("Type")) : null;
            if (type == null && payload.containsKey("type")) {
                type = String.valueOf(payload.get("type"));
            }

            if (status == null || orderCode == null || status.trim().isEmpty() || orderCode.trim().isEmpty()) {
                log.warn("Invalid GHN webhook payload: missing Status or OrderCode");
                return ResponseEntity.badRequest().body(ResultRes.errorResult(OrderMessageConstants.GHN_WEBHOOK_INVALID, 400, java.util.Collections.emptyList()));
            }

            // Only process type == "switch_status" or similar. GHN type is usually "switch_status" or Case Insensitive.
            if (type != null && !"switch_status".equalsIgnoreCase(type.trim())) {
                log.info("Ignoring GHN webhook type: {}, only switch_status is processed", type);
                return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.GHN_WEBHOOK_PROCESSED, 200));
            }

            String statusLower = status.trim().toLowerCase();
            if (!java.util.List.of("ready_to_pick", "delivering", "delivered", "cancel", "return").contains(statusLower)) {
                log.info("Ignoring GHN status: {}", status);
                return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.GHN_WEBHOOK_PROCESSED, 200));
            }

            // Map to Outbox message
            String rawJson = com.inkpulse.corehelpers.JsonHelper.serializeSafe(payload);
            GhnStatusUpdateMessage msg = GhnStatusUpdateMessage.builder()
                    .orderCode(orderCode.trim())
                    .status(statusLower)
                    .rawPayload(rawJson)
                    .type(type != null ? type.trim() : "switch_status")
                    .build();

            // Publish to Outbox
            outboxPublisher.publish(
                    QueueConstants.GHN_STATUS_UPDATE,
                    msg,
                    "urn:message:InkPulse.Worker.Features.Order.Messages:GhnStatusUpdateMessage");

            log.info("Successfully wrote GHN Webhook event to Outbox. GHN Code: {}, Status: {}", msg.getOrderCode(), msg.getStatus());

            return ResponseEntity.ok(ResultRes.successResult(null, OrderMessageConstants.GHN_WEBHOOK_PROCESSED, 200));
        } catch (Exception e) {
            log.error("Exception handling GHN webhook", e);
            return ResponseEntity.status(500).body(ResultRes.errorResult("Internal error processing GHN webhook: " + e.getMessage(), 500, java.util.Collections.emptyList()));
        }
    }
}
