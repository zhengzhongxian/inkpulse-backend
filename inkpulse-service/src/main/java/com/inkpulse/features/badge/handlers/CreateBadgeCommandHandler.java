package com.inkpulse.features.badge.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Badge;
import com.inkpulse.features.badge.commands.CreateBadgeCommand;
import com.inkpulse.models.response.badge.BadgeResponse;
import com.inkpulse.repositories.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateBadgeCommandHandler implements Command.CommandHandler<CreateBadgeCommand, BadgeResponse> {

    private final BadgeRepository badgeRepository;

    @Override
    @Transactional
    public BadgeResponse handle(CreateBadgeCommand command) {
        log.info("Handling CreateBadgeCommand: text={}", command.getText());

        Badge badge = Badge.builder()
                .text(command.getText())
                .textColor(command.getTextColor())
                .bgColor(command.getBgColor())
                .shape(command.getShape())
                .build();

        Badge saved = badgeRepository.save(badge);

        return BadgeResponse.builder()
                .id(saved.getId())
                .text(saved.getText())
                .textColor(saved.getTextColor())
                .bgColor(saved.getBgColor())
                .shape(saved.getShape())
                .build();
    }
}
