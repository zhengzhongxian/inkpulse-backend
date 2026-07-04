package com.inkpulse.features.badge.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.badge.dto.BadgeResponse;
import com.inkpulse.features.badge.queries.GetBadgesQuery;
import com.inkpulse.repositories.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetBadgesQueryHandler implements Query.QueryHandler<GetBadgesQuery, List<BadgeResponse>> {

    private final BadgeRepository badgeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> handle(GetBadgesQuery query) {
        log.info("Handling GetBadgesQuery to fetch all active badges");
        return badgeRepository.findAll().stream()
                .map(badge -> BadgeResponse.builder()
                        .id(badge.getId())
                        .text(badge.getText())
                        .textColor(badge.getTextColor())
                        .bgColor(badge.getBgColor())
                        .build())
                .toList();
    }
}
