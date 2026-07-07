package com.inkpulse.service.payos;

import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v1.payouts.Payout;

public interface IPayOsService {
    CreatePaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request);
    PaymentLink getPaymentLinkInformation(long orderCode);
    WebhookData verifyWebhook(Webhook webhook);
    Payout refundPayment(PayoutRequests payoutRequests);
}
