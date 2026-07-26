package com.inkpulse.features.banner.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GetPagedBannersQuery extends PagedRequest implements Query<PagedList<BannerResponse>> {

    private Boolean isActive;

    public GetPagedBannersQuery() {
        super();
    }
}
