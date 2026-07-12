package com.inkpulse.entities;

import com.inkpulse.entities.enums.StockTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.util.UUID;

@Entity
@Table(name = "stock_transactions")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "transaction_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edition_id", nullable = false)
    private BookEdition edition;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private StockTransactionType type;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
