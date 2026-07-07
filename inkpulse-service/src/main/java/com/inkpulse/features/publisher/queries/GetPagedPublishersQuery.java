package com.inkpulse.features.publisher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.publisher.PublisherResponse;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GetPagedPublishersQuery extends PagedRequest implements Query<PagedList<PublisherResponse>> {
}
