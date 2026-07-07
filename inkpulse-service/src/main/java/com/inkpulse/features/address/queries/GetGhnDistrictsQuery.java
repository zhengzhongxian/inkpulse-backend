package com.inkpulse.features.address.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.ghn.GhnDistrictResponse;
import java.util.List;

public record GetGhnDistrictsQuery(Integer provinceId) implements Query<List<GhnDistrictResponse>> {
}
