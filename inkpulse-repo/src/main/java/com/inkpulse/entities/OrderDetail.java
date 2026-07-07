package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders_detail")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "detail_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_edition_id", nullable = false)
    private BookEdition bookEdition;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "original_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalPrice;

    @Column(name = "flash_sale_discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal flashSaleDiscountAmount = BigDecimal.ZERO;

    @Column(name = "voucher_discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashsale_id")
    private FlashSale flashSale;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
