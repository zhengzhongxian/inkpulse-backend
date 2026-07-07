package com.inkpulse.features.badge.handlers;

import com.inkpulse.constants.message.BadgeMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Badge;
import com.inkpulse.models.response.badge.BadgeResponse;
import com.inkpulse.features.badge.queries.GetBadgeByIdQuery;
import com.inkpulse.repositories.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetBadgeByIdQueryHandler implements Query.QueryHandler<GetBadgeByIdQuery, BadgeResponse> {

    private final BadgeRepository badgeRepository;

    @Override
    @Transactional(readOnly = true)
    public BadgeResponse handle(GetBadgeByIdQuery query) {
        log.info("Handling GetBadgeByIdQuery: id={}", query.getId());

        Badge badge = badgeRepository.findById(query.getId())
                .orElseThrow(() -> new BusinessValidationException(
                        BadgeMessageConstants.BADGE_NOT_FOUND,
                        BadgeMessageConstants.CODE_BADGE_NOT_FOUND
                ));

        return BadgeResponse.builder()
                .id(badge.getId())
                .text(badge.getText())
                .textColor(badge.getTextColor())
                .bgColor(badge.getBgColor())
                .build();
    }
}
