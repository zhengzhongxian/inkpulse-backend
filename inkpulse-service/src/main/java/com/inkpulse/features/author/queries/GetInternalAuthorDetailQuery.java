package com.inkpulse.features.author.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.author.dto.AuthorDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetInternalAuthorDetailQuery implements Query<AuthorDetailResponse> {
    private UUID authorId;
}
