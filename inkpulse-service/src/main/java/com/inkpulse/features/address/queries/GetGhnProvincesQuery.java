package com.inkpulse.features.address.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.GhnProvinceResponse;
import java.util.List;

public record GetGhnProvincesQuery() implements Query<List<GhnProvinceResponse>> {
}
