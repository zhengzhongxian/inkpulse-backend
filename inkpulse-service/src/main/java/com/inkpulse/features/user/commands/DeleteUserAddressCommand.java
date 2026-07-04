package com.inkpulse.features.user.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteUserAddressCommand implements Command<UserProfileCacheDto> {
    private UUID addressId;
    private UUID userId;
}
