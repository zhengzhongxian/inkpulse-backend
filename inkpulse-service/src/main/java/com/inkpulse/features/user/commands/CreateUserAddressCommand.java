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
public class CreateUserAddressCommand implements Command<UserProfileCacheDto> {
    private UUID userId;
    private String recipientPhone;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String streetAddress;
    private String addressLabel;
}
