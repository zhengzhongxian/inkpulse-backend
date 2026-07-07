package com.inkpulse.features.cart.queries;

import com.inkpulse.cqrs.Query;
import java.util.UUID;

public record GetMyCartCountQuery(UUID userId) implements Query<Integer> {
}
