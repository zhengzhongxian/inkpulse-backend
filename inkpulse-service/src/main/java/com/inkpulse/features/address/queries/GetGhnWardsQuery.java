package com.inkpulse.features.address.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.GhnWardResponse;
import java.util.List;

public record GetGhnWardsQuery(Integer districtId) implements Query<List<GhnWardResponse>> {
}
