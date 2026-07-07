package com.inkpulse.entities;

import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.entities.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghn_province_id", nullable = false)
    private GhnProvince province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghn_district_id", nullable = false)
    private GhnDistrict district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghn_ward_code", nullable = false)
    private GhnWard ward;

    @Convert(converter = AesEncryptConverter.class)
    @Column(name = "recipient_phone", nullable = false, length = 255)
    private String recipientPhone;

    @Column(name = "receiver_name", nullable = false, length = 255)
    private String receiverName;

    @Column(name = "ghn_order_code", length = 100)
    private String ghnOrderCode;

    @Column(name = "order_code", nullable = false, unique = true, length = 100)
    private String orderCode;

    @Column(name = "street_address", nullable = false, length = 500)
    private String streetAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 50)
    private OrderStatus orderStatus;

    @Column(name = "address_label", nullable = false, length = 100)
    private String addressLabel;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingFee;

    @Column(name = "order_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal orderFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "voucher_discount_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
