package com.inkpulse.features.author.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.ImageHelper;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Author;
import com.inkpulse.features.author.commands.CreateAuthorCommand;
import com.inkpulse.models.response.author.AuthorResponse;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.features.author.dto.SyncAuthorMessage;
import com.inkpulse.repositories.AuthorRepository;
import com.inkpulse.service.minio.IMinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateAuthorCommandHandler implements Command.CommandHandler<CreateAuthorCommand, AuthorResponse> {

    private final AuthorRepository authorRepository;
    private final OutboxPublisher outboxPublisher;
    private final IMinioService minioService;
    private final TransactionTemplate transactionTemplate;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    public AuthorResponse handle(CreateAuthorCommand cmd) {
        if (cmd.getName() == null || cmd.getName().isBlank()) {
            throw new BusinessValidationException(BookMessageConstants.AUTHOR_NAME_EMPTY,
                    BookMessageConstants.CODE_AUTHOR_NAME_EMPTY);
        }

        String avatarRelativePath = null;

        UploadFileModel avatarFile = cmd.getAvatarFile();
        if (avatarFile != null && avatarFile.getInputStream() != null) {
            String ext = ".jpg";
            if (avatarFile.getFileName() != null) {
                int extIndex = avatarFile.getFileName().lastIndexOf('.');
                if (extIndex != -1) {
                    ext = avatarFile.getFileName().substring(extIndex);
                }
            }
            // Resize avatar to 400x400
            UploadFileModel resizedAvatar = ImageHelper.resizeTo400x400(
                    avatarFile.getInputStream(),
                    avatarFile.getFileName(),
                    avatarFile.getContentType());

            String avatarObjectName = "author/" + UUID.randomUUID() + "_avatar" + ext;
            try {
                minioService.uploadFile(
                        resizedAvatar.getInputStream(),
                        resizedAvatar.getFileName(),
                        resizedAvatar.getContentType(),
                        resizedAvatar.getFileSize(),
                        avatarObjectName,
                        null);
                avatarRelativePath = avatarObjectName;
            } catch (Exception ex) {
                log.error("Failed to upload author avatar to MinIO", ex);
                throw new BusinessValidationException(BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                        BookMessageConstants.CODE_UPLOAD_FAILED);
            }
        }

        final String finalAvatarRelativePath = avatarRelativePath;

        try {
            return transactionTemplate.execute(status -> {
                Author author = Author.builder()
                        .name(cmd.getName().trim())
                        .biography(cmd.getBiography() != null ? cmd.getBiography().trim() : null)
                        .avatar(finalAvatarRelativePath)
                        .build();

                Author savedAuthor = authorRepository.save(author);
                UUID authorId = savedAuthor.getId();

                // Sync to Elasticsearch via Outbox
                SyncAuthorMessage syncMsg = SyncAuthorMessage.builder()
                        .id(authorId)
                        .name(savedAuthor.getName())
                        .biography(savedAuthor.getBiography())
                        .avatarUrl(finalAvatarRelativePath)
                        .isDeleted(false)
                        .build();
                outboxPublisher.publish(
                        QueueConstants.SYNC_AUTHOR,
                        syncMsg,
                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncAuthorMessage");
                log.info("Author sync message published to outbox. Author ID: {}", authorId);

                return AuthorResponse.builder()
                        .id(authorId)
                        .name(savedAuthor.getName())
                        .biography(savedAuthor.getBiography())
                        .avatarUrl(UrlHelper.buildAbsoluteUrl(publicUrl, finalAvatarRelativePath, useSsl))
                        .build();
            });
        } catch (Exception ex) {
            // Cleanup MinIO file if DB transaction failed
            if (finalAvatarRelativePath != null) {
                try {
                    minioService.deleteFile(finalAvatarRelativePath);
                    log.info("Cleaned up orphaned MinIO file: {}", finalAvatarRelativePath);
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup MinIO file after DB exception: {}", finalAvatarRelativePath, cleanupEx);
                }
            }
            throw ex;
        }
    }
}
