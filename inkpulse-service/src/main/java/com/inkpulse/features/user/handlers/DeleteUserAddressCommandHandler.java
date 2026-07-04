package com.inkpulse.features.user.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.user.commands.DeleteUserAddressCommand;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.features.user.queries.GetUserProfileByUserIdQuery;
import com.inkpulse.repositories.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import an.awesome.pipelinr.Pipeline;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteUserAddressCommandHandler implements Command.CommandHandler<DeleteUserAddressCommand, UserProfileCacheDto> {

    private final UserAddressRepository userAddressRepository;
    private final SectionCacheService sectionCache;
    private final Pipeline pipeline;

    @Override
    @Transactional
    public UserProfileCacheDto handle(DeleteUserAddressCommand cmd) {
        UserAddress address = userAddressRepository.findById(cmd.getAddressId())
                .orElseThrow(() -> new BusinessValidationException(UserMessageConstants.ADDRESS_NOT_FOUND, "ADDRESS_NOT_FOUND"));

        if (!address.getUser().getId().equals(cmd.getUserId())) {
            throw new BusinessValidationException(UserMessageConstants.ACCESS_DENIED, "ACCESS_DENIED");
        }

        // Soft delete address
        address.setDeleted(true);
        userAddressRepository.save(address);

        // Evict profile cache so list is refreshed
        sectionCache.remove(cmd.getUserId().toString(), UserProfileCacheDto.class);

        return pipeline.send(new GetUserProfileByUserIdQuery(cmd.getUserId()));
    }
}
