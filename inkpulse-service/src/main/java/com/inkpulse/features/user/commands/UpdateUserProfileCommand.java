package com.inkpulse.features.user.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.corehelpers.images.UploadFileModel;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileCommand implements Command<UserProfileCacheDto> {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dob;
    private String biography;
    private String displayMode;
    private String choiceLanguage;
    private List<String> mfaTypes;
    private UploadFileModel avatarFile;
}
