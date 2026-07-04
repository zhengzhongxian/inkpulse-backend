package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.features.book.commands.DeleteBookCommand;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.repositories.BookRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteBookCommandHandler implements Command.CommandHandler<DeleteBookCommand, Boolean> {

    private final BookRepository bookRepository;
    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional
    public Boolean handle(DeleteBookCommand cmd) {
        if (cmd.getId() == null) {
            throw new ResourceNotFoundException(BookMessageConstants.BOOK_NOT_FOUND);
        }

        Book book = bookRepository.findById(cmd.getId())
                .orElseThrow(() -> new ResourceNotFoundException(BookMessageConstants.BOOK_NOT_FOUND));

        // Soft delete the book and all associated editions
        book.setDeleted(true);

        for (BookEdition edition : book.getEditions()) {
            edition.setDeleted(true);
            
            SyncBookEditionMessage edMsg = SyncBookEditionMessage.builder()
                    .id(edition.getId())
                    .bookId(book.getId())
                    .deleted(true)
                    .build();

            outboxPublisher.publish(
                    QueueConstants.SYNC_BOOK_EDITION,
                    edMsg,
                    "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage"
            );
        }

        bookRepository.save(book);

        log.info("Book and its editions soft-deleted and sync messages published. Book ID: {}", cmd.getId());
        return true;
    }
}
