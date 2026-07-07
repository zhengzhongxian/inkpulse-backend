package com.inkpulse.controllers;

import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.models.response.ResultRes;
import com.inkpulse.models.message.PayOsWebhookMessage;
import com.inkpulse.service.payos.IPayOsService;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@Slf4j
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
}
