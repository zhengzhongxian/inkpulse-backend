package com.inkpulse.entities;

import com.inkpulse.entities.enums.OrderEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "order_events")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "event_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OrderEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private String eventData;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
