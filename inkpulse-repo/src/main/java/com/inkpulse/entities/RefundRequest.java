package com.inkpulse.entities;

import com.inkpulse.entities.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "refund_requests")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "refund_request_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "payos_refund_id", length = 100)
    private String payosRefundId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
