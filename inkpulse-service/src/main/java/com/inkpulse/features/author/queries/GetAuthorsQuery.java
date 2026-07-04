package com.inkpulse.features.author.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.author.dto.AuthorResponse;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetAuthorsQuery extends PagedRequest implements Query<PagedList<AuthorResponse>> {

    public GetAuthorsQuery(String searchKeyword, int pageNumber, int pageSize) {
        this.setSearchKeyword(searchKeyword);
        this.setPageNumber(pageNumber);
        this.setPageSize(pageSize);
    }
}
