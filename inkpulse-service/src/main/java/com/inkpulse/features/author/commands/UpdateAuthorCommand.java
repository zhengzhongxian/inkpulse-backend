package com.inkpulse.features.author.commands;

import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.author.AuthorResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAuthorCommand implements Command<AuthorResponse> {
    private UUID id;
    private String name;
    private String biography;
    private UploadFileModel avatarFile;
}
