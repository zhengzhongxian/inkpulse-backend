package com.inkpulse.entities;

import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.entities.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "transaction_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_code", referencedColumnName = "order_code", nullable = false)
    private Order order;

    @Column(name = "transaction_code", nullable = false, length = 100)
    private String transactionCode;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "raw_response", columnDefinition = "text")
    private String rawResponse;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
