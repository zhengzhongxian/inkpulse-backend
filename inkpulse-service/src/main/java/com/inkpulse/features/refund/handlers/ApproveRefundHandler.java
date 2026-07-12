package com.inkpulse.features.refund.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.RefundRequest;
import com.inkpulse.entities.User;
import com.inkpulse.entities.enums.RefundStatus;
import com.inkpulse.features.refund.commands.ApproveRefundCommand;
import com.inkpulse.repositories.RefundRequestRepository;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.service.payos.IPayOsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v1.payouts.Payout;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApproveRefundHandler implements Command.CommandHandler<ApproveRefundCommand, Void> {

    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;
    private final IPayOsService payOsService;

    @Override
    @Transactional
    public Void handle(ApproveRefundCommand command) {
        log.info("Handling ApproveRefundCommand for refund request: {} by admin: {}", 
                command.getRefundRequestId(), command.getAdminUserId());

        // 1. Fetch Refund Request
        Optional<RefundRequest> refundOpt = refundRequestRepository.findById(command.getRefundRequestId());
        if (refundOpt.isEmpty()) {
            throw new BusinessValidationException("Không tìm thấy phiếu yêu cầu hoàn tiền!", "REFUND_NOT_FOUND");
        }

        RefundRequest refund = refundOpt.get();

        // 2. Perform step-lock update from PENDING to PROCESSING (allows PENDING or FAILED)
        int affectedRows = refundRequestRepository.updateStatusSecurely(
                refund.getId(),
                RefundStatus.PENDING,
                RefundStatus.PROCESSING,
                LocalDateTime.now()
        );

        if (affectedRows == 0) {
            log.warn("Refund request {} is already being processed or not in PENDING/FAILED state.", refund.getId());
            throw new BusinessValidationException("Phiếu đang được xử lý bởi luồng khác!", "REFUND_ALREADY_PROCESSING");
        }

        // Fetch User actor
        Optional<User> adminOpt = userRepository.findById(UUID.fromString(command.getAdminUserId()));
        refund.setApprovedBy(adminOpt.orElse(null));

        // Try to automatically retrieve customer payment bank details from PayOS
        String accountNumber = command.getAccountNumber();
        String bin = command.getBin();
        String accountName = command.getAccountName();

        try {
            vn.payos.model.v2.paymentRequests.PaymentLink paymentLink = payOsService.getPaymentLinkInformation(Long.parseLong(refund.getOrder().getOrderCode()));
            if (paymentLink != null && paymentLink.getTransactions() != null && !paymentLink.getTransactions().isEmpty()) {
                // Get the first transaction
                vn.payos.model.v2.paymentRequests.Transaction tx = (vn.payos.model.v2.paymentRequests.Transaction) paymentLink.getTransactions().get(0);
                if (tx.getCounterAccountNumber() != null && !tx.getCounterAccountNumber().trim().isEmpty()) {
                    accountNumber = tx.getCounterAccountNumber();
                }
                if (tx.getCounterAccountBankId() != null && !tx.getCounterAccountBankId().trim().isEmpty()) {
                    bin = tx.getCounterAccountBankId();
                }
                if (tx.getCounterAccountName() != null && !tx.getCounterAccountName().trim().isEmpty()) {
                    accountName = tx.getCounterAccountName();
                }
                log.info("Automatically detected customer bank account from prior PayOS transaction: Bank BIN {}, Account {}, Name {}", 
                        bin, accountNumber, accountName);
            }
        } catch (Exception ex) {
            log.warn("Could not automatically retrieve customer bank details from PayOS for order {}: {}. Falling back to default command values.", 
                    refund.getOrder().getOrderCode(), ex.getMessage());
        }

        try {
            // Build payout model for PayOS
            PayoutRequests payout = PayoutRequests.builder()
                    .referenceId(refund.getId().toString())
                    .amount(refund.getAmount().longValue())
                    .description("Hoan tien don hang " + refund.getOrder().getOrderCode())
                    .toBin(bin)
                    .toAccountNumber(accountNumber)
                    .category(java.util.Collections.singletonList("Refund"))
                    .build();

            // 3. Call PayOS API
            Payout result = payOsService.refundPayment(payout);

            // 4. Update status to SUCCESS
            refund.setStatus(RefundStatus.SUCCESS);
            refund.setPayosRefundId(result.getReferenceId() != null ? result.getReferenceId() : refund.getId().toString());
            refund.setErrorMessage(null);
            refundRequestRepository.save(refund);
            log.info("Successfully refunded order {} via PayOS. Refund ID: {}", 
                    refund.getOrder().getOrderCode(), refund.getPayosRefundId());
        } catch (Exception ex) {
            // 5. Update status to FAILED on exception/timeout
            log.error("Failed to process PayOS refund for request {}", refund.getId(), ex);
            refund.setStatus(RefundStatus.FAILED);
            refund.setErrorMessage(ex.getMessage());
            refundRequestRepository.save(refund);
            throw new BusinessValidationException("Thanh toán hoàn tiền thất bại: " + ex.getMessage(), "PAYOS_REFUND_FAILED");
        }

        return null;
    }
}
