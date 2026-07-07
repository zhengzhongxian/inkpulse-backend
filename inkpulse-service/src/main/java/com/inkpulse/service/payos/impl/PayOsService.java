package com.inkpulse.service.payos.impl;

import com.inkpulse.corehelpers.exceptions.PayOsBusinessException;
import com.inkpulse.corehelpers.exceptions.PayOsTechnicalException;
import com.inkpulse.service.payos.IPayOsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOsService implements IPayOsService {

    private final PayOS payOS;

    @Override
    public CreatePaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request) {
        log.info("Creating PayOS payment link for orderCode: {}", request.getOrderCode());
        try {
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            if (response == null) {
                throw new PayOsTechnicalException("Received null response from PayOS API.");
            }
            return response;
        } catch (PayOsTechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create PayOS payment link", ex);
            throw new PayOsBusinessException("PayOS Error: " + ex.getMessage(), "PAYOS_ERROR");
        }
    }

    @Override
    public PaymentLink getPaymentLinkInformation(long orderCode) {
        log.info("Fetching PayOS payment link information for orderCode: {}", orderCode);
        try {
            PaymentLink response = payOS.paymentRequests().get(orderCode);
            if (response == null) {
                throw new PayOsTechnicalException("Received null response from PayOS API.");
            }
            return response;
        } catch (PayOsTechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch PayOS payment link information", ex);
            throw new PayOsBusinessException("PayOS Error: " + ex.getMessage(), "PAYOS_GET_ERROR");
        }
    }

    @Override
    public WebhookData verifyWebhook(Webhook webhook) {
        log.info("Verifying PayOS webhook payload");
        try {
            if (webhook == null || webhook.getData() == null) {
                return null;
            }
            WebhookData verifiedData = payOS.webhooks().verify(webhook);
            if (verifiedData == null) {
                throw new PayOsTechnicalException("Webhook verification failed: verification returned null.");
            }
            return verifiedData;
        } catch (PayOsTechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to verify PayOS webhook signature", ex);
            throw new PayOsBusinessException("Webhook verification failed: " + ex.getMessage(), "WEBHOOK_VERIFY_ERROR");
        }
    }

    @Override
    public Payout refundPayment(PayoutRequests payoutRequests) {
        log.info("Initiating PayOS payout refund for referenceId: {}", payoutRequests.getReferenceId());
        try {
            if (payoutRequests == null) {
                throw new PayOsBusinessException("Payout requests cannot be null", "INVALID_PAYOUT_DATA");
            }
            Payout result = payOS.payouts().create(payoutRequests);
            if (result == null) {
                throw new PayOsTechnicalException("Received null response from PayOS Payout API.");
            }
            return result;
        } catch (PayOsBusinessException | PayOsTechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process PayOS payout refund", ex);
            throw new PayOsBusinessException("Payout (Refund) failed: " + ex.getMessage(), "PAYOUT_ERROR");
        }
    }
}
