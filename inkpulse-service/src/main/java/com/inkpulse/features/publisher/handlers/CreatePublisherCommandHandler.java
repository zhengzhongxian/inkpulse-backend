package com.inkpulse.features.publisher.handlers;

import com.inkpulse.constants.message.PublisherMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Publisher;
import com.inkpulse.features.publisher.commands.CreatePublisherCommand;
import com.inkpulse.features.publisher.dto.PublisherResponse;
import com.inkpulse.repositories.PublisherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreatePublisherCommandHandler implements Command.CommandHandler<CreatePublisherCommand, PublisherResponse> {

    private final PublisherRepository publisherRepository;

    @Override
    @Transactional
    public PublisherResponse handle(CreatePublisherCommand command) {
        log.info("Handling CreatePublisherCommand: name={}", command.getName());

        if (command.getName() == null || command.getName().isBlank()) {
            throw new BusinessValidationException(
                    PublisherMessageConstants.PUBLISHER_NAME_EMPTY,
                    PublisherMessageConstants.CODE_PUBLISHER_NAME_EMPTY
            );
        }

        Publisher pub = Publisher.builder()
                .name(command.getName().trim())
                .address(command.getAddress() != null ? command.getAddress().trim() : null)
                .build();

        Publisher saved = publisherRepository.save(pub);

        return PublisherResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .address(saved.getAddress())
                .build();
    }
}
